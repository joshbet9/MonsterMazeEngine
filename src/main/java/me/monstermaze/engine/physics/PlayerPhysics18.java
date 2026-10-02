package me.monstermaze.engine.physics;

import me.monstermaze.engine.api.Action;
import me.monstermaze.engine.api.PlayerState;
import me.monstermaze.engine.api.Vec3;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.MazeGraph;

/**
 * Monster Maze's 1.8 movement model, aligned with MonsterMazeAI's
 * LegacyMovementModel. The engine treats the Minecraft client as an external
 * executor of the same semantic Action.
 */
public final class PlayerPhysics18 {
    private static final float SLIPPERINESS = 0.6F;
    private static final float GROUND_FRICTION = 0.91F;
    private static final float WALK_SPEED = 0.10F;
    private static final float SPRINT_MULTIPLIER = 1.30F;
    private static final float AIR_MOVE_FACTOR = 0.02F;
    private static final double GRAVITY = 0.08D;
    private static final double AIR_DRAG = 0.9800000190734863D;
    private static final double JUMP_VELOCITY = 0.42D;
    private static final double SPRINT_JUMP_IMPULSE = 0.2D;

    private final MazeCollision collision;

    public PlayerPhysics18(MazeGraph maze, int centerX, int centerY, int centerZ) {
        this.collision = new MazeCollision(maze, centerX, centerY, centerZ);
    }

    public void tick(PlayerState p, Action action, int jumpAmplifier) {
        p.yaw = normalise(p.yaw + action.yawDelta);
        boolean groundedAtStart = p.onGround;
        float friction = groundedAtStart ? SLIPPERINESS * GROUND_FRICTION : GROUND_FRICTION;

        if (action.jump && groundedAtStart && p.jumpTicks == 0) {
            if (jumpAmplifier <= -2) {
                p.vel = new Vec3(p.vel.x - Math.sin(Math.toRadians(p.yaw)) * SPRINT_JUMP_IMPULSE * (action.sprint ? 1.0 : 0.0),
                        0.0,
                        p.vel.z + Math.cos(Math.toRadians(p.yaw)) * SPRINT_JUMP_IMPULSE * (action.sprint ? 1.0 : 0.0));
                p.jumpTicks = 0;
            } else {
                double vy = JUMP_VELOCITY + (jumpAmplifier > 0 ? ((jumpAmplifier + 1) * 0.1D) : 0.0D);
                double vx = p.vel.x;
                double vz = p.vel.z;
                if (action.sprint) {
                    double yaw = Math.toRadians(p.yaw);
                    vx -= Math.sin(yaw) * SPRINT_JUMP_IMPULSE;
                    vz += Math.cos(yaw) * SPRINT_JUMP_IMPULSE;
                }
                p.vel = new Vec3(vx, vy, vz);
                p.onGround = false;
                p.jumpTicks = 10;
            }
        } else if (!action.jump) {
            p.jumpTicks = 0;
        } else if (p.jumpTicks > 0) {
            p.jumpTicks--;
        }

        float movementFactor;
        if (groundedAtStart) {
            movementFactor = (float) (WALK_SPEED
                    * (action.sprint ? SPRINT_MULTIPLIER : 1.0F)
                    * (0.16277136F / Math.pow(friction, 3)));
        } else {
            movementFactor = AIR_MOVE_FACTOR
                    * (action.sprint ? SPRINT_MULTIPLIER : 1.0F);
        }

        Vec3 v = p.vel;
        Vec3 added = moveFlying(p.yaw, action.strafe, action.forward, movementFactor);
        p.vel = new Vec3(v.x + added.x, v.y + added.y, v.z + added.z);

        double dx = p.vel.x;
        double dy = p.vel.y;
        double dz = p.vel.z;
        collision.move(p, dx, dy, dz);

        if (!p.onGround && p.pos.y <= collision.floorY()) {
            p.pos = new Vec3(p.pos.x, collision.floorY(), p.pos.z);
            p.vel = new Vec3(p.vel.x, 0.0, p.vel.z);
            p.onGround = true;
        }

        if (!p.onGround) {
            p.vel = new Vec3(p.vel.x, (p.vel.y - GRAVITY) * AIR_DRAG, p.vel.z);
        }

        p.vel = new Vec3(p.vel.x * friction, p.vel.y, p.vel.z * friction);
        if (Math.abs(p.vel.x) < 0.005) p.vel = new Vec3(0.0, p.vel.y, p.vel.z);
        if (Math.abs(p.vel.y) < 0.005) p.vel = new Vec3(p.vel.x, 0.0, p.vel.z);
        if (Math.abs(p.vel.z) < 0.005) p.vel = new Vec3(p.vel.x, p.vel.y, 0.0);
    }

    private static Vec3 moveFlying(float yawDegrees, double strafe, double forward, double factor) {
        double magnitude = strafe * strafe + forward * forward;
        if (magnitude < 1.0E-4) return Vec3.ZERO;
        magnitude = Math.sqrt(magnitude);
        if (magnitude < 1.0) magnitude = 1.0;
        double scale = factor / magnitude;
        strafe *= scale;
        forward *= scale;
        double yaw = Math.toRadians(yawDegrees);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        return new Vec3(strafe * cos - forward * sin, 0.0,
                forward * cos + strafe * sin);
    }

    private static float normalise(float yaw) {
        while (yaw >= 180.0F) yaw -= 360.0F;
        while (yaw < -180.0F) yaw += 360.0F;
        return yaw;
    }
}
