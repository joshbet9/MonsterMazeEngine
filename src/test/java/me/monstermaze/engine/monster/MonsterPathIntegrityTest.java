package me.monstermaze.engine.monster;

import me.monstermaze.engine.api.MonsterState;
import me.monstermaze.engine.api.Vec3;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;
import me.monstermaze.engine.util.SeededRandom;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MonsterPathIntegrityTest {

    @Test
    void monstersStayOnLivePhysicalCellsAndMoveCardinally() {
        MazeGraph graph = new MazeGraph(0);
        int[] start = findCellWithAtLeastThreeChoices(graph);

        MonsterState monster = new MonsterState(
                1,
                new Vec3(
                        Coordinates.pathCenterX(0, start[0]), 64,
                        Coordinates.pathCenterZ(0, start[1])),
                Vec3.ZERO,
                -1, -1, -1, false, 0);

        List<MonsterState> monsters = new ArrayList<>(List.of(monster));
        MonsterSimulator simulator = new MonsterSimulator(
                graph, 0, 64, 0, new SeededRandom(123456789L), 1.4, 123456789L);

        Set<Integer> directionsSeen = new HashSet<>();
        Vec3 previous = monster.pos;

        for (int tick = 0; tick < 1600; tick++) {
            simulator.tick(monsters, tick);

            if (monster.removed) fail("Monster was removed during a live-path integrity test");

            double dx = monster.pos.x - previous.x;
            double dz = monster.pos.z - previous.z;
            if (Math.hypot(dx, dz) > 1.0e-9) {
                assertTrue(Math.abs(dx) < 1.0e-9 || Math.abs(dz) < 1.0e-9,
                        "Monster movement must stay cardinal; got dx=" + dx + " dz=" + dz);
                directionsSeen.add(monster.direction);

                int row = cellRow(monster.pos.x);
                int col = cellCol(monster.pos.z);
                assertTrue(graph.isTraversable(row, col),
                        "Monster entered a disabled/non-path waypoint at " + row + "," + col);
                assertTrue(graph.isPhysicalFloor(row, col),
                        "Monster crossed a physical gap at " + row + "," + col);
                assertFalse(graph.hasPadSurface(row, col),
                        "Monster entered Safe Pad surface at " + row + "," + col);
            }

            previous = monster.pos;
        }

        assertTrue(directionsSeen.size() >= 2,
                "Monster should make random cardinal route decisions rather than travel one straight line");
    }

    @Test
    void disabledPadCellCannotBeEnteredByMonsterRoute() {
        MazeGraph graph = new MazeGraph(0);
        int[] pad = findLiveCellWithLiveCardinalNeighbour(graph);
        graph.setPadSurface(pad[0], pad[1], true);
        graph.setTraversable(pad[0], pad[1], false);

        int[] start = graph.traversableCardinals(pad[0], pad[1]).get(0);
        MonsterState monster = new MonsterState(
                1,
                new Vec3(
                        Coordinates.pathCenterX(0, start[0]), 64,
                        Coordinates.pathCenterZ(0, start[1])),
                Vec3.ZERO,
                -1, -1, -1, false, 0);

        List<MonsterState> monsters = new ArrayList<>(List.of(monster));
        MonsterSimulator simulator = new MonsterSimulator(
                graph, 0, 64, 0, new SeededRandom(7L), 1.4, 7L);

        for (int tick = 0; tick < 600; tick++) {
            simulator.tick(monsters, tick);
            if (monster.removed) break;
            int row = cellRow(monster.pos.x);
            int col = cellCol(monster.pos.z);
            assertFalse(graph.hasPadSurface(row, col),
                    "Monster route entered the disabled Safe Pad footprint");
        }
    }

    private static int[] findCellWithAtLeastThreeChoices(MazeGraph graph) {
        for (int r = 1; r < Layouts.SIZE - 1; r++) {
            for (int c = 1; c < Layouts.SIZE - 1; c++) {
                if (graph.isTraversable(r, c) && graph.traversableCardinals(r, c).size() >= 3) {
                    return new int[]{r, c};
                }
            }
        }
        fail("Layout has no suitable three-way path branch");
        return new int[]{0, 0};
    }

    private static int[] findLiveCellWithLiveCardinalNeighbour(MazeGraph graph) {
        for (int r = 1; r < Layouts.SIZE - 1; r++) {
            for (int c = 1; c < Layouts.SIZE - 1; c++) {
                if (!graph.isTraversable(r, c)) continue;
                if (!graph.traversableCardinals(r, c).isEmpty()) return new int[]{r, c};
            }
        }
        fail("Layout has no usable path cell");
        return new int[]{0, 0};
    }

    private static int cellRow(double worldX) {
        return (int) Math.floor(worldX + 49.0);
    }

    private static int cellCol(double worldZ) {
        return (int) Math.floor(worldZ + 49.0);
    }
}
