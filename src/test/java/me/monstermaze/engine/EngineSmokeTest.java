package me.monstermaze.engine;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.game.EngineImpl;
import me.monstermaze.engine.maze.Layouts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineSmokeTest {

    @Test
    void initialStateLoadsRealLayout() {
        MonsterMazeEngine engine = new EngineImpl();
        GameState state = engine.initialState(MazeMode.ORIGINAL, 0, KitType.JUMPER, 42L);

        assertEquals(0L, state.tick);
        assertEquals(MazeMode.ORIGINAL, state.mode);
        assertEquals(KitType.JUMPER, state.player.kit);
        assertEquals(5, state.player.jumpCharges);
        assertEquals(0, state.maze.layoutId);
        assertEquals(Layouts.SIZE, state.maze.raw.length);
        assertEquals(Layouts.SIZE, state.maze.raw[0].length);

        int pathCells = 0;
        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                if (Layouts.isRawPath(state.maze.raw[r][c])) pathCells++;
            }
        }
        assertTrue(pathCells > 100, "expected many path cells, got " + pathCells);

        TickResult result = engine.tick(state, Action.noop());
        assertEquals(1L, result.next.tick);
        assertFalse(result.terminal);
    }

    @Test
    void allThreeLayoutsLoad() {
        MonsterMazeEngine engine = new EngineImpl();
        for (int id = 0; id < Layouts.LAYOUT_COUNT; id++) {
            GameState s = engine.initialState(MazeMode.SPEED, id, KitType.REPULSOR, id * 7L);
            assertEquals(id, s.maze.layoutId);
            assertEquals(0, s.player.jumpCharges);
        }
    }
}
