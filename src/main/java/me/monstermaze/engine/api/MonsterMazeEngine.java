package me.monstermaze.engine.api;

/**
 * Headless Monster Maze simulation engine.
 *
 * <p>Implementations must be deterministic given the same seed and action sequence.
 */
public interface MonsterMazeEngine {

    /**
     * Create the initial state for a new run.
     *
     * @param mode     game mode (affects timers and spawn rates)
     * @param layoutId 0, 1, or 2 (the three fixed maze patterns)
     * @param kit      player kit
     * @param seed     RNG seed for monster decisions and any other stochastic behaviour
     */
    GameState initialState(MazeMode mode, int layoutId, KitType kit, long seed);

    /**
     * Advance the simulation by one tick.
     */
    TickResult tick(GameState state, Action action);
}
