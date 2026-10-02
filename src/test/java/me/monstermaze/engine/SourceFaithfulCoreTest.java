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
    void startingPhaseMatchesSourceCountdownLength() {
        EngineImpl engine = new EngineImpl(1);
        GameState state = engine.initialState(MazeMode.SPEED, 0, KitType.JUMPER, 9L);
        for (int i = 0; i < 69; i++) {
            state = engine.tick(state, Action.noop()).next;
        }
        assertEquals(GamePhase.STARTING, state.phase);
        state = engine.tick(state, Action.noop()).next;
        assertEquals(GamePhase.LIVE, state.phase);
        assertEquals(70L, state.tick);
    }


    @Test
    void fallingOffTheMazeEndsTheRun() {
        EngineImpl engine = new EngineImpl(0);
        GameState state = engine.initialState(MazeMode.SPEED, 0, KitType.JUMPER, 4L);
        state = state.copy();
        state.phase = GamePhase.LIVE;
        state.player.pos = new Vec3(state.player.pos.x, state.centerY - 3.1, state.player.pos.z);
        TickResult result = engine.tick(state, Action.noop());
        assertFalse(result.next.alive);
        assertEquals(GamePhase.ENDING, result.next.phase);
        assertTrue(result.next.inMonsterMaze == false);
    }

    @Test
    void centerSafeZonePathCellsStartDisabledAsMonsterWaypoints() {
        EngineImpl engine = new EngineImpl(5);
        engine.initialState(MazeMode.SPEED, 0, KitType.JUMPER, 1L);
        int checked = 0;
        for (int r = 0; r < me.monstermaze.engine.maze.Layouts.SIZE; r++) {
            for (int col = 0; col < me.monstermaze.engine.maze.Layouts.SIZE; col++) {
                int raw = engine.getWorkingGraph().raw(r, col);
                if (raw == 5 || raw == 6) {
                    checked++;
                    assertFalse(engine.getWorkingGraph().isTraversable(r, col));
                }
            }
        }
        assertTrue(checked > 0);
    }

    @Test
    void previewPadIsActiveLikeSourceNextSafePad() {
        me.monstermaze.engine.maze.MazeGraph graph = new me.monstermaze.engine.maze.MazeGraph(0);
        SafePadState preview = me.monstermaze.engine.pad.SafePadSimulator.previewPad(
                graph, 0, 64, 0, new me.monstermaze.engine.util.SeededRandom(2L), java.util.List.of());
        assertNotNull(preview);
        assertTrue(preview.active);
        assertTrue(preview.isPreview);
    }

    @Test
    void monstersStayStillDuringStartingCountdown() {
        EngineImpl engine = new EngineImpl(5);
        GameState state = engine.initialState(MazeMode.SPEED, 0, KitType.JUMPER, 3L);
        state = engine.tick(state, Action.noop()).next;
        assertFalse(state.monsters.isEmpty());
        Vec3 before = state.monsters.get(0).pos;
        state = engine.tick(state, Action.noop()).next;
        assertEquals(before.x, state.monsters.get(0).pos.x, 1e-12);
        assertEquals(before.y, state.monsters.get(0).pos.y, 1e-12);
        assertEquals(before.z, state.monsters.get(0).pos.z, 1e-12);
    }

    @Test
    void bodyRushContactDoesNotDealNormalDamage() {
        GameState state = new GameState();
        state.mode = MazeMode.SPEED;
        state.tick = 100L;
        state.player = new PlayerState(
                new Vec3(0.5, 64, 0.5), Vec3.ZERO, 0, 0, true,
                20, 20, KitType.BODY_BUILDER, 0, 0, 0, 0, 0, 0, false);
        state.player.abilityActiveUntilTick = 200L;
        state.maze = new me.monstermaze.engine.maze.MazeGraph(0).toMazeState();
        state.monsters.add(new MonsterState(
                1, new Vec3(0.9, 64, 0.5), Vec3.ZERO, 0, 0, -1, false, 0));
        int result = me.monstermaze.engine.physics.MonsterMazeBumpModel.apply(state);
        assertEquals(me.monstermaze.engine.physics.MonsterMazeBumpModel.RESULT_BODY_RUSH, result);
        assertEquals(20.0, state.player.health, 1e-9);
        assertTrue(state.monsters.get(0).launched);
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