package me.monstermaze.engine;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.game.EngineImpl;
import me.monstermaze.engine.maze.Layouts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineSmokeTest {

    @Test
    void initialStateLoadsRealLayout() {
        MonsterMazeEngine engine = new EngineImpl(10);
        GameState state = engine.initialState(MazeMode.ORIGINAL, 0, KitType.JUMPER, 42L);

        assertEquals(0L, state.tick);
        assertEquals(MazeMode.ORIGINAL, state.mode);
        assertEquals(KitType.JUMPER, state.player.kit);
        assertEquals(5, state.player.jumpCharges);
        assertEquals(0, state.maze.layoutId);
        assertEquals(Layouts.SIZE, state.maze.raw.length);
        assertEquals(10, state.monsters.size());
        assertNotNull(state.activePad);

        TickResult result = engine.tick(state, Action.noop());
        assertEquals(1L, result.next.tick);
        assertFalse(result.terminal);
    }

    @Test
    void allThreeLayoutsLoad() {
        MonsterMazeEngine engine = new EngineImpl(5);
        for (int id = 0; id < Layouts.LAYOUT_COUNT; id++) {
            GameState s = engine.initialState(MazeMode.SPEED, id, KitType.REPULSOR, id * 7L);
            assertEquals(id, s.maze.layoutId);
            assertEquals(0, s.player.jumpCharges);
        }
    }
}
