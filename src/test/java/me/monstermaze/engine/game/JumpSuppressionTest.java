package me.monstermaze.engine.game;

import me.monstermaze.engine.api.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class JumpSuppressionTest {

    @Test
    void jumplessKitsCannotLeaveTheGround() {
        for (KitType kit : new KitType[] {
                KitType.SLOWBALL, KitType.BODY_BUILDER, KitType.REPULSOR, KitType.MAVERICK
        }) {
            EngineImpl engine = new EngineImpl(0);
            GameState state = engine.initialState(MazeMode.SPEED, 0, kit, 12345L);
            double startY = state.player.pos.y;

            TickResult result = engine.tick(state,
                    new Action(0, 0, true, false, 0, false));

            assertEquals(startY, result.next.player.pos.y, 1.0E-9, kit.name());
            assertTrue(result.next.player.onGround, kit.name());
        }
    }

    @Test
    void jumperLeavesTheGroundWithACharge() {
        EngineImpl engine = new EngineImpl(0);
        GameState state = engine.initialState(MazeMode.SPEED, 0, KitType.JUMPER, 12345L);
        double startY = state.player.pos.y;

        TickResult result = engine.tick(state,
                new Action(0, 0, true, false, 0, false));

        assertTrue(result.next.player.pos.y > startY, "Jumper should perform a normal jump");
        assertFalse(result.next.player.onGround);
    }
}
