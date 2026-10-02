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
            assertEquals(-0.48D, JumpMechanics.vanillaJumpVelocity(-10), 1.0E-12);
        }
    }

    @Test
    void jumperGetsNormalJumpOnlyWithCharge() {
        assertEquals(0, JumpMechanics.jumpBoostAmplifier(KitType.JUMPER, true));
        assertEquals(-10, JumpMechanics.jumpBoostAmplifier(KitType.JUMPER, false));
        assertEquals(0.52D, JumpMechanics.vanillaJumpVelocity(0), 1.0E-12);
    }
}
