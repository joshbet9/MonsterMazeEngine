package me.monstermaze.engine.api;

/**
 * Discrete event that occurred during a tick.
 */
public final class GameEvent {
    public final GameEventType type;
    public final Object data;

    public GameEvent(GameEventType type) {
        this(type, null);
    }

    public GameEvent(GameEventType type, Object data) {
        this.type = type;
        this.data = data;
    }
}
