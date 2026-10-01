package me.monstermaze.engine;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.game.EngineImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineSmokeTest {

    @Test
    void initialStateAndNoopTick() {
        MonsterMazeEngine engine = new EngineImpl();
        GameState state = engine.initialState(MazeMode.ORIGINAL, 0, KitType.JUMPER, 42L);

        assertEquals(0L, state.tick);
        assertEquals(MazeMode.ORIGINAL, state.mode);
        assertEquals(KitType.JUMPER, state.player.kit);
        assertEquals(5, state.player.jumpCharges);

        TickResult result = engine.tick(state, Action.noop());
        assertNotNull(result.next);
        assertFalse(result.terminal);
    }
}
