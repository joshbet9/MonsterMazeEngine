package me.monstermaze.engine;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.ml.CpuObservationEncoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpuObservationEncoderTest {

    @Test
    void emitsStableNinetySixFeatureVector() {
        GameState state = new GameState(100, 64, 200);
        state.mode = MazeMode.MODERN;
        state.stage = 7;
        state.phaseTimerMax = 400;
        state.phaseTimerTicks = 300;
        state.tick = 123L;
        state.activePad = new SafePadState(101, 202, 63, 11, true, false);

        int[][] raw = new int[99][99];
        boolean[][] traversable = new boolean[99][99];
        boolean[][] floor = new boolean[99][99];
        boolean[][] pad = new boolean[99][99];
        raw[50][50] = 1;
        raw[50][51] = 1;
        floor[50][50] = true;
        floor[50][51] = true;
        state.maze = new me.monstermaze.engine.api.MazeState(2, raw, traversable, floor, pad);

        state.player = new PlayerState(
                new Vec3(101.5, 64.0, 200.5),
                new Vec3(0.1, 0.0, 0.2),
                0.0f, 0.0f, true,
                20.0, 20.0, KitType.MAVERICK,
                0, 0, 0, 0, 0, 0, false);

        state.monsters.add(new MonsterState(
                1, new Vec3(103.0, 64.0, 200.5),
                new Vec3(-0.1, 0.0, 0.0),
                50, 50, -1, false, 0));

        CpuObservationEncoder encoder = new CpuObservationEncoder();
        float[] a = new float[CpuObservationEncoder.FEATURE_COUNT];
        float[] b = new float[CpuObservationEncoder.FEATURE_COUNT];

        encoder.encode(state, a);
        encoder.encode(state, b);

        assertArrayEquals(a, b);
        assertEquals(96, a.length);
        assertEquals(1.0f, a[27]); // MODERN
        assertEquals(1.0f, a[30]); // layout/pattern 2
        assertEquals(1.0f, a[23], 1e-6f); // Maverick ordinal 4 / 4
        assertTrue(a[43] > 0.0f);
        assertTrue(a[44] > 0.0f);
    }

    @Test
    void noCompetitorsUseTheNeutralZeroSlots() {
        GameState state = new GameState(0, 64, 0);
        state.mode = MazeMode.SPEED;
        state.phaseTimerMax = 200;
        state.phaseTimerTicks = 100;
        state.maze = new me.monstermaze.engine.api.MazeState(
                0,
                new int[99][99],
                new boolean[99][99]);

        state.player = new PlayerState(
                new Vec3(49.5, 64.0, 49.5),
                Vec3.ZERO, 0.0f, 0.0f, true,
                20.0, 20.0, KitType.JUMPER,
                3, 0, 3, 0, 0, 0, false);

        float[] values = new float[CpuObservationEncoder.FEATURE_COUNT];
        new CpuObservationEncoder().encode(state, values);

        for (int i = 76; i < 96; i++) {
            assertEquals(0.0f, values[i]);
        }
    }
}
