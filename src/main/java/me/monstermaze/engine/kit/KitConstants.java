package me.monstermaze.engine.kit;

import me.monstermaze.engine.api.KitType;

public final class KitConstants {
    public static final int SLOWBALL_MAX = 16;
    public static final int SLOWBALL_REGEN_TICKS = 40;
    public static final int CRYO_RADIUS = 6;
    public static final int CRYO_FREEZE_TICKS = 60;
    public static final int CRYO_COOLDOWN_TICKS = 600;
    public static final int REPULSOR_CHARGES = 3;
    public static final double REPULSOR_RADIUS = 6.0;
    public static final int BODY_RUSH_DURATION_TICKS = 200;
    public static final int BODY_RUSH_CONTACT_COST_TICKS = 40;
    public static final int BODY_RUSH_ACTIVATIONS = 2;
    public static final double BODY_MAX_HP = 30.0;
    public static final double BODY_FIRST_PAD_MAX_GAIN = 2.0;
    public static final double BODY_FIRST_HEAL = 4.0;
    public static final double BODY_LATER_HEAL = 2.0;

    public static int initialAbilityCharges(KitType kit) {
        switch (kit) {
            case SLOWBALL: return 0;
            case REPULSOR: return REPULSOR_CHARGES;
            case BODY_BUILDER: return BODY_RUSH_ACTIVATIONS;
            default: return 0;
        }
    }

    private KitConstants() {}
}
