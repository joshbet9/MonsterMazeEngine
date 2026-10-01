package me.monstermaze.engine.game;

import me.monstermaze.engine.api.*;

import java.util.Collections;

/**
 * Placeholder implementation. Real 1.8 mechanics will be filled in incrementally.
 */
public final class EngineImpl implements MonsterMazeEngine {

    @Override
    public GameState initialState(MazeMode mode, int layoutId, KitType kit, long seed) {
        // TODO: load layout, place player at spawn, create initial monsters, start stage timer
        MazeState maze = new MazeState(layoutId, new int[99][99], new boolean[99][99]);
        PlayerState player = new PlayerState(
                Vec3.ZERO,
                Vec3.ZERO,
                0f, 0f,
                true,
                20.0, 20.0,
                kit,
                kit == KitType.JUMPER ? (mode == MazeMode.ORIGINAL ? 5 : 3) : 0,
                0,
                0, 0, 0, 0,
                false
        );
        return new GameState(
                0L,
                mode,
                GamePhase.STARTING,
                0,
                0,
                0,
                0,
                maze,
                player,
                Collections.emptyList(),
                null,
                null
        );
    }

    @Override
    public TickResult tick(GameState state, Action action) {
        // TODO: apply action, step physics, monsters, pads, timers
        return new TickResult(state, Collections.emptyList(), false);
    }
}
