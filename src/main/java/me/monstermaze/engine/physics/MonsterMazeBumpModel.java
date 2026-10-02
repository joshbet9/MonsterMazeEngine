package me.monstermaze.engine.physics;

import me.monstermaze.engine.api.GameState;
import me.monstermaze.engine.api.MazeMode;
import me.monstermaze.engine.api.MonsterState;
import me.monstermaze.engine.api.PlayerState;
import me.monstermaze.engine.api.SafePadState;
import me.monstermaze.engine.api.Vec3;
import me.monstermaze.engine.kit.KitSimulator;
import me.monstermaze.engine.pad.SafePadSimulator;

/** Source-compatible MonsterManager.bump() semantics. */
public final class MonsterMazeBumpModel {
    public static final double CONTACT_DISTANCE = 1.0;
    public static final double DAMAGE = 4.0;
    public static final long HIT_COOLDOWN_TICKS = 20L;
    public static final int RESULT_NONE = 0;
    public static final int RESULT_NORMAL_HIT = 1;
    public static final int RESULT_BODY_RUSH = 2;

    private MonsterMazeBumpModel() {}

    public static int apply(GameState game) {
        PlayerState p = game.player;
        if (p.health <= 0 || p.hitCooldownTicks > 0 || onAnyPad(game, p)) return 0;

        for (MonsterState m : game.monsters) {
            if (m.removed || m.launched(game.tick) || m.frozen(game.tick)) continue;
            if (!contact(p, m)) continue;

            if (KitSimulator.bodyRushActive(p, game.tick)) {
                launchMonsterAway(m, p, game.tick);
                KitSimulator.consumeBodyRushContact(p, game.tick);
                return RESULT_BODY_RUSH;
            }

            boolean wasGrounded = p.onGround;
            double aboveFloor = p.pos.y - game.centerY;
            if (aboveFloor >= 0.0 && aboveFloor < 0.9) {
                // Source checks the player's actual height above the arena floor,
                // not the server grounded flag, before raising them to +0.7.
                p.pos = new Vec3(p.pos.x, game.centerY + 0.7, p.pos.z);
            }

            double dx = p.pos.x - m.pos.x;
            double dz = p.pos.z - m.pos.z;
            double len = Math.hypot(dx, dz);
            double vx;
            double vz;

            if (game.mode != MazeMode.ORIGINAL && p.kit == me.monstermaze.engine.api.KitType.MAVERICK) {
                SafePadState target = game.activePad != null ? game.activePad : game.previewPad;
                double tx = target == null ? 0.0 : target.centerX + 0.5 - p.pos.x;
                double tz = target == null ? 0.0 : target.centerZ + 0.5 - p.pos.z;
                double tlen = Math.hypot(tx, tz);
                if (target != null && tlen > 1e-9) {
                    vx = tx / tlen;
                    vz = tz / tlen;
                } else {
                    vx = fallbackX(p.yaw);
                    vz = fallbackZ(p.yaw);
                }
            } else if (len > 1e-9) {
                vx = dx / len;
                vz = dz / len;
            } else {
                vx = fallbackX(p.yaw);
                vz = fallbackZ(p.yaw);
            }

            double vy = 0.75 + (wasGrounded ? 0.2 : 0.0);
            p.vel = new Vec3(vx, Math.min(1.2, vy), vz);
            p.pendingAirborne = true;
            p.onGround = false;
            p.health -= DAMAGE;
            p.damageTaken += DAMAGE;
            p.hitCooldownTicks = (int) HIT_COOLDOWN_TICKS;
            p.mobHitGraceUntilTick = game.tick + 40L;
            return RESULT_NORMAL_HIT;
        }
        return 0;
    }

    private static void launchMonsterAway(MonsterState m, PlayerState p, long tick) {
        double dx = m.pos.x - p.pos.x;
        double dz = m.pos.z - p.pos.z;
        double len = Math.hypot(dx, dz);
        if (len < 1e-9) { dx = 1.0; dz = 0.0; len = 1.0; }
        m.vel = new Vec3(dx / len, 0.95, dz / len);
        m.launchedAtTick = tick;
        m.launchedUntilTick = tick + 30;
        m.targetWaypointX = -1;
        m.targetWaypointZ = -1;
        m.launched = true;
    }

    private static boolean contact(PlayerState p, MonsterState m) {
        double dx = p.pos.x - m.pos.x;
        double dy = p.pos.y - m.pos.y;
        double dz = p.pos.z - m.pos.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz) < CONTACT_DISTANCE;
    }

    private static boolean onAnyPad(GameState game, PlayerState p) {
        if (SafePadSimulator.isOn(game.activePad, p.pos)
                || SafePadSimulator.isOn(game.previewPad, p.pos)) return true;
        for (SafePadState pad : game.oldPads) {
            if (SafePadSimulator.isOn(pad, p.pos)) return true;
        }
        return false;
    }

    private static double fallbackX(float yaw) { return Math.sin(Math.toRadians(yaw)); }
    private static double fallbackZ(float yaw) { return -Math.cos(Math.toRadians(yaw)); }
}
