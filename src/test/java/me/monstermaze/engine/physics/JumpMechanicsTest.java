package me.monstermaze.engine.physics;

import me.monstermaze.engine.api.KitType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class JumpMechanicsTest {

    @Test
    void jumplessKitsUseJumpBoostMinusTen() {
        for (KitType kit : new KitType[] {
                KitType.SLOWBALL, KitType.BODY_BUILDER, KitType.REPULSOR, KitType.MAVERICK
        }) {
            assertEquals(-10, JumpMechanics.jumpBoostAmplifier(kit, false), kit.name());
        }
    }

    @Test
    void jumperGetsNormalJumpOnlyWithCharge() {
        assertEquals(JumpMechanics.NO_JUMP_BOOST,
                JumpMechanics.jumpBoostAmplifier(KitType.JUMPER, true));
        assertEquals(-10, JumpMechanics.jumpBoostAmplifier(KitType.JUMPER, false));
        assertEquals(0.42D, JumpMechanics.vanillaJumpVelocity(JumpMechanics.NO_JUMP_BOOST), 1.0E-12);
        assertEquals(-0.48D, JumpMechanics.vanillaJumpVelocity(-10), 1.0E-12);
    }
}
