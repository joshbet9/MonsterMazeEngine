package me.monstermaze.engine.ai;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.MazeGraph;
import me.monstermaze.engine.util.Pathfinder;

/** Small deterministic smoke agent using the same semantic Action contract as MonsterMazeAI. */
public final class HeuristicAgent {
    private MazeGraph graph;

    public void setGraph(MazeGraph graph) {
        this.graph = graph;
    }

    public Action act(GameState state) {
        PlayerState p = state.player;
        SafePadState pad = state.activePad != null ? state.activePad : state.previewPad;

        double targetX = pad != null ? pad.centerX + 0.5 : p.pos.x;
        double targetZ = pad != null ? pad.centerZ + 0.5 : p.pos.z;

        MonsterState nearest = null;
        double nearestD = Double.POSITIVE_INFINITY;
        int closeCount = 0;
        for (MonsterState m : state.monsters) {
            if (m.removed || m.launched(state.tick) || m.frozen(state.tick)) continue;
            double d = Math.hypot(m.pos.x - p.pos.x, m.pos.z - p.pos.z);
            if (d < nearestD) {
                nearestD = d;
                nearest = m;
            }
            if (d < 4.0) closeCount++;
        }

        double desiredX = targetX - p.pos.x;
        double desiredZ = targetZ - p.pos.z;

        if (graph != null && pad != null) {
            int sr = Coordinates.layoutRow(state.centerX, (int) Math.floor(p.pos.x));
            int sc = Coordinates.layoutCol(state.centerZ, (int) Math.floor(p.pos.z));
            int gr = Coordinates.layoutRow(state.centerX, pad.centerX);
            int gc = Coordinates.layoutCol(state.centerZ, pad.centerZ);
            int[] next = Pathfinder.nextStep(graph, sr, sc, gr, gc);
            if (next != null) {
                desiredX = Coordinates.pathCenterX(state.centerX, next[0]) - p.pos.x;
                desiredZ = Coordinates.pathCenterZ(state.centerZ, next[1]) - p.pos.z;
            }
        }

        if (nearest != null && nearestD < 2.2) {
            double fx = p.pos.x - nearest.pos.x;
            double fz = p.pos.z - nearest.pos.z;
            double fl = Math.hypot(fx, fz);
            if (fl > 1e-6) {
                fx /= fl;
                fz /= fl;
                desiredX = desiredX * 0.25 + fx * 4.0;
                desiredZ = desiredZ * 0.25 + fz * 4.0;
            }
        }

        float desiredYaw = (float) Math.toDegrees(Math.atan2(-desiredX, desiredZ));
        float yawDelta = normaliseDelta(desiredYaw - p.yaw);
        yawDelta = Math.max(-30.0f, Math.min(30.0f, yawDelta));

        boolean jump = p.onGround && (nearestD < 1.6
                || (p.kit == KitType.JUMPER && p.jumpCharges > 0 && nearestD < 3.0));

        boolean ability = switch (p.kit) {
            case REPULSOR -> closeCount >= 2 && p.abilityCharges > 0;
            case SLOWBALL -> closeCount >= 3
                    && p.abilityCooldownUntilTick <= state.tick;
            case BODY_BUILDER -> closeCount >= 2
                    && p.abilityActivations > 0
                    && p.abilityActiveUntilTick <= state.tick;
            default -> false;
        };

        return new Action(1.0, 0.0, jump, true, yawDelta, ability,
                nearest == null ? null : nearest.id);
    }

    private static float normaliseDelta(float value) {
        while (value >= 180.0f) value -= 360.0f;
        while (value < -180.0f) value += 360.0f;
        return value;
    }
}
