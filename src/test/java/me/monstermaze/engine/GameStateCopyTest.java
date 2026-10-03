package me.monstermaze.engine;

import me.monstermaze.engine.api.Action;
import me.monstermaze.engine.api.GameState;
import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.MazeMode;
import me.monstermaze.engine.api.MazeState;
import me.monstermaze.engine.api.TickResult;
import me.monstermaze.engine.game.EngineImpl;
import me.monstermaze.engine.runtime.SimulationSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class GameStateCopyTest {

    @Test
    void copySharesMazeSnapshot() {
        GameState state = new GameState();
        state.maze = new MazeState(0,
                new int[][]{{1}},
                new boolean[][]{{true}},
                new boolean[][]{{true}},
                new boolean[][]{{false}});

        GameState copy = state.copy();

        assertSame(state.maze, copy.maze);
    }

    @Test
    void engineReusesMazeSnapshotWhenDynamicOverlayIsUnchanged() {
        EngineImpl engine = new EngineImpl();
        GameState state = engine.initialState(MazeMode.ORIGINAL, 0, KitType.JUMPER, 1234L);

        TickResult result = engine.tick(state, Action.noop());

        assertSame(state.maze, result.next.maze);
    }

    @Test
    void sessionsKeepIndependentEngineState() {
        SimulationSession a = SimulationSession.create(MazeMode.ORIGINAL, 0, KitType.JUMPER, 1L);
        SimulationSession b = SimulationSession.create(MazeMode.ORIGINAL, 0, KitType.JUMPER, 1L);

        assertNotSame(a.engine(), b.engine());
        assertNotSame(a.state(), b.state());

        a.step(Action.noop());
        assertEquals(0L, b.state().tick);
    }
}
