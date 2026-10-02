package me.monstermaze.engine.ai;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.MazeGraph;
import me.monstermaze.engine.util.Pathfinder;

/** Heuristic agent: BFS toward pad, flee monsters, use abilities when crowded. */
public final class HeuristicAgent {

    private float yaw = 0f;
    private MazeGraph graph;

    public void setGraph(MazeGraph graph) { this.graph = graph; }

    public Action act(GameState state) {
        PlayerState p = state.player;
        SafePadState pad = state.activePad != null ? state.activePad : state.previewPad;

        double targetX = pad != null ? pad.centerX + 0.5 : 0;
        double targetZ = pad != null ? pad.centerZ + 0.5 : 0;

        MonsterState nearest = null;
        double nearestD = Double.MAX_VALUE;
        int closeCount = 0;
        for (MonsterState m : state.monsters) {
            if (m.launched || m.frozenTicks > 0) continue;
            double d = Math.hypot(m.pos.x - p.pos.x, m.pos.z - p.pos.z);
            if (d < nearestD) { nearestD = d; nearest = m; }
            if (d < 4.0) closeCount++;
        }

        double desiredX = targetX - p.pos.x;
        double desiredZ = targetZ - p.pos.z;

        if (graph != null && pad != null) {
            int sr = Coordinates.layoutRow(0, (int) Math.floor(p.pos.x));
            int sc = Coordinates.layoutCol(0, (int) Math.floor(p.pos.z));
            int gr = Coordinates.layoutRow(0, pad.centerX);
            int gc = Coordinates.layoutCol(0, pad.centerZ);
            int[] next = Pathfinder.nextStep(graph, sr, sc, gr, gc);
            if (next != null) {
                desiredX = Coordinates.pathCenterX(0, next[0]) - p.pos.x;
                desiredZ = Coordinates.pathCenterZ(0, next[1]) - p.pos.z;
            }
        }

        if (nearest != null && nearestD < 2.2) {
            double fx = p.pos.x - nearest.pos.x;
            double fz = p.pos.z - nearest.pos.z;
            double fl = Math.hypot(fx, fz);
            if (fl > 1e-6) {
                fx /= fl; fz /= fl;
                desiredX = desiredX * 0.25 + fx * 4.0;
                desiredZ = desiredZ * 0.25 + fz * 4.0;
            }
        }

        double len = Math.hypot(desiredX, desiredZ);
        if (len > 1e-6) {
            yaw = (float) Math.toDegrees(Math.atan2(-desiredX, desiredZ));
        }

        boolean jump = p.onGround && (nearestD < 1.6 || (p.kit == KitType.JUMPER && p.jumpCharges > 0 && nearestD < 3));
        boolean primary = false;
        boolean enhanced = false;

        if (p.kit == KitType.REPULSOR && closeCount >= 2 && p.abilityCharges > 0) primary = true;
        if (p.kit == KitType.SLOWBALL && closeCount >= 3 && p.enhancedCooldownTicks <= 0) enhanced = true;
        if (p.kit == KitType.BODY_BUILDER && closeCount >= 2 && p.abilityCharges > 0
                && p.abilityCooldownTicks <= 0) enhanced = true;

        Integer targetMonster = null;
        if (p.kit == KitType.MAVERICK && nearest != null && nearestD < 2.0 && pad != null) {
            targetMonster = nearest.id;
        }

        return new Action(0, 1.0, true, jump, jump, yaw, 0f, primary, enhanced, targetMonster);
    }
}
