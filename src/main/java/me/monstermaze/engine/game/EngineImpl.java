package me.monstermaze.engine.game;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.kit.KitSimulator;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;
import me.monstermaze.engine.monster.MonsterSimulator;
import me.monstermaze.engine.pad.SafePadSimulator;
import me.monstermaze.engine.physics.Knockback;
import me.monstermaze.engine.physics.PlayerPhysics18;
import me.monstermaze.engine.util.SeededRandom;

import java.util.ArrayList;
import java.util.List;

/** Headless 1.8-faithful engine with full kit support. */
public final class EngineImpl implements MonsterMazeEngine {

    private final int centerX, centerY, centerZ, starterOverride;
    private MazeGraph workingGraph;
    private SeededRandom rng;
    private boolean firstArrivalThisStage;
    private long liveStartTick = -1;

    public EngineImpl() {
        this(Coordinates.DEFAULT_CENTER_X, Coordinates.DEFAULT_CENTER_Y, Coordinates.DEFAULT_CENTER_Z, -1);
    }

    public EngineImpl(int starterOverride) {
        this(Coordinates.DEFAULT_CENTER_X, Coordinates.DEFAULT_CENTER_Y, Coordinates.DEFAULT_CENTER_Z, starterOverride);
    }

    public EngineImpl(int centerX, int centerY, int centerZ, int starterOverride) {
        this.centerX = centerX; this.centerY = centerY; this.centerZ = centerZ;
        this.starterOverride = starterOverride;
    }

    private static boolean qol(MazeMode mode) { return mode != MazeMode.ORIGINAL; }

    @Override
    public GameState initialState(MazeMode mode, int layoutId, KitType kit, long seed) {
        if (layoutId < 0 || layoutId >= Layouts.LAYOUT_COUNT) {
            throw new IllegalArgumentException("layoutId must be 0.." + (Layouts.LAYOUT_COUNT - 1));
        }
        if (kit == KitType.MAVERICK && !qol(mode)) kit = KitType.JUMPER;
        this.rng = new SeededRandom(seed);
        this.workingGraph = new MazeGraph(layoutId);
        this.firstArrivalThisStage = false;
        this.liveStartTick = -1;

        int spawnRow = Layouts.HALF, spawnCol = Layouts.HALF;
        int[][] spawns = workingGraph.spawnCells();
        if (spawns.length > 0) {
            double best = Double.MAX_VALUE;
            for (int[] sc : spawns) {
                double d = (sc[0] - Layouts.HALF) * (sc[0] - Layouts.HALF) + (sc[1] - Layouts.HALF) * (sc[1] - Layouts.HALF);
                if (d < best) { best = d; spawnRow = sc[0]; spawnCol = sc[1]; }
            }
        }

        Vec3 pos = new Vec3(Coordinates.pathCenterX(centerX, spawnRow), centerY, Coordinates.pathCenterZ(centerZ, spawnCol));
        int jumpCharges = kit == KitType.JUMPER ? (mode == MazeMode.ORIGINAL ? 5 : 3) : 0;
        int abilityCharges = KitSimulator.initialAbilityCharges(kit);
        PlayerState player = new PlayerState(pos, Vec3.ZERO, 0f, 0f, true, 20.0, 20.0, kit, jumpCharges, 0, abilityCharges, 0, 0, 0, false);

        int starter = starterOverride >= 0 ? starterOverride : StageTimer.starterMonsters(mode);
        List<MonsterState> monsters = MonsterSimulator.spawnInitial(workingGraph, starter, centerX, centerY, centerZ, rng);
        SafePadState activePad = SafePadSimulator.spawnPad(workingGraph, centerX, centerY, centerZ, rng, false);
        int phaseMax = StageTimer.maxTicks(mode, 0);

        return new GameState(0L, mode, GamePhase.STARTING, 0, phaseMax, phaseMax, 0, workingGraph.toMazeState(), player, monsters, activePad, null);
    }

    @Override
    public TickResult tick(GameState state, Action action) {
        if (rng == null) rng = new SeededRandom(state.tick);
        if (workingGraph == null) workingGraph = new MazeGraph(state.maze.layoutId);

        List<GameEvent> events = new ArrayList<>();
        GamePhase phase = state.phase;
        long tick = state.tick + 1;
        int stage = state.stage;
        int timer = state.phaseTimerTicks;
        int timerMax = state.phaseTimerMax;
        int centerDet = state.centerDeteriorationStep;
        SafePadState activePad = state.activePad;
        SafePadState previewPad = state.previewPad;
        PlayerState player = state.player;
        List<MonsterState> monsters = new ArrayList<>(state.monsters);
        boolean qol = qol(state.mode);

        if (phase == GamePhase.STARTING && tick >= 200) { phase = GamePhase.LIVE; liveStartTick = tick; }

        if (phase == GamePhase.LIVE || phase == GamePhase.STARTING) {
            boolean padJumpFree = qol && activePad != null && SafePadSimulator.isOn(activePad, player.pos);
            player = PlayerPhysics18.step(player, action, workingGraph, centerX, centerY, centerZ, padJumpFree);
            boolean onPad = SafePadSimulator.isOn(activePad, player.pos);
            player = withOnPad(player, onPad);

            KitSimulator.KitTickResult kitResult = KitSimulator.applyAbilities(player, monsters, action, state.mode, activePad, previewPad, qol);
            player = kitResult.player;
            monsters = kitResult.monsters;
            events.addAll(kitResult.events);

            monsters = MonsterSimulator.stepAll(monsters, workingGraph, centerX, centerY, centerZ, 1.0f, tick, rng);

            if (phase == GamePhase.LIVE && !onPad && player.hitCooldownTicks <= 0) {
                List<MonsterState> afterHit = new ArrayList<>();
                boolean hitApplied = false;
                for (MonsterState m : monsters) {
                    if (hitApplied || m.launched || m.frozenTicks > 0) { afterHit.add(m); continue; }
                    double dx = player.pos.x - m.pos.x, dy = player.pos.y - m.pos.y, dz = player.pos.z - m.pos.z;
                    if (dx * dx + dz * dz >= Knockback.CONTACT_RADIUS_SQ || dx * dx + dy * dy + dz * dz >= Knockback.CONTACT_RADIUS_SQ) {
                        afterHit.add(m); continue;
                    }
                    if (KitSimulator.isBodyRushActive(player)) {
                        afterHit.add(KitSimulator.launchMonsterAway(m, player.pos));
                        player = KitSimulator.bodyRushConsumeContact(player);
                        events.add(new GameEvent(GameEventType.ABILITY_USED, "body_rush_deflect"));
                        hitApplied = true; continue;
                    }
                    Vec3 kb = (player.kit == KitType.MAVERICK && qol)
                            ? KitSimulator.maverickKnockback(player.pos, activePad != null ? activePad : previewPad, player.onGround)
                            : Knockback.compute(m.pos, player.pos, player.onGround);
                    double hp = player.health - Knockback.DAMAGE;
                    player = new PlayerState(player.pos, kb, player.yaw, player.pitch, false, hp, player.maxHealth, player.kit,
                            player.jumpCharges, player.jumpChargeCooldownTicks, player.abilityCharges, player.abilityCooldownTicks,
                            player.enhancedCooldownTicks, Knockback.HIT_COOLDOWN_TICKS, player.onSafePad);
                    events.add(new GameEvent(GameEventType.DAMAGE, Knockback.DAMAGE));
                    events.add(new GameEvent(GameEventType.KNOCKBACK, kb));
                    afterHit.add(m);
                    hitApplied = true;
                }
                if (hitApplied) monsters = afterHit;
            }

            if (phase == GamePhase.LIVE) {
                timer--;
                if (onPad && !firstArrivalThisStage) {
                    firstArrivalThisStage = true;
                    int capped = StageTimer.firstArrivalCapTicks(stage, timer);
                    if (capped < timer) { timer = capped; events.add(new GameEvent(GameEventType.TIMER_SHORTENED, capped)); }
                    if (qol && player.kit == KitType.JUMPER) player = withJumpCharges(player, 3);
                    player = KitSimulator.applyBodyBuilderFirstPad(player, true);
                    events.add(new GameEvent(GameEventType.PAD_REACHED, stage));
                }
                if (timer <= StageTimer.PREVIEW_SECONDS * 20 && previewPad == null) {
                    previewPad = SafePadSimulator.spawnPad(workingGraph, centerX, centerY, centerZ, rng, true);
                }
                if (timer <= 0) {
                    stage++; timerMax = StageTimer.maxTicks(state.mode, stage); timer = timerMax; firstArrivalThisStage = false;
                    activePad = previewPad != null
                            ? new SafePadState(previewPad.centerX, previewPad.centerZ, previewPad.surfaceY, SafePadSimulator.DECAY_FULL, true, false)
                            : SafePadSimulator.spawnPad(workingGraph, centerX, centerY, centerZ, rng, false);
                    previewPad = null;
                    int extra = StageTimer.monstersPerTransition(state.mode);
                    if (starterOverride >= 0) extra = Math.min(extra, 10);
                    List<MonsterState> more = MonsterSimulator.spawnInitial(workingGraph, extra, centerX, centerY, centerZ, rng);
                    int baseId = monsters.size();
                    List<MonsterState> merged = new ArrayList<>(monsters);
                    for (int i = 0; i < more.size(); i++) {
                        MonsterState m = more.get(i);
                        merged.add(new MonsterState(baseId + i, m.pos, m.vel, m.targetWaypointX, m.targetWaypointZ, m.direction, false, 0));
                    }
                    monsters = merged;
                    events.add(new GameEvent(GameEventType.STAGE_ADVANCE, stage));
                    events.add(new GameEvent(GameEventType.MONSTER_SPAWN, extra));
                }
                if (liveStartTick >= 0 && tick - liveStartTick >= StageTimer.CENTER_DETERIORATION_START_TICKS
                        && centerDet < 11 && (tick - liveStartTick) % 40 == 0) {
                    centerDet++;
                    CenterDeterioration.apply(workingGraph, centerDet);
                }
            }
        }

        boolean terminal = player.health <= 0 || phase == GamePhase.ENDING;
        if (player.health <= 0) {
            events.add(new GameEvent(GameEventType.ELIMINATED, null));
            phase = GamePhase.ENDING; terminal = true;
        }

        return new TickResult(new GameState(tick, state.mode, phase, stage, Math.max(0, timer), timerMax, centerDet,
                workingGraph.toMazeState(), player, monsters, activePad, previewPad), events, terminal);
    }

    private static PlayerState withOnPad(PlayerState p, boolean onPad) {
        return new PlayerState(p.pos, p.vel, p.yaw, p.pitch, p.onGround, p.health, p.maxHealth, p.kit,
                p.jumpCharges, p.jumpChargeCooldownTicks, p.abilityCharges, p.abilityCooldownTicks, p.enhancedCooldownTicks, p.hitCooldownTicks, onPad);
    }

    private static PlayerState withJumpCharges(PlayerState p, int charges) {
        return new PlayerState(p.pos, p.vel, p.yaw, p.pitch, p.onGround, p.health, p.maxHealth, p.kit,
                charges, p.jumpChargeCooldownTicks, p.abilityCharges, p.abilityCooldownTicks, p.enhancedCooldownTicks, p.hitCooldownTicks, p.onSafePad);
    }
}
