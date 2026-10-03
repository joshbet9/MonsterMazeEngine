package me.monstermaze.engine.runtime;

import me.monstermaze.engine.api.Action;
import me.monstermaze.engine.api.GameState;
import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.MazeMode;
import me.monstermaze.engine.api.MonsterMazeEngine;
import me.monstermaze.engine.api.TickResult;
import me.monstermaze.engine.game.EngineImpl;

/**
 * Owns exactly one independent simulation episode.
 *
 * <p>The session boundary is important for parallel training: each episode
 * gets its own EngineImpl, RNG streams, maze graph and physics runtime.
 */
public final class SimulationSession {
    private final MonsterMazeEngine engine;
    private GameState state;

    private SimulationSession(MonsterMazeEngine engine, GameState state) {
        this.engine = engine;
        this.state = state;
    }

    public static SimulationSession create(MazeMode mode, int layoutId, KitType kit, long seed) {
        MonsterMazeEngine engine = new EngineImpl();
        return new SimulationSession(
                engine,
                engine.initialState(mode, layoutId, kit, seed));
    }

    public static SimulationSession create(MazeMode mode, int layoutId, KitType kit,
                                            long seed, int starterOverride) {
        MonsterMazeEngine engine = new EngineImpl(starterOverride);
        return new SimulationSession(
                engine,
                engine.initialState(mode, layoutId, kit, seed));
    }

    public TickResult step(Action action) {
        TickResult result = engine.tick(state, action);
        state = result.next;
        return result;
    }

    public GameState state() {
        return state;
    }

    public boolean terminal() {
        return !state.alive || state.phase.name().equals("ENDING");
    }

    public MonsterMazeEngine engine() {
        return engine;
    }
}
