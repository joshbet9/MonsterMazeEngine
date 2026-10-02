package me.monstermaze.engine.monster;

import me.monstermaze.engine.api.MonsterState;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;
import me.monstermaze.engine.util.SeededRandom;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterMovementCoverageTest {
    @Test
    void everyEligibleSpawnedMonsterGetsMovementOpportunity() {
        MazeGraph graph = new MazeGraph(0);
        List<MonsterState> monsters = new ArrayList<>();
        int id = 1;

        for (int r = 8; r < Layouts.SIZE - 8 && monsters.size() < 40; r++) {
            for (int c = 8; c < Layouts.SIZE - 8 && monsters.size() < 40; c++) {
                if (!graph.isTraversable(r, c)) continue;
                if (graph.traversableCardinals(r, c).isEmpty()) continue;
                monsters.add(new MonsterState(
                        id++,
                        new me.monstermaze.engine.api.Vec3(
                                Coordinates.pathCenterX(0, r), 64,
                                Coordinates.pathCenterZ(0, c)),
                        me.monstermaze.engine.api.Vec3.ZERO,
                        -1, -1, -1, false, 0));
            }
        }

        List<Double> beforeX = monsters.stream().map(m -> m.pos.x).toList();
        List<Double> beforeZ = monsters.stream().map(m -> m.pos.z).toList();

        MonsterSimulator simulator = new MonsterSimulator(
                graph, 0, 64, 0, new SeededRandom(987654321L), 1.4, 987654321L);
        for (int tick = 0; tick < 8; tick++) simulator.tick(monsters, tick);

        int moved = 0;
        for (int i = 0; i < monsters.size(); i++) {
            MonsterState m = monsters.get(i);
            if (m.removed) continue;
            if (Math.hypot(m.pos.x - beforeX.get(i), m.pos.z - beforeZ.get(i)) > 0.01) moved++;
        }

        assertEquals(40, monsters.size());
        assertEquals(40, moved,
                "Every eligible monster must get a valid movement path rather than deadlocking");
    }
}
