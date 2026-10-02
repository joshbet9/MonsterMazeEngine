package me.monstermaze.engine.api;

/**
 * Mutable player simulation state. The fields mirror the state needed by the
 * MonsterMazeAI common simulator; the engine returns immutable GameState
 * snapshots around this mutable per-tick payload.
 */
public final class PlayerState {
    public Vec3 pos;
    public Vec3 vel;
    public float yaw;
    public float pitch;
    public boolean onGround;
    public boolean pendingAirborne;
    public double health;
    public double maxHealth;
    public final KitType kit;

    public int jumpCharges;
    public long nextJumpChargeTick;
    public long mobHitGraceUntilTick;
    public int jumpTicks;

    public int abilityCharges;
    public int abilityActivations;
    public long abilityCooldownUntilTick;
    public long abilityActiveUntilTick;

    public int hitCooldownTicks;
    public double damageTaken;
    public boolean onSafePad;

    public PlayerState(Vec3 pos, Vec3 vel, float yaw, float pitch, boolean onGround,
                       double health, double maxHealth, KitType kit,
                       int jumpCharges, int jumpChargeCooldownTicks,
                       int abilityCharges, int abilityCooldownTicks,
                       int enhancedCooldownTicks, int hitCooldownTicks,
                       boolean onSafePad) {
        this.pos = pos;
        this.vel = vel;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.pendingAirborne = false;
        this.health = health;
        this.maxHealth = maxHealth;
        this.kit = kit;
        this.jumpCharges = jumpCharges;
        this.nextJumpChargeTick = jumpChargeCooldownTicks;
        this.mobHitGraceUntilTick = 0L;
        this.jumpTicks = 0;
        this.abilityCharges = abilityCharges;
        this.abilityActivations = kit == KitType.BODY_BUILDER ? 2 : 0;
        this.abilityCooldownUntilTick = abilityCooldownTicks;
        this.abilityActiveUntilTick = enhancedCooldownTicks;
        this.hitCooldownTicks = hitCooldownTicks;
        this.damageTaken = 0.0;
        this.onSafePad = onSafePad;
    }

    public PlayerState copy() {
        PlayerState p = new PlayerState(
                pos, vel, yaw, pitch, onGround,
                health, maxHealth, kit,
                jumpCharges, 0, abilityCharges, 0, 0,
                hitCooldownTicks, onSafePad);
        p.pos = pos;
        p.vel = vel;
        p.pendingAirborne = pendingAirborne;
        p.nextJumpChargeTick = nextJumpChargeTick;
        p.mobHitGraceUntilTick = mobHitGraceUntilTick;
        p.jumpTicks = jumpTicks;
        p.abilityActivations = abilityActivations;
        p.abilityCooldownUntilTick = abilityCooldownUntilTick;
        p.abilityActiveUntilTick = abilityActiveUntilTick;
        p.damageTaken = damageTaken;
        return p;
    }
}
