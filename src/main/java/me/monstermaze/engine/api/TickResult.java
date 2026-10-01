package me.monstermaze.engine.api;

import java.util.Collections;
import java.util.List;

/**
 * Result of advancing the simulation by one tick.
 */
public final class TickResult {
    public final GameState next;
    public final List<GameEvent> events;
    /** True when the player is eliminated or the run otherwise ends. */
    public final boolean terminal;

    public TickResult(GameState next, List<GameEvent> events, boolean terminal) {
        this.next = next;
        this.events = events == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(events);
        this.terminal = terminal;
    }
}
