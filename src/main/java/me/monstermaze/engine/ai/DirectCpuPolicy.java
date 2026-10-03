package me.monstermaze.engine.ai;

import me.monstermaze.engine.api.Action;
import me.monstermaze.engine.api.GameState;

/**
 * Production CPU-policy boundary.
 *
 * <p>The policy receives one authoritative game state and emits one semantic
 * player action. Implementations must not mutate the state or invoke search
 * over simulated candidate actions.
 */
public interface DirectCpuPolicy {
    void reset(long seed);
    Action decide(GameState state);
}
