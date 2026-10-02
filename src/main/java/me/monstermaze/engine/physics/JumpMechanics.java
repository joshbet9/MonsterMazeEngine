package me.monstermaze.engine.physics;

import me.monstermaze.engine.api.KitType;

/**
 * Source-compatible jump state. Jumper receives the ordinary jump calculation;
 * all jumpless kits use the same Jump Boost -10 suppression mechanic as the
 * reference plugin.
 */
public final class JumpMechanics {
    public static final int DISABLED_JUMP_BOOST_AMPLIFIER = -10;

    private JumpMechanics() {}

    public static int jumpBoostAmplifier(KitType kit, boolean jumperChargeAvailable) {
        return kit == KitType.JUMPER && jumperChargeAvailable
                ? 0
                : DISABLED_JUMP_BOOST_AMPLIFIER;
    }

    /** Minecraft 1.8 jump formula: 0.42 + (amplifier + 1) * 0.1. */
    public static double vanillaJumpVelocity(int amplifier) {
        return 0.42D + (amplifier + 1) * 0.1D;
    }
}
