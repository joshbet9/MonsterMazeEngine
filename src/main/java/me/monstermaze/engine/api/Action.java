package me.monstermaze.engine.api;

/**
 * Per-tick semantic action. Field semantics intentionally match
 * MonsterMazeAI's me.monstermazeai.player.Action so a trained policy can
 * issue the same six control dimensions to the standalone engine and the
 * Minecraft 1.8 adapter.
 */
public final class Action {
    public final double forward;
    public final double strafe;
    public final boolean jump;
    public final boolean sprint;
    public final float yawDelta;
    public final boolean useAbility;
    /** Optional target id for diagnostics/intent; mechanics use the actual collision state. */
    public final Integer approachMonsterId;

    // Compatibility aliases for the original standalone prototype.
    public final double moveX;
    public final double moveZ;
    public final boolean holdJump;

    public Action(double forward, double strafe, boolean jump, boolean sprint,
                  float yawDelta, boolean useAbility) {
        this(forward, strafe, jump, sprint, yawDelta, useAbility, null);
    }

    public Action(double forward, double strafe, boolean jump, boolean sprint,
                  float yawDelta, boolean useAbility, Integer approachMonsterId) {
        if (Double.isNaN(forward) || Double.isInfinite(forward)) forward = 0.0;
        if (Double.isNaN(strafe) || Double.isInfinite(strafe)) strafe = 0.0;
        if (Float.isNaN(yawDelta) || Float.isInfinite(yawDelta)) yawDelta = 0.0f;
        this.forward = clamp(forward);
        this.strafe = clamp(strafe);
        this.jump = jump;
        this.sprint = sprint;
        this.yawDelta = Math.max(-30.0f, Math.min(30.0f, yawDelta));
        this.useAbility = useAbility;
        this.approachMonsterId = approachMonsterId;
        this.moveX = this.strafe;
        this.moveZ = this.forward;
        this.holdJump = this.jump;
    }

    /** Legacy constructor retained for source compatibility with the first UI prototype. */
    public Action(double moveX, double moveZ, boolean sprint, boolean jump, boolean holdJump,
                  float yaw, float pitch, boolean usePrimary, boolean useEnhanced,
                  Integer approachMonsterId) {
        this(moveZ, moveX, jump, sprint, yaw, usePrimary || useEnhanced, approachMonsterId);
    }

    public static Action noop() {
        return new Action(0, 0, false, false, 0, false);
    }

    private static double clamp(double value) {
        return Math.max(-1.0, Math.min(1.0, value));
    }

    @Override
    public String toString() {
        return "Action[forward=" + forward + ", strafe=" + strafe
                + ", jump=" + jump + ", sprint=" + sprint
                + ", yawDelta=" + yawDelta + ", useAbility=" + useAbility + "]";
    }
}
