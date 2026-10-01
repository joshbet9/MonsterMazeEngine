package me.monstermaze.engine.pad;

import me.monstermaze.engine.api.SafePadState;
import me.monstermaze.engine.api.Vec3;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;
import me.monstermaze.engine.util.SeededRandom;

/**
 * Safe Pad: 5x5 surface, isOn uses +/-2.5 horizontal and height band.
 * Decay: 11 steps (visual only in pure engine until destroy).
 */
public final class SafePadSimulator {

    public static final int DECAY_FULL = 11;
    public static final double HALF_EXTENT = 2.5;
    public static final int HEIGHT_BAND = 5;

    private SafePadSimulator() {}

    public static boolean isOn(SafePadState pad, Vec3 playerPos) {
        if (pad == null || !pad.active) return false;
        double dx = playerPos.x - (pad.centerX + 0.5);
        double dz = playerPos.z - (pad.centerZ + 0.5);
        return dx > -HALF_EXTENT && dx < HALF_EXTENT
                && dz > -HALF_EXTENT && dz < HALF_EXTENT
                && playerPos.y > pad.surfaceY
                && playerPos.y < pad.surfaceY + HEIGHT_BAND;
    }

    /**
     * Pick a path cell far from centre for the next Safe Pad (simple heuristic).
     */
    public static SafePadState spawnPad(
            MazeGraph graph,
            int centerX,
            int surfaceY,
            int centerZ,
            SeededRandom rng,
            boolean preview) {
        java.util.List<int[]> candidates = new java.util.ArrayList<>();
        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                if (!graph.isTraversable(r, c)) continue;
                int dist = Math.abs(r - Layouts.HALF) + Math.abs(c - Layouts.HALF);
                if (dist > 15) candidates.add(new int[]{r, c});
            }
        }
        if (candidates.isEmpty()) {
            for (int r = 0; r < Layouts.SIZE; r++) {
                for (int c = 0; c < Layouts.SIZE; c++) {
                    if (graph.isTraversable(r, c)) candidates.add(new int[]{r, c});
                }
            }
        }
        if (candidates.isEmpty()) return null;
        int[] cell = candidates.get(rng.nextInt(candidates.size()));
        int wx = Coordinates.worldX(centerX, cell[0]);
        int wz = Coordinates.worldZ(centerZ, cell[1]);
        return new SafePadState(wx, wz, surfaceY, DECAY_FULL, !preview, preview);
    }
}
