package me.monstermaze.engine.monster;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;
import me.monstermaze.engine.util.SeededRandom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterSimulatorSpeedTest {
    @Test
    void controllerSpeedIsNotTreatedAsBlocksPerTick() {
        MazeGraph graph = new MazeGraph(0);
        SeededRandom random = new SeededRandom(1234L);
        MonsterSimulator simulator = new MonsterSimulator(
                graph, 0, 64, 0, random, 1.4, 1234L);

        int row = findPlainPath(graph);
        int col = findPlainPathColumn(graph, row);

        MonsterState monster = new MonsterState(
                1,
                new Vec3(Coordinates.pathCenterX(0, row), 64,
                        Coordinates.pathCenterZ(0, col)),
                Vec3.ZERO, row, col, -1, false, 0);

        simulator.tick(java.util.List.of(monster), 0);

        double blocksPerSecond = Math.hypot(monster.vel.x, monster.vel.z) * 20.0;
        assertEquals(2.8, blocksPerSecond, 1e-9);
        assertTrue(blocksPerSecond < 4.0,
                "the 1.4 controller input must not become a multi-block-per-tick speed");
    }

    private static int findPlainPath(MazeGraph graph) {
        for (int r = 8; r < Layouts.SIZE - 8; r++) {
            if (graph.isTraversable(r, 49)) return r;
        }
        throw new AssertionError("No traversable test row");
    }

    private static int findPlainPathColumn(MazeGraph graph, int row) {
        for (int c = 8; c < Layouts.SIZE - 8; c++) {
            if (graph.isTraversable(row, c)) return c;
        }
        throw new AssertionError("No traversable test column");
    }
}
