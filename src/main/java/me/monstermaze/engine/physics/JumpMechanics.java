package me.monstermaze.engine.physics;

import me.monstermaze.engine.api.KitType;

/**
 * Source-compatible jump state. Jumper receives the ordinary jump calculation;
 * all jumpless kits use the same Jump Boost -10 suppression mechanic as the
 * reference plugin.
 */
public final class JumpMechanics {
    public static final int DISABLED_JUMP_BOOST_AMPLIFIER = -10;
    public static final int NO_JUMP_BOOST = Integer.MIN_VALUE;

    private JumpMechanics() {}

    public static int jumpBoostAmplifier(KitType kit, boolean jumperChargeAvailable) {
        return kit == KitType.JUMPER && jumperChargeAvailable
                ? NO_JUMP_BOOST
                : DISABLED_JUMP_BOOST_AMPLIFIER;
    }

    public static boolean isJumpEnabled(int amplifier) {
        return amplifier == NO_JUMP_BOOST || amplifier > -2;
    }

    /** Minecraft 1.8 jump formula; with no Jump Boost effect the base is 0.42. */
    public static double vanillaJumpVelocity(int amplifier) {
        if (amplifier == NO_JUMP_BOOST) return 0.42D;
        return 0.42D + (amplifier + 1) * 0.1D;
    }
}
