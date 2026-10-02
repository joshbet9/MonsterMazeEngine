package me.monstermaze.engine;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.game.EngineImpl;
import me.monstermaze.engine.game.StageTimer;
import me.monstermaze.engine.ml.PolicyLearningFeatures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceFaithfulCoreTest {
    @Test
    void actionContractMatchesMonsterMazeAI() {
        Action a = new Action(1.0, -0.5, true, true, 30.0f, true);
        assertEquals(1.0, a.forward);
        assertEquals(-0.5, a.strafe);
        assertTrue(a.jump);
        assertTrue(a.sprint);
        assertEquals(30.0f, a.yawDelta, 1e-6);
        assertTrue(a.useAbility);
    }

    @Test
    void noInputKeepsPlayerGrounded() {
        EngineImpl engine = new EngineImpl(5);
        GameState state = engine.initialState(MazeMode.SPEED, 0, KitType.JUMPER, 42L);
        for (int i = 0; i < 40; i++) state = engine.tick(state, Action.noop()).next;
        assertEquals(state.centerY, state.player.pos.y, 1e-9);
        assertTrue(state.player.onGround);
        assertEquals(0.0, state.player.vel.y, 1e-9);
    }

    @Test
    void yawIsACommandDeltaNotAnAbsoluteHeading() {
        EngineImpl engine = new EngineImpl(5);
        GameState state = engine.initialState(MazeMode.SPEED, 0, KitType.JUMPER, 7L);
        state = engine.tick(state, new Action(0, 0, false, false, 30, false)).next;
        assertEquals(30.0f, state.player.yaw, 1e-6);
        state = engine.tick(state, Action.noop()).next;
        assertEquals(30.0f, state.player.yaw, 1e-6);
    }

    @Test
    void starterMonstersUseSourceBatchSize() {
        EngineImpl engine = new EngineImpl();
        GameState state = engine.initialState(MazeMode.SPEED, 0, KitType.JUMPER, 99L);
        for (int i = 0; i < 6; i++) state = engine.tick(state, Action.noop()).next;
        assertEquals(StageTimer.starterMonsters(MazeMode.SPEED), state.monsters.size());
    }

    @Test
    void policyFeatureContractIsExactly52Inputs() {
        EngineImpl engine = new EngineImpl(5);
        GameState state = engine.initialState(MazeMode.MODERN, 1, KitType.REPULSOR, 123L);
        state = engine.tick(state, Action.noop()).next;
        double[] features = PolicyLearningFeatures.extract(state, Action.noop(), engine.getWorkingGraph());
        assertEquals(52, features.length);
        assertEquals("action_forward", PolicyLearningFeatures.NAMES[38]);
        assertEquals("action_strafe", PolicyLearningFeatures.NAMES[39]);
        assertEquals("action_jump", PolicyLearningFeatures.NAMES[40]);
        assertEquals("action_sprint", PolicyLearningFeatures.NAMES[41]);
        assertEquals("action_yaw_delta", PolicyLearningFeatures.NAMES[42]);
        assertEquals("action_ability", PolicyLearningFeatures.NAMES[43]);
        for (double value : features) assertTrue(Double.isFinite(value));
    }
}