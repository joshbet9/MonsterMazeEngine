package me.monstermaze.engine.kit;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.pad.SafePadSimulator;

import java.util.ArrayList;
import java.util.List;

import static me.monstermaze.engine.kit.KitConstants.*;

/** Kit/resource semantics aligned with MonsterMazeAI's common AbilityModel. */
public final class KitSimulator {
    private static final int JUMPER_RECHARGE_TICKS = 15;
    private static final int JUMPER_POST_HIT_GRACE_TICKS = 40;
    private static final int CRYO_COOLDOWN_TICKS = 600;
    private static final int CRYO_FREEZE_TICKS = 60;
    private static final int BODY_RUSH_TICKS = 200;
    private static final int BODY_RUSH_CONTACT_PENALTY_TICKS = 40;

    private KitSimulator() {}

    public static int initialAbilityCharges(KitType kit) {
        return switch (kit) {
            case JUMPER -> 3;
            case SLOWBALL -> 1;
            case REPULSOR -> 3;
            default -> 0;
        };
    }

    public static int initialAbilityActivations(KitType kit) {
        return kit == KitType.BODY_BUILDER ? 2 : 0;
    }

    public static void tickResources(PlayerState p, MazeMode mode, long tick) {
        if (p.kit == KitType.SLOWBALL && p.abilityCharges < SLOWBALL_MAX) {
            if (p.abilityCooldownUntilTick <= tick) {
                p.abilityCharges++;
                p.abilityCooldownUntilTick = tick + SLOWBALL_REGEN_TICKS;
            }
        }
    }

    public static void activate(PlayerState p, List<MonsterState> monsters,
                                Action action, MazeMode mode, SafePadState activePad,
                                SafePadState previewPad, long tick) {
        if (!action.useAbility) return;

        switch (p.kit) {
            case REPULSOR -> activateRepulsor(p, monsters, tick);
            case SLOWBALL -> activateCryo(p, monsters, mode, tick);
            case BODY_BUILDER -> activateBodyRush(p, mode, tick);
            default -> {
            }
        }
    }

    private static void activateRepulsor(PlayerState p, List<MonsterState> monsters, long tick) {
        if (p.abilityCharges <= 0) return;
        p.abilityCharges--;
        for (MonsterState m : monsters) {
            if (m.removed || m.launched(tick)) continue;
            double dx = m.pos.x - p.pos.x;
            double dz = m.pos.z - p.pos.z;
            double d2 = dx * dx + dz * dz;
            if (d2 > 36.0) continue;
            double len = Math.sqrt(d2);
            if (len < 1e-9) { dx = 1.0; dz = 0.0; len = 1.0; }
            m.vel = new Vec3(dx / len, 1.0, dz / len);
            m.launchedAtTick = tick;
            m.launchedUntilTick = tick + 30;
            m.targetWaypointX = -1;
            m.targetWaypointZ = -1;
            m.launched = true;
        }
    }

    private static void activateCryo(PlayerState p, List<MonsterState> monsters,
                                     MazeMode mode, long tick) {
        if (mode == MazeMode.ORIGINAL || tick < p.abilityCooldownUntilTick) return;
        p.abilityCooldownUntilTick = tick + CRYO_COOLDOWN_TICKS;
        for (MonsterState m : monsters) {
            if (m.removed) continue;
            double dx = p.pos.x - m.pos.x;
            double dy = p.pos.y - m.pos.y;
            double dz = p.pos.z - m.pos.z;
            if (dx * dx + dy * dy + dz * dz <= 36.0) {
                m.frozenUntilTick = Math.max(m.frozenUntilTick, tick + CRYO_FREEZE_TICKS);
                m.frozenTicks = CRYO_FREEZE_TICKS;
                m.vel = Vec3.ZERO;
            }
        }
    }

    private static void activateBodyRush(PlayerState p, MazeMode mode, long tick) {
        if (mode == MazeMode.ORIGINAL
                || p.abilityActivations <= 0
                || p.abilityActiveUntilTick > tick) return;
        p.abilityActivations--;
        p.abilityActiveUntilTick = tick + BODY_RUSH_TICKS;
    }

    public static boolean consumeJumperCharge(PlayerState p, MazeMode mode,
                                              SafePadState activePad, SafePadState previewPad,
                                              long tick) {
        if (p.kit != KitType.JUMPER || p.jumpCharges <= 0) return false;
        if (tick < p.nextJumpChargeTick || tick < p.mobHitGraceUntilTick) return false;
        boolean onPad = SafePadSimulator.isOn(activePad, p.pos)
                || SafePadSimulator.isOn(previewPad, p.pos);
        if (mode != MazeMode.ORIGINAL && onPad) return false;
        p.jumpCharges--;
        p.nextJumpChargeTick = tick + JUMPER_RECHARGE_TICKS;
        return true;
    }

    public static boolean bodyRushActive(PlayerState p, long tick) {
        return p.kit == KitType.BODY_BUILDER
                && p.abilityActiveUntilTick > tick;
    }

    public static void consumeBodyRushContact(PlayerState p, long tick) {
        if (bodyRushActive(p, tick)) {
            p.abilityActiveUntilTick = Math.max(tick,
                    p.abilityActiveUntilTick - BODY_RUSH_CONTACT_PENALTY_TICKS);
        }
    }

    public static void onReachedPad(PlayerState p, MazeMode mode, boolean first) {
        if (p.kit == KitType.JUMPER && mode != MazeMode.ORIGINAL) p.jumpCharges = 3;
        if (p.kit == KitType.BODY_BUILDER && first) {
            p.maxHealth = Math.min(BODY_MAX_HP, p.maxHealth + BODY_FIRST_PAD_MAX_GAIN);
            p.health = Math.min(p.maxHealth, p.health + BODY_FIRST_HEAL);
        } else if (first) {
            p.health = Math.min(p.maxHealth, p.health + BODY_FIRST_HEAL);
        } else {
            p.health = Math.min(p.maxHealth, p.health + BODY_LATER_HEAL);
        }
    }
}
