package me.monstermaze.engine.api;

/**
 * Player action for a single tick.
 * Semantic rather than raw keypresses so the AI planner and human input can share the same interface.
 */
public final class Action {
    /** Desired horizontal movement intent (typically -1..1). */
    public final double moveX;
    public final double moveZ;
    public final boolean sprint;
    /** Single jump press this tick. */
    public final boolean jump;
    /** Held jump (for continuous Jumper charges / 1.8 spam behaviour). */
    public final boolean holdJump;
    public final float yaw;
    public final float pitch;
    public final boolean usePrimary;
    public final boolean useEnhanced;
    /** Non-null when the agent deliberately approaches a specific monster for knockback. */
    public final Integer approachMonsterId;

    public Action(
            double moveX,
            double moveZ,
            boolean sprint,
            boolean jump,
            boolean holdJump,
            float yaw,
            float pitch,
            boolean usePrimary,
            boolean useEnhanced,
            Integer approachMonsterId) {
        this.moveX = moveX;
        this.moveZ = moveZ;
        this.sprint = sprint;
        this.jump = jump;
        this.holdJump = holdJump;
        this.yaw = yaw;
        this.pitch = pitch;
        this.usePrimary = usePrimary;
        this.useEnhanced = useEnhanced;
        this.approachMonsterId = approachMonsterId;
    }

    public static Action noop() {
        return new Action(0, 0, false, false, false, 0f, 0f, false, false, null);
    }
}
