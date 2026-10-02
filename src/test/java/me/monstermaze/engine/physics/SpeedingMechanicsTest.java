package me.monstermaze.engine.physics;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.maze.MazeGraph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeedingMechanicsTest {
    @Test
    void jumpBoostMinusTenStillProvidesSprintImpulse() {
        MazeGraph graph = new MazeGraph(0);
        PlayerState ordinary = new PlayerState(
                new Vec3(0.5, 64.0, 0.5), Vec3.ZERO,
                0.0F, 0.0F, true, 20.0, 20.0,
                KitType.SLOWBALL, 0, 0, 0, 0, 0, 0, false);
        PlayerState speeding = ordinary.copy();
        PlayerPhysics18 physics = new PlayerPhysics18(graph, 0, 64, 0);

        physics.tick(ordinary, new Action(1, 0, false, true, 0, false), -10);
        physics.tick(speeding, new Action(1, 0, true, true, 0, false), -10);

        double ordinaryHorizontal = Math.hypot(ordinary.vel.x, ordinary.vel.z);
        double speedingHorizontal = Math.hypot(speeding.vel.x, speeding.vel.z);
        assertTrue(speedingHorizontal > ordinaryHorizontal + 0.05,
                "Jump Boost -10 must retain the sprint-jump horizontal speeding impulse");
        assertTrue(speeding.pos.y <= ordinary.pos.y + 1.0E-9,
                "Jumpless speeding must not create vertical movement");
    }
}
