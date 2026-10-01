package me.monstermaze.engine.kit;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.physics.Knockback;

import java.util.ArrayList;
import java.util.List;

import static me.monstermaze.engine.kit.KitConstants.*;

/** Kit ability simulation for 1.8 mechanics. */
public final class KitSimulator {

    private KitSimulator() {}

    public static int initialAbilityCharges(KitType kit) {
        return KitConstants.initialAbilityCharges(kit);
    }

    public static KitTickResult applyAbilities(
            PlayerState player,
            List<MonsterState> monsters,
            Action action,
            MazeMode mode,
            SafePadState activePad,
            SafePadState previewPad,
            boolean qol) {

        List<GameEvent> events = new ArrayList<>();
        List<MonsterState> mons = new ArrayList<>(monsters);
        PlayerState p = player;

        if (p.kit == KitType.SLOWBALL) {
            int charges = p.abilityCharges;
            int cd = p.abilityCooldownTicks;
            if (charges < SLOWBALL_MAX) {
                if (cd <= 0) { charges++; cd = SLOWBALL_REGEN_TICKS; }
                else cd--;
            }
            p = withAbility(p, charges, cd, p.enhancedCooldownTicks);
        }

        if (action.usePrimary && p.kit == KitType.REPULSOR && p.abilityCharges > 0) {
            int hit = 0;
            List<MonsterState> updated = new ArrayList<>();
            for (MonsterState m : mons) {
                double dx = m.pos.x - p.pos.x;
                double dz = m.pos.z - p.pos.z;
                if (dx * dx + dz * dz <= REPULSOR_RADIUS * REPULSOR_RADIUS && !m.launched) {
                    double len = Math.sqrt(dx * dx + dz * dz);
                    if (len < 1e-6) { dx = 1; dz = 0; len = 1; }
                    dx /= len; dz /= len;
                    Vec3 launchVel = new Vec3(dx * 1.2, 0.8, dz * 1.2);
                    updated.add(new MonsterState(m.id, m.pos, launchVel,
                            m.targetWaypointX, m.targetWaypointZ, m.direction, true, m.frozenTicks));
                    hit++;
                } else updated.add(m);
            }
            mons = updated;
            p = withAbility(p, p.abilityCharges - 1, p.abilityCooldownTicks, p.enhancedCooldownTicks);
            events.add(new GameEvent(GameEventType.ABILITY_USED, "repulsor:" + hit));
        }

        if (action.usePrimary && p.kit == KitType.SLOWBALL && p.abilityCharges > 0) {
            int left = p.abilityCharges - 1;
            int cd = (left < SLOWBALL_MAX && p.abilityCooldownTicks <= 0) ? SLOWBALL_REGEN_TICKS : p.abilityCooldownTicks;
            p = withAbility(p, left, cd, p.enhancedCooldownTicks);
            events.add(new GameEvent(GameEventType.ABILITY_USED, "slowball"));
        }

        if (action.useEnhanced && qol && p.kit == KitType.SLOWBALL && p.enhancedCooldownTicks <= 0) {
            List<MonsterState> updated = new ArrayList<>();
            int frozen = 0;
            for (MonsterState m : mons) {
                double dx = m.pos.x - p.pos.x;
                double dz = m.pos.z - p.pos.z;
                if (dx * dx + dz * dz <= CRYO_RADIUS * CRYO_RADIUS) {
                    updated.add(new MonsterState(m.id, m.pos, Vec3.ZERO,
                            m.targetWaypointX, m.targetWaypointZ, m.direction, m.launched, CRYO_FREEZE_TICKS));
                    frozen++;
                } else updated.add(m);
            }
            mons = updated;
            p = withAbility(p, p.abilityCharges, p.abilityCooldownTicks, CRYO_COOLDOWN_TICKS);
            events.add(new GameEvent(GameEventType.ABILITY_USED, "cryo:" + frozen));
        }

        if (action.useEnhanced && qol && p.kit == KitType.BODY_BUILDER
                && p.abilityCharges > 0 && p.abilityCooldownTicks <= 0) {
            p = withAbility(p, p.abilityCharges - 1, BODY_RUSH_DURATION_TICKS, p.enhancedCooldownTicks);
            events.add(new GameEvent(GameEventType.ABILITY_USED, "body_rush"));
        }

        if (p.kit == KitType.BODY_BUILDER && p.abilityCooldownTicks > 0) {
            p = withAbility(p, p.abilityCharges, p.abilityCooldownTicks - 1, p.enhancedCooldownTicks);
        }
        if (p.enhancedCooldownTicks > 0 && p.kit == KitType.SLOWBALL) {
            p = withAbility(p, p.abilityCharges, p.abilityCooldownTicks, p.enhancedCooldownTicks - 1);
        }

        return new KitTickResult(p, mons, events);
    }

    public static boolean isBodyRushActive(PlayerState p) {
        return p.kit == KitType.BODY_BUILDER && p.abilityCooldownTicks > 0;
    }

    public static PlayerState bodyRushConsumeContact(PlayerState p) {
        return withAbility(p, p.abilityCharges,
                Math.max(0, p.abilityCooldownTicks - BODY_RUSH_CONTACT_COST_TICKS),
                p.enhancedCooldownTicks);
    }

    public static MonsterState launchMonsterAway(MonsterState m, Vec3 fromPlayer) {
        double dx = m.pos.x - fromPlayer.x;
        double dz = m.pos.z - fromPlayer.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-6) { dx = 1; dz = 0; len = 1; }
        dx /= len; dz /= len;
        return new MonsterState(m.id, m.pos, new Vec3(dx * 1.2, 0.8, dz * 1.2),
                m.targetWaypointX, m.targetWaypointZ, m.direction, true, m.frozenTicks);
    }

    public static Vec3 maverickKnockback(Vec3 playerPos, SafePadState pad, boolean grounded) {
        if (pad == null) {
            return new Vec3(0, Knockback.Y_ADD + (grounded ? Knockback.GROUND_BOOST : 0), 0);
        }
        double tx = pad.centerX + 0.5;
        double tz = pad.centerZ + 0.5;
        return Knockback.compute(playerPos, new Vec3(tx, playerPos.y, tz), grounded);
    }

    public static PlayerState applyBodyBuilderFirstPad(PlayerState p, boolean first) {
        if (p.kit != KitType.BODY_BUILDER) return p;
        double maxHp = p.maxHealth;
        double hp = p.health;
        if (first) {
            maxHp = Math.min(BODY_MAX_HP, maxHp + BODY_FIRST_PAD_MAX_GAIN);
            hp = Math.min(maxHp, hp + BODY_FIRST_HEAL);
        } else {
            hp = Math.min(maxHp, hp + BODY_LATER_HEAL);
        }
        return new PlayerState(p.pos, p.vel, p.yaw, p.pitch, p.onGround,
                hp, maxHp, p.kit, p.jumpCharges, p.jumpChargeCooldownTicks,
                p.abilityCharges, p.abilityCooldownTicks, p.enhancedCooldownTicks,
                p.hitCooldownTicks, p.onSafePad);
    }

    private static PlayerState withAbility(PlayerState p, int charges, int abCd, int enhCd) {
        return new PlayerState(p.pos, p.vel, p.yaw, p.pitch, p.onGround,
                p.health, p.maxHealth, p.kit, p.jumpCharges, p.jumpChargeCooldownTicks,
                charges, abCd, enhCd, p.hitCooldownTicks, p.onSafePad);
    }

    public static final class KitTickResult {
        public final PlayerState player;
        public final List<MonsterState> monsters;
        public final List<GameEvent> events;
        public KitTickResult(PlayerState player, List<MonsterState> monsters, List<GameEvent> events) {
            this.player = player; this.monsters = monsters; this.events = events;
        }
    }
}
