package me.monstermaze.engine.api;

/**
 * Snapshot of the player at a given tick.
 */
public final class PlayerState {
    public final Vec3 pos;
    public final Vec3 vel;
    public final float yaw;
    public final float pitch;
    public final boolean onGround;
    public final double health;
    public final double maxHealth;
    public final KitType kit;
    public final int jumpCharges;
    public final int jumpChargeCooldownTicks;
    /** Kit-specific primary ability charges (e.g. snowballs, repulsor charges). */
    public final int abilityCharges;
    public final int abilityCooldownTicks;
    public final int enhancedCooldownTicks;
    public final int hitCooldownTicks;
    public final boolean onSafePad;

    public PlayerState(
            Vec3 pos,
            Vec3 vel,
            float yaw,
            float pitch,
            boolean onGround,
            double health,
            double maxHealth,
            KitType kit,
            int jumpCharges,
            int jumpChargeCooldownTicks,
            int abilityCharges,
            int abilityCooldownTicks,
            int enhancedCooldownTicks,
            int hitCooldownTicks,
            boolean onSafePad) {
        this.pos = pos;
        this.vel = vel;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.health = health;
        this.maxHealth = maxHealth;
        this.kit = kit;
        this.jumpCharges = jumpCharges;
        this.jumpChargeCooldownTicks = jumpChargeCooldownTicks;
        this.abilityCharges = abilityCharges;
        this.abilityCooldownTicks = abilityCooldownTicks;
        this.enhancedCooldownTicks = enhancedCooldownTicks;
        this.hitCooldownTicks = hitCooldownTicks;
        this.onSafePad = onSafePad;
    }
}
