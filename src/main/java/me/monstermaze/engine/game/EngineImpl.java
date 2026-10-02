package me.monstermaze.engine.game;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.kit.KitSimulator;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;
import me.monstermaze.engine.monster.MonsterSimulator;
import me.monstermaze.engine.pad.SafePadSimulator;
import me.monstermaze.engine.physics.MonsterMazeBumpModel;
import me.monstermaze.engine.physics.PlayerPhysics18;
import me.monstermaze.engine.util.SeededRandom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared pure Monster Maze game engine.
 *
 * The mechanics/order are intentionally aligned with MonsterMazeAI's common
 * source-faithful simulator so the same learned policy actions can be evaluated
 * here and later executed through the Minecraft 1.8 adapter.
 */
public final class EngineImpl implements MonsterMazeEngine {
    private static final long MONSTER_SEED_XOR = 0x6A09E667F3BCC909L;
    private static final long PAD_SEED_XOR = 0xBB67AE8584CAA73BL;
    private static final int STARTING_TICKS = 200;
    private static final int INITIAL_CENTER_STAGE = 11;

    private final int centerX, centerY, centerZ, starterOverride;
    private MazeGraph graph;
    private SeededRandom monsterRandom;
    private SeededRandom padRandom;
    private PlayerPhysics18 physics;
    private int nextMonsterId;
 
    public EngineImpl() {
        this(Coordinates.DEFAULT_CENTER_X, Coordinates.DEFAULT_CENTER_Y,
                Coordinates.DEFAULT_CENTER_Z, -1);
    }

    public EngineImpl(int starterOverride) {
        this(Coordinates.DEFAULT_CENTER_X, Coordinates.DEFAULT_CENTER_Y,
                Coordinates.DEFAULT_CENTER_Z, starterOverride);
    }

    public EngineImpl(int centerX, int centerY, int centerZ, int starterOverride) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.starterOverride = starterOverride;
    }

    public MazeGraph getWorkingGraph() {
        return graph;
    }

    @Override
    public GameState initialState(MazeMode mode, int layoutId, KitType kit, long seed) {
        if (layoutId < 0 || layoutId >= Layouts.LAYOUT_COUNT) {
            throw new IllegalArgumentException("layoutId must be 0.." + (Layouts.LAYOUT_COUNT - 1));
        }
        if (kit == KitType.MAVERICK && mode == MazeMode.ORIGINAL) {
            throw new IllegalArgumentException("Maverick requires SPEED or MODERN");
        }

        graph = new MazeGraph(layoutId);
        monsterRandom = new SeededRandom(seed ^ MONSTER_SEED_XOR);
        padRandom = new SeededRandom(seed ^ PAD_SEED_XOR);
        physics = new PlayerPhysics18(graph, centerX, centerY, centerZ);
        nextMonsterId = 1;
 
        PlayerState player = new PlayerState(
                new Vec3(Coordinates.pathCenterX(centerX, Layouts.HALF), centerY,
                        Coordinates.pathCenterZ(centerZ, Layouts.HALF)),
                Vec3.ZERO, 0.0F, 0.0F, true,
                20.0, 20.0, kit,
                initialJumperCharges(mode, kit), 0,
                KitSimulator.initialAbilityCharges(kit), 0, 0, 0, false);
        player.abilityActivations = KitSimulator.initialAbilityActivations(kit);
        player.nextJumpChargeTick = 0L;

        SafePadState activePad =
                SafePadSimulator.initialPad(graph, centerX, centerY, centerZ, padRandom);

        GameState state = new GameState();
        state.tick = 0L;
        state.mode = mode;
        state.phase = GamePhase.STARTING;
        state.stage = 1;
        state.phaseTimerMax = StageTimer.initialTicks(mode, 1);
        state.phaseTimerTicks = state.phaseTimerMax;
        state.centerDeteriorationStep = INITIAL_CENTER_STAGE;
        state.maze = graph.toMazeState();
        state.player = player;
        state.activePad = activePad;
        state.inMonsterMaze = true;
        state.alive = true;
        state.pendingMonsterSpawns = starterOverride >= 0
                ? Math.min(starterOverride, StageTimer.starterMonsters(mode))
                : StageTimer.starterMonsters(mode);
        return state;
    }

    @Override
    public TickResult tick(GameState input, Action action) {
        if (input == null) throw new IllegalArgumentException("state");
        if (action == null) action = Action.noop();

        GameState state = input.copy();
        if (!state.alive) return new TickResult(state, List.of(), true);
        ensureRuntime(state);

        long currentTick = state.tick;
        List<GameEvent> events = new ArrayList<>();

        // Source spawn task runs before the monster movement task.
        if (state.phase == GamePhase.STARTING || state.phase == GamePhase.LIVE) {
            spawnInitialBatch(state, events);
        }

        // STARTING ends after five seconds; no phase timer decrement occurs here.
        if (state.phase == GamePhase.STARTING && currentTick + 1 >= STARTING_TICKS) {
            state.phase = GamePhase.LIVE;
        }

        if (state.phase == GamePhase.LIVE || state.phase == GamePhase.STARTING) {
            // Match Simulator.tick(): abilities are resolved before movement.
            KitSimulator.activate(state.player, state.monsters, action, state.mode,
                    state.activePad, state.previewPad, currentTick);

            boolean canUseNormalJump = state.player.kit != KitType.JUMPER
                    || state.player.jumpCharges > 0;
            int jumpAmplifier = canUseNormalJump ? 0 : -10;

            physics.tick(state.player, action, jumpAmplifier);

            state.player.onSafePad =
                    SafePadSimulator.isOn(state.activePad, state.player.pos)
                            || SafePadSimulator.isOn(state.previewPad, state.player.pos)
                            || onOldPad(state, state.player);

            // MonsterManager's movement/bump tick is gated on LIVE. Monsters may spawn
            // during STARTING behind the source containment barrier, but they do not move
            // or damage the player until LIVE.
            if (state.phase == GamePhase.LIVE) {
                MonsterSimulator monsters = new MonsterSimulator(
                        graph, centerX, centerY, centerZ, monsterRandom, 1.4, currentTick);
                monsters.tick(state.monsters, currentTick);

                int bumpResult = MonsterMazeBumpModel.apply(state);
                if (bumpResult == MonsterMazeBumpModel.RESULT_NORMAL_HIT) {
                    events.add(new GameEvent(GameEventType.DAMAGE, 4.0));
                    events.add(new GameEvent(GameEventType.KNOCKBACK, state.player.vel));
                } else if (bumpResult == MonsterMazeBumpModel.RESULT_BODY_RUSH) {
                    events.add(new GameEvent(GameEventType.ABILITY_USED, "body_rush_contact"));
                }
            }

            progress(state, events);

            if (state.player.kit == KitType.JUMPER && state.player.pos.y > centerY) {
                boolean consumed = KitSimulator.consumeJumperCharge(
                        state.player, state.mode, state.activePad, state.previewPad, currentTick);
                if (consumed) {
                    state.player.jumpCharges = Math.max(0, state.player.jumpCharges);
                    // Jumper's policy resource is the same charge pool.
                    state.player.abilityCharges = state.player.jumpCharges;
                }
            }
        }

        state.player.hitCooldownTicks = Math.max(0, state.player.hitCooldownTicks - 1);
        state.tick = currentTick + 1;

        if (state.player.health <= 0) {
            state.alive = false;
            state.phase = GamePhase.ENDING;
            events.add(new GameEvent(GameEventType.ELIMINATED, null));
        }

        state.maze = graph.toMazeState();
        boolean terminal = !state.alive || state.phase == GamePhase.ENDING;
        return new TickResult(state, events, terminal);
    }

    private void progress(GameState state, List<GameEvent> events) {
        boolean onActive = SafePadSimulator.isOn(state.activePad, state.player.pos);

        if (onActive && !state.padReached) {
            state.padReached = true;
            KitSimulator.onReachedPad(state.player, state.mode, state.stage == 1);
            if (state.player.kit == KitType.JUMPER && state.mode != MazeMode.ORIGINAL) {
                state.player.abilityCharges = 3;
                state.player.jumpCharges = 3;
            }
            int shortened = StageTimer.firstArrivalCapTicks(
                    state.stage, state.phaseTimerTicks);
            if (shortened < state.phaseTimerTicks) {
                state.phaseTimerTicks = shortened;
                events.add(new GameEvent(GameEventType.TIMER_SHORTENED, shortened));
            }
            events.add(new GameEvent(GameEventType.PAD_REACHED, state.stage));
        }

        // Solo mode means the one living player is the whole population.
        if (onActive) {
            int shortened = Math.min(state.phaseTimerTicks, StageTimer.ALL_ON_PAD_SECONDS * 20);
            state.phaseTimerTicks = shortened;
        }

        state.phaseSecondAccumulatorTicks++;
        if (state.phaseSecondAccumulatorTicks < 20 || state.phase == GamePhase.STARTING) return;
        state.phaseSecondAccumulatorTicks = 0;
        state.liveSeconds++;

        if (state.phaseTimerTicks > 0) {
            state.phaseTimerTicks -= 20;
        }

        if (state.phaseTimerTicks == StageTimer.PREVIEW_SECONDS * 20
                && state.previewPad == null) {
            List<SafePadState> avoid = new ArrayList<>(state.oldPads);
            if (state.activePad != null) avoid.add(state.activePad);
            if (state.previewPad != null) avoid.add(state.previewPad);
            state.previewPad = SafePadSimulator.previewPad(
                    graph, centerX, centerY, centerZ, padRandom, avoid);
            state.previewPadRequested = state.previewPad != null;
            // Source removes any monster already standing on the newly built preview pad.
            removeMonstersOnPad(state, state.previewPad);
        }

        tickOldPadDecay(state);

        // Source deteriorates the center once per second beginning 20 live seconds in.
        if (state.liveSeconds >= StageTimer.CENTER_DETERIORATION_START_SECONDS
                && state.centerDeteriorationStep > 0) {
            state.centerDeteriorationStep--;
            applyCenterDeterioration(state);
        }

        if (state.phaseTimerTicks > 0) return;

        if (!onActive) {
            state.alive = false;
            state.phase = GamePhase.ENDING;
            events.add(new GameEvent(GameEventType.ELIMINATED, "missed_safe_pad"));
            return;
        }

        advanceStage(state, events);
    }

    private void advanceStage(GameState state, List<GameEvent> events) {
        if (state.activePad != null) {
            state.oldPads.add(state.activePad);
            state.oldPadDecaySeconds.put(padKey(state.activePad), 11);
        }

        state.stage++;
        state.phaseTimerMax = StageTimer.initialTicks(state.mode, state.stage);
        state.phaseTimerTicks = state.phaseTimerMax;
        state.phaseSecondAccumulatorTicks = 0;
        state.padReached = false;

        SafePadState promoted = state.previewPad;
        if (promoted == null) {
            List<SafePadState> avoid = new ArrayList<>(state.oldPads);
            state.activePad = SafePadSimulator.nextPad(
                    graph, centerX, centerY, centerZ, padRandom, avoid);
        } else {
            state.activePad = new SafePadState(
                    promoted.centerX, promoted.centerZ, promoted.surfaceY,
                    promoted.decayStep, true, false);
        }
        state.previewPad = null;
        state.previewPadRequested = false;

        if (state.activePad != null) {
            SafePadSimulator.installSurface(
                    graph, centerX, centerZ, state.activePad);
            removeMonstersOnPad(state, state.activePad);
        }

        int extra = StageTimer.monstersPerTransition(state.mode);
        int spawned = spawnTransitionBatch(state, extra);
        events.add(new GameEvent(GameEventType.STAGE_ADVANCE, state.stage));
        events.add(new GameEvent(GameEventType.MONSTER_SPAWN, spawned));
    }

    private void spawnInitialBatch(GameState state, List<GameEvent> events) {
        if (state.pendingMonsterSpawns <= 0) return;
        int batch = Math.min(25, state.pendingMonsterSpawns);
        List<int[]> paths = rawPathCells();
        int spawned = 0;
        int guard = 0;
        int centerRow = Layouts.HALF, centerCol = Layouts.HALF;
        while (spawned < batch && guard++ < batch * 5) {
            int[] cell = paths.get(monsterRandom.nextInt(paths.size()));
            if (distanceSq(cell[0], cell[1], centerRow, centerCol)
                    < 7.5 * 7.5) continue;
            state.monsters.add(new MonsterState(
                    nextMonsterId++,
                    new Vec3(Coordinates.pathCenterX(centerX, cell[0]), centerY,
                            Coordinates.pathCenterZ(centerZ, cell[1])),
                    Vec3.ZERO, cell[0], cell[1], -1, false, 0));
            spawned++;
        }
        state.pendingMonsterSpawns -= spawned;
        if (spawned > 0) events.add(new GameEvent(GameEventType.MONSTER_SPAWN, spawned));
    }

    private List<int[]> rawPathCells() {
        List<int[]> cells = new ArrayList<>();
        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                if (Layouts.isRawPath(graph.raw(r, c))) cells.add(new int[]{r, c});
            }
        }
        return cells;
    }

    /** Source spawnMore(): later waves use the layout's dedicated spawn markers (value 2). */
    private int spawnTransitionBatch(GameState state, int count) {
        if (count <= 0) return 0;
        List<int[]> spawns = spawnCells();
        if (spawns.isEmpty()) return 0;
        int target = starterOverride >= 0 ? Math.min(count, 10) : count;
        int spawned = 0;
        for (int i = 0; i < target; i++) {
            int[] cell = spawns.get(monsterRandom.nextInt(spawns.size()));
            state.monsters.add(new MonsterState(
                    nextMonsterId++,
                    new Vec3(Coordinates.pathCenterX(centerX, cell[0]), centerY,
                            Coordinates.pathCenterZ(centerZ, cell[1])),
                    Vec3.ZERO, cell[0], cell[1], -1, false, 0));
            spawned++;
        }
        return spawned;
    }

    private List<int[]> spawnCells() {
        List<int[]> cells = new ArrayList<>();
        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                if (Layouts.isSpawn(graph.raw(r, c))) cells.add(new int[]{r, c});
            }
        }
        return cells;
    }

    private void removeMonstersOnPad(GameState state, SafePadState pad) {
        for (MonsterState m : state.monsters) {
            if (m.removed) continue;
            if (SafePadSimulator.isOn(pad, m.pos)) m.removed = true;
        }
    }

    private void tickOldPadDecay(GameState state) {
        if (state.oldPads.isEmpty()) return;
        List<SafePadState> expired = new ArrayList<>();
        for (SafePadState pad : state.oldPads) {
            String key = padKey(pad);
            int left = state.oldPadDecaySeconds.getOrDefault(key, 11) - 1;
            if (left <= 0) {
                expired.add(pad);
            } else {
                state.oldPadDecaySeconds.put(key, left);
            }
        }
        for (SafePadState pad : expired) {
            state.oldPads.remove(pad);
            state.oldPadDecaySeconds.remove(padKey(pad));
            SafePadSimulator.decayOldPad(graph, centerX, centerZ, pad);
        }
    }

    private void applyCenterDeterioration(GameState state) {
        // The source changes the center-safe-zone material during steps 8..2 and
        // only removes/restores path-vs-void geometry on the final step.
        if (state.centerDeteriorationStep != 1) return;
        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                int v = graph.raw(r, c);
                if (v == 5 || v == 6) {
                    graph.enableIfRawPath(r, c);
                    graph.setPhysicalFloor(r, c, true);
                } else if (v == 3 || v == 4) {
                    graph.setPhysicalFloor(r, c, false);
                }
            }
        }
        state.centerDeteriorationStep = -1;
    }

    private boolean onOldPad(GameState state, PlayerState p) {
        for (SafePadState pad : state.oldPads) {
            if (SafePadSimulator.isOn(pad, p.pos)) return true;
        }
        return false;
    }

    private void ensureRuntime(GameState state) {
        if (graph == null || graph.layoutId() != state.maze.layoutId) {
            graph = graphFromState(state.maze);
            physics = new PlayerPhysics18(graph, centerX, centerY, centerZ);
            if (monsterRandom == null) monsterRandom = new SeededRandom(0x1234ABCDL);
            if (padRandom == null) padRandom = new SeededRandom(0x5678EF01L);
        }
    }

    private MazeGraph graphFromState(MazeState mazeState) {
        MazeGraph rebuilt = new MazeGraph(mazeState.layoutId);
        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                rebuilt.setPhysicalFloor(r, c, mazeState.physicalFloor[r][c]);
                rebuilt.setPadSurface(r, c, mazeState.padSurface[r][c]);
                rebuilt.setTraversable(r, c, mazeState.traversable[r][c]);
            }
        }
        return rebuilt;
    }

    private static int initialJumperCharges(MazeMode mode, KitType kit) {
        if (kit != KitType.JUMPER) return 0;
        return mode == MazeMode.ORIGINAL ? 5 : 3;
    }

    private static double distanceSq(int r1, int c1, int r2, int c2) {
        double dr = r1 - r2, dc = c1 - c2;
        return dr * dr + dc * dc;
    }

    private static String padKey(SafePadState pad) {
        return pad.centerX + ":" + pad.centerZ;
    }
}
