package me.monstermaze.engine.physics;

import me.monstermaze.engine.api.Vec3;

/**
 * Exact port of Mineplex UtilAction.velocity as used by Maze.bump():
 * velocity(player, trajectory, 1.0, false, 0, 0.75, 1.2, true)
 */
public final class Knockback {

    public static final double STRENGTH = 1.0;
    public static final double Y_ADD = 0.75;
    public static final double Y_MAX = 1.2;
    public static final double GROUND_BOOST = 0.2;
    public static final double DAMAGE = 4.0;
    public static final int HIT_COOLDOWN_TICKS = 20;
    /** Horizontal+3D contact radius squared threshold from MonsterManager. */
    public static final double CONTACT_RADIUS_SQ = 1.0;

    private Knockback() {}

    /**
     * Compute launch velocity from monster \u2192 player direction.
     */
    public static Vec3 compute(Vec3 fromMonster, Vec3 toPlayer, boolean grounded) {
        double dx = toPlayer.x - fromMonster.x;
        double dz = toPlayer.z - fromMonster.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-9) {
            dx = 0;
            dz = 1;
            len = 1;
        }
        dx /= len;
        dz /= len;

        double vx = dx * STRENGTH;
        double vz = dz * STRENGTH;
        double vy = Y_ADD;
        if (vy > Y_MAX) {
            vy = Y_MAX;
        }
        if (grounded) {
            vy += GROUND_BOOST;
        }
        return new Vec3(vx, vy, vz);
    }
}
