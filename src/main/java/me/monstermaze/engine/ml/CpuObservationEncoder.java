package me.monstermaze.engine.ml;

import me.monstermaze.engine.api.GameState;
import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.MonsterState;
import me.monstermaze.engine.game.EngineImpl;
import me.monstermaze.engine.pad.SafePadSimulator;

import java.util.Arrays;

/**
 * Canonical v2 CPU observation encoder for Engine.
 *
 * <p>The ordering mirrors MonsterMaze 1.8's server-side CpuObservationBuilder.
 * The caller owns and reuses the output array.
 */
public final class CpuObservationEncoder {
    public static final int FEATURE_COUNT = 96;

    private static final double POS_RANGE = 64.0;
    private static final double MOB_RANGE = 32.0;
    private static final double COMPETITOR_RANGE = 64.0;
    private static final int MONSTER_SLOTS = 8;

    private final double[] mobD2 = new double[MONSTER_SLOTS];
    private final double[] mobDx = new double[MONSTER_SLOTS];
    private final double[] mobDz = new double[MONSTER_SLOTS];
    private final double[] mobDy = new double[MONSTER_SLOTS];

    public void encode(GameState state, float[] out) {
        if (state == null || state.player == null || out == null || out.length != FEATURE_COUNT) {
            throw new IllegalArgumentException("state/player/output(96)");
        }
        Arrays.fill(out, 0.0f);

        double px = state.player.pos.x;
        double py = state.player.pos.y;
        double pz = state.player.pos.z;
        double vx = state.player.vel.x;
        double vy = state.player.vel.y;
        double vz = state.player.vel.z;
        double yawRad = Math.toRadians(state.player.yaw);

        out[0] = norm(px - state.centerX, POS_RANGE);
        out[1] = norm(pz - state.centerZ, POS_RANGE);
        out[2] = norm(py - state.centerY, 8.0);
        out[3] = clamp(vx, -1.0, 1.0);
        out[4] = clamp(vz, -1.0, 1.0);
        out[5] = clamp(vy, -1.0, 1.0);
        out[6] = (float) clamp(Math.hypot(vx, vz), 0.0, 1.0);
        out[7] = (float) Math.sin(yawRad);
        out[8] = (float) Math.cos(yawRad);

        double targetX = state.activePad == null ? Double.NaN : state.activePad.centerX;
        double targetZ = state.activePad == null ? Double.NaN : state.activePad.centerZ;
        double padDx = 0.0;
        double padDz = 0.0;
        if (Double.isFinite(targetX) && Double.isFinite(targetZ)) {
            padDx = targetX - px;
            padDz = targetZ - pz;
        }
        double padDistance = Math.hypot(padDx, padDz);
        double padBearing = Math.atan2(-padDx, padDz);
        double yawError = normaliseDegrees(Math.toDegrees(padBearing) - state.player.yaw);

        out[9] = norm(padDx, POS_RANGE);
        out[10] = norm(padDz, POS_RANGE);
        out[11] = norm(padDistance, POS_RANGE);
        out[12] = (float) Math.sin(padBearing);
        out[13] = (float) Math.cos(padBearing);
        out[14] = (float) (yawError / 180.0);

        out[15] = clamp(state.stage / 100.0, 0.0, 1.0);
        double phaseSeconds = state.phaseTimerTicks / 20.0;
        out[16] = clamp(phaseSeconds / 60.0, 0.0, 1.0);
        double phaseStart = Math.max(1.0, state.phaseTimerMax / 20.0);
        double phaseElapsed = (phaseStart - phaseSeconds) / phaseStart;
        out[17] = clamp(phaseSeconds / phaseStart, 0.0, 1.0);

        double maxHealth = Math.max(1.0, state.player.maxHealth);
        out[18] = clamp(state.player.health / maxHealth, 0.0, 1.0);
        out[19] = clamp(maxHealth / 30.0, 0.0, 1.0);
        out[20] = state.player.onGround ? 1.0f : 0.0f;
        out[21] = SafePadSimulator.isOn(state.activePad, state.player.pos) ? 1.0f : 0.0f;
        out[22] = onAnyPad(state) ? 1.0f : 0.0f;

        out[23] = state.player.kit.ordinal() / 4.0f;
        out[24] = clamp(state.player.jumpCharges / 5.0, 0.0, 1.0);
        out[25] = abilityReady(state);
        out[26] = state.mode == me.monstermaze.engine.api.MazeMode.SPEED ? 1.0f : 0.0f;
        out[27] = state.mode == me.monstermaze.engine.api.MazeMode.MODERN ? 1.0f : 0.0f;
        out[28] = state.mazePattern == 0 ? 1.0f : 0.0f;
        out[29] = state.mazePattern == 1 ? 1.0f : 0.0f;
        out[30] = state.mazePattern == 2 ? 1.0f : 0.0f;
        out[31] = clamp((state.stage - 1.0 + phaseElapsed) / 100.0, 0.0, 1.0);

        int row = (int) Math.floor(px - (state.centerX - 49));
        int col = (int) Math.floor(pz - (state.centerZ - 49));
        out[32] = rawPath(state, row, col - 1);
        out[33] = rawPath(state, row, col + 1);
        out[34] = rawPath(state, row + 1, col);
        out[35] = rawPath(state, row - 1, col);
        out[36] = rawPath(state, row + 1, col - 1);
        out[37] = rawPath(state, row - 1, col - 1);
        out[38] = rawPath(state, row + 1, col + 1);
        out[39] = rawPath(state, row - 1, col + 1);
        out[40] = rawPath(state, row, col);

        // Engine currently models one active player. These population features
        // deliberately retain the live 1/8 and 1/8 semantics until multi-player
        // engine state is introduced.
        out[41] = 1.0f / 8.0f;
        out[42] = 1.0f / 8.0f;

        Arrays.fill(mobD2, Double.POSITIVE_INFINITY);
        int monstersWithin8 = 0;
        for (MonsterState monster : state.monsters) {
            if (monster.removed || monster.launched(state.tick) || monster.frozen(state.tick)) continue;
            double dx = monster.pos.x - px;
            double dz = monster.pos.z - pz;
            double dy = monster.pos.y - py;
            if (dx * dx + dz * dz <= 64.0) monstersWithin8++;
            insertMonster(dx, dz, dy);
        }
        out[43] = clamp(monstersWithin8 / 8.0, 0.0, 1.0);

        for (int i = 0; i < MONSTER_SLOTS; i++) {
            if (!Double.isFinite(mobD2[i])) continue;
            int base = 44 + i * 4;
            out[base] = norm(mobDx[i], MOB_RANGE);
            out[base + 1] = norm(mobDz[i], MOB_RANGE);
            out[base + 2] = norm(mobDy[i], 4.0);
            out[base + 3] = norm(Math.sqrt(mobD2[i]), MOB_RANGE);
        }

        // Competitor slots remain zero until Engine gains multi-player state.
        // Zero is the same "no competitor observed" representation used by
        // the live encoder.
    }

    private float abilityReady(GameState state) {
        KitType kit = state.player.kit;
        switch (kit) {
            case JUMPER:
                return state.player.jumpCharges > 0 ? 1.0f : 0.0f;
            case SLOWBALL:
                return state.mode != me.monstermaze.engine.api.MazeMode.ORIGINAL
                        && state.tick >= state.player.abilityCooldownUntilTick ? 1.0f : 0.0f;
            case BODY_BUILDER:
                return state.mode != me.monstermaze.engine.api.MazeMode.ORIGINAL
                        && state.player.abilityActivations > 0
                        && state.player.abilityActiveUntilTick <= state.tick ? 1.0f : 0.0f;
            case REPULSOR:
                return state.player.abilityCharges > 0 ? 1.0f : 0.0f;
            case MAVERICK:
            default:
                return 0.0f;
        }
    }

    private boolean onAnyPad(GameState state) {
        if (SafePadSimulator.isOn(state.activePad, state.player.pos)
                || SafePadSimulator.isOn(state.previewPad, state.player.pos)) return true;
        for (me.monstermaze.engine.api.SafePadState pad : state.oldPads) {
            if (SafePadSimulator.isOn(pad, state.player.pos)) return true;
        }
        return false;
    }

    private float rawPath(GameState state, int row, int col) {
        if (row < 0 || col < 0 || state.maze == null
                || row >= state.maze.raw.length || col >= state.maze.raw[row].length) {
            return 0.0f;
        }
        return Layouts.isRawPath(state.maze.raw[row][col]) ? 1.0f : 0.0f;
    }

    private void insertMonster(double dx, double dz, double dy) {
        double d2 = dx * dx + dz * dz + dy * dy;
        for (int i = 0; i < MONSTER_SLOTS; i++) {
            if (d2 >= mobD2[i]) continue;
            for (int j = MONSTER_SLOTS - 1; j > i; j--) {
                mobD2[j] = mobD2[j - 1];
                mobDx[j] = mobDx[j - 1];
                mobDz[j] = mobDz[j - 1];
                mobDy[j] = mobDy[j - 1];
            }
            mobD2[i] = d2;
            mobDx[i] = dx;
            mobDz[i] = dz;
            mobDy[i] = dy;
            return;
        }
    }

    private static float norm(double value, double range) {
        return clamp(value / range, -1.0, 1.0);
    }

    private static float clamp(double value, double min, double max) {
        return (float) Math.max(min, Math.min(max, value));
    }

    private static double normaliseDegrees(double degrees) {
        while (degrees >= 180.0) degrees -= 360.0;
        while (degrees < -180.0) degrees += 360.0;
        return degrees;
    }
}
