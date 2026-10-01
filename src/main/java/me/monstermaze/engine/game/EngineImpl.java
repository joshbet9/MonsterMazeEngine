package me.monstermaze.engine.game;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;

import java.util.Collections;

/**
 * Headless 1.8-faithful engine. Mechanics are filled in incrementally;
 * maze loading and coordinate mapping are live.
 */
public final class EngineImpl implements MonsterMazeEngine {

    private final int centerX;
    private final int centerY;
    private final int centerZ;

    public EngineImpl() {
        this(Coordinates.DEFAULT_CENTER_X,
                Coordinates.DEFAULT_CENTER_Y,
                Coordinates.DEFAULT_CENTER_Z);
    }

    public EngineImpl(int centerX, int centerY, int centerZ) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
    }

    @Override
    public GameState initialState(MazeMode mode, int layoutId, KitType kit, long seed) {
        if (layoutId < 0 || layoutId >= Layouts.LAYOUT_COUNT) {
            throw new IllegalArgumentException(
                    "layoutId must be 0.." + (Layouts.LAYOUT_COUNT - 1));
        }

        MazeGraph graph = new MazeGraph(layoutId);

        int spawnRow = Layouts.HALF;
        int spawnCol = Layouts.HALF;
        int[][] spawns = graph.spawnCells();
        if (spawns.length > 0) {
            double best = Double.MAX_VALUE;
            for (int[] sc : spawns) {
                double d = (sc[0] - Layouts.HALF) * (sc[0] - Layouts.HALF)
                        + (sc[1] - Layouts.HALF) * (sc[1] - Layouts.HALF);
                if (d < best) {
                    best = d;
                    spawnRow = sc[0];
                    spawnCol = sc[1];
                }
            }
        }

        double px = Coordinates.pathCenterX(centerX, spawnRow);
        double pz = Coordinates.pathCenterZ(centerZ, spawnCol);
        Vec3 pos = new Vec3(px, centerY, pz);

        int jumpCharges = 0;
        if (kit == KitType.JUMPER) {
            jumpCharges = (mode == MazeMode.ORIGINAL) ? 5 : 3;
        }

        PlayerState player = new PlayerState(
                pos,
                Vec3.ZERO,
                0f, 0f,
                true,
                20.0, 20.0,
                kit,
                jumpCharges,
                0,
                0, 0, 0, 0,
                false
        );

        int phaseMax = phaseTimerMaxTicks(mode, 0);

        return new GameState(
                0L,
                mode,
                GamePhase.STARTING,
                0,
                phaseMax,
                phaseMax,
                0,
                graph.toMazeState(),
                player,
                Collections.emptyList(),
                null,
                null
        );
    }

    @Override
    public TickResult tick(GameState state, Action action) {
        GameState next = new GameState(
                state.tick + 1,
                state.mode,
                state.phase == GamePhase.STARTING && state.tick + 1 >= 200
                        ? GamePhase.LIVE
                        : state.phase,
                state.stage,
                state.phaseTimerTicks,
                state.phaseTimerMax,
                state.centerDeteriorationStep,
                state.maze,
                state.player,
                state.monsters,
                state.activePad,
                state.previewPad
        );
        return new TickResult(next, Collections.emptyList(), false);
    }

    /**
     * Initial phase length in ticks (20 ticks/s).
     * Original/Speed: 60s stepping down; Modern: 35s progression.
     */
    static int phaseTimerMaxTicks(MazeMode mode, int stage) {
        int startSec = (mode == MazeMode.MODERN) ? 35 : 60;
        int sec = Math.max(15, startSec - 2 * stage);
        return sec * 20;
    }
}
