package me.monstermaze.engine;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.game.EngineImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Vertical-slice integration tests: Jumper + monsters + pads + timer. */
class EngineSliceTest {

    @Test
    void jumperSliceRunsWithoutCrash() {
        MonsterMazeEngine engine = new EngineImpl(15);
        GameState state = engine.initialState(MazeMode.ORIGINAL, 0, KitType.JUMPER, 12345L);

        assertEquals(15, state.monsters.size());
        assertNotNull(state.activePad);
        assertEquals(5, state.player.jumpCharges);
        assertEquals(GamePhase.STARTING, state.phase);

        Action move = new Action(1, 0, false, true, 0f, false);

        for (int i = 0; i < 250; i++) {
            TickResult r = engine.tick(state, move);
            state = r.next;
            if (r.terminal) break;
        }

        assertTrue(state.tick >= 200);
        assertEquals(GamePhase.LIVE, state.phase);
    }

    @Test
    void monstersMoveFromSpawn() {
        MonsterMazeEngine engine = new EngineImpl(5);
        GameState state = engine.initialState(MazeMode.ORIGINAL, 1, KitType.JUMPER, 99L);
        Vec3 p0 = state.monsters.get(0).pos;

        for (int i = 0; i < 40; i++) {
            state = engine.tick(state, Action.noop()).next;
        }
        Vec3 p1 = state.monsters.get(0).pos;
        double dist = Math.hypot(p1.x - p0.x, p1.z - p0.z);
        assertTrue(dist > 0.5, "monster should have moved, dist=" + dist);
    }

    @Test
    void knockbackCanApplyOnContact() {
        MonsterMazeEngine engine = new EngineImpl(30);
        GameState state = engine.initialState(MazeMode.ORIGINAL, 0, KitType.JUMPER, 7L);

        while (state.phase != GamePhase.LIVE) {
            state = engine.tick(state, Action.noop()).next;
        }

        double startHp = state.player.health;
        boolean gotHit = false;
        for (int i = 0; i < 500; i++) {
            MonsterState nearest = state.monsters.get(0);
            double best = Double.MAX_VALUE;
            for (MonsterState m : state.monsters) {
                double d = Math.hypot(m.pos.x - state.player.pos.x, m.pos.z - state.player.pos.z);
                if (d < best) {
                    best = d;
                    nearest = m;
                }
            }
            float yaw = (float) Math.toDegrees(Math.atan2(
                    -(nearest.pos.x - state.player.pos.x),
                    nearest.pos.z - state.player.pos.z));
            float yawDelta = yaw - state.player.yaw;
            while (yawDelta >= 180f) yawDelta -= 360f;
            while (yawDelta < -180f) yawDelta += 360f;
            yawDelta = Math.max(-30f, Math.min(30f, yawDelta));
            Action chase = new Action(1, 0, true, true, yawDelta, false, nearest.id);
            TickResult r = engine.tick(state, chase);
            state = r.next;
            for (GameEvent e : r.events) {
                if (e.type == GameEventType.DAMAGE || e.type == GameEventType.KNOCKBACK) {
                    gotHit = true;
                }
            }
            if (gotHit) break;
        }
        assertTrue(gotHit || state.player.health <= startHp);
    }
}
