package me.monstermaze.engine;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.game.EngineImpl;
import me.monstermaze.engine.kit.KitConstants;
import me.monstermaze.engine.kit.KitSimulator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KitTest {

    @Test
    void repulsorConsumesCharge() {
        MonsterMazeEngine engine = new EngineImpl(20);
        GameState state = engine.initialState(MazeMode.ORIGINAL, 0, KitType.REPULSOR, 1L);
        assertEquals(3, state.player.abilityCharges);
        while (state.phase != GamePhase.LIVE) {
            state = engine.tick(state, Action.noop()).next;
        }
        Action repulse = new Action(0, 0, false, false, 0, true);
        TickResult r = engine.tick(state, repulse);
        assertEquals(2, r.next.player.abilityCharges);
        assertTrue(r.events.stream().anyMatch(e -> e.type == GameEventType.ABILITY_USED));
    }

    @Test
    void slowballRegens() {
        MonsterMazeEngine engine = new EngineImpl(5);
        GameState state = engine.initialState(MazeMode.SPEED, 0, KitType.SLOWBALL, 2L);
        assertEquals(0, state.player.abilityCharges);
        for (int i = 0; i < KitConstants.SLOWBALL_REGEN_TICKS + 5; i++) {
            state = engine.tick(state, Action.noop()).next;
        }
        assertTrue(state.player.abilityCharges >= 1);
    }

    @Test
    void bodyBuilderGainsMaxHpOnFirstPad() {
        PlayerState p = new PlayerState(
                Vec3.ZERO, Vec3.ZERO, 0, 0, true,
                20, 20, KitType.BODY_BUILDER, 0, 0, 2, 0, 0, 0, true);
        PlayerState after = KitSimulator.applyBodyBuilderFirstPad(p, true);
        assertEquals(22.0, after.maxHealth, 0.01);
        assertEquals(24.0, after.health, 0.01);
    }
}
