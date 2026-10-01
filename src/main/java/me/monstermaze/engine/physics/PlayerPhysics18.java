package me.monstermaze.engine.physics;

import me.monstermaze.engine.api.Action;
import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.PlayerState;
import me.monstermaze.engine.api.Vec3;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;

/**
 * Simplified but tick-faithful 1.8-style player movement for the pure engine.
 *
 * <p>Models sprint walk, jump impulse, gravity, Jumper charge consumption (750ms gate),
 * and legacy speeding (rapid jump while sprinting ~1.5x). Speeding numbers are approximate.
 */
public final class PlayerPhysics18 {

    public static final double WALK_SPEED = 0.22;
    public static final double SPRINT_SPEED = 0.28;
    public static final double JUMP_VELOCITY = 0.42;
    public static final double GRAVITY = 0.08;
    public static final double DRAG_AIR = 0.91;
    public static final double SPEEDING_MULT = 1.5;
    public static final int JUMPER_CHARGE_GATE_TICKS = 15;

    private PlayerPhysics18() {}

    public static PlayerState step(
            PlayerState p,
            Action action,
            MazeGraph graph,
            int centerX,
            int centerY,
            int centerZ,
            boolean padJumpFree) {

        double yawRad = Math.toRadians(action.yaw);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        double intentX = action.moveZ * forwardX + action.moveX * rightX;
        double intentZ = action.moveZ * forwardZ + action.moveX * rightZ;
        double intentLen = Math.sqrt(intentX * intentX + intentZ * intentZ);
        if (intentLen > 1e-6) {
            intentX /= intentLen;
            intentZ /= intentLen;
        }

        boolean wantJump = action.jump || action.holdJump;
        boolean canSpendCharge = p.kit == KitType.JUMPER
                && p.jumpCharges > 0
                && p.jumpChargeCooldownTicks <= 0
                && p.onGround;

        int jumpCharges = p.jumpCharges;
        int jumpCd = Math.max(0, p.jumpChargeCooldownTicks - 1);
        double vx = p.vel.x;
        double vy = p.vel.y;
        double vz = p.vel.z;
        boolean onGround = p.onGround;

        boolean speeding = action.sprint && wantJump && onGround && !canSpendCharge
                && intentLen > 0.1;
        double speed = action.sprint ? SPRINT_SPEED : WALK_SPEED;
        if (speeding) {
            speed *= SPEEDING_MULT;
        }

        if (onGround) {
            vx = intentX * speed;
            vz = intentZ * speed;

            if (wantJump) {
                if (canSpendCharge || padJumpFree) {
                    vy = JUMP_VELOCITY;
                    onGround = false;
                    if (canSpendCharge && !padJumpFree) {
                        jumpCharges--;
                        jumpCd = JUMPER_CHARGE_GATE_TICKS;
                    }
                } else if (p.kit != KitType.JUMPER || p.jumpCharges <= 0) {
                    vy = JUMP_VELOCITY * 0.85;
                    onGround = false;
                }
            }
        } else {
            vx += intentX * speed * 0.02;
            vz += intentZ * speed * 0.02;
            vy -= GRAVITY;
            vx *= DRAG_AIR;
            vz *= DRAG_AIR;
        }

        double nx = p.pos.x + vx;
        double ny = p.pos.y + vy;
        double nz = p.pos.z + vz;

        int row = Coordinates.layoutRow(centerX, (int) Math.floor(nx));
        int col = Coordinates.layoutCol(centerZ, (int) Math.floor(nz));
        boolean onPath = Coordinates.inBounds(row, col) && Layouts.isRawPath(graph.raw(row, col));

        if (ny <= centerY) {
            ny = centerY;
            vy = 0;
            onGround = true;
        } else {
            onGround = false;
        }

        if (!onPath && onGround) {
            nx = p.pos.x;
            nz = p.pos.z;
            vx = 0;
            vz = 0;
        }

        int hitCd = Math.max(0, p.hitCooldownTicks - 1);

        return new PlayerState(
                new Vec3(nx, ny, nz),
                new Vec3(vx, vy, vz),
                action.yaw,
                action.pitch,
                onGround,
                p.health,
                p.maxHealth,
                p.kit,
                jumpCharges,
                jumpCd,
                p.abilityCharges,
                Math.max(0, p.abilityCooldownTicks - 1),
                Math.max(0, p.enhancedCooldownTicks - 1),
                hitCd,
                p.onSafePad
        );
    }
}
