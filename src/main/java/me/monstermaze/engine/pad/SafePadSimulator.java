package me.monstermaze.engine.pad;

import me.monstermaze.engine.api.SafePadState;
import me.monstermaze.engine.api.Vec3;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;
import me.monstermaze.engine.util.SeededRandom;

import java.util.ArrayList;
import java.util.List;

/**
 * Safe Pad placement matching 1.8: prefer far path cells, avoid prior pads by 40 blocks,
 * disable +/-2 waypoint cells under the pad.
 */
public final class SafePadSimulator {

    public static final int DECAY_FULL = 11;
    public static final double HALF_EXTENT = 2.5;
    public static final int HEIGHT_BAND = 5;
    public static final double MIN_AVOID_DIST = 40.0;
    public static final int PAD_DISABLE_RADIUS = 2;

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

    public static SafePadState spawnPad(
            MazeGraph graph, int centerX, int surfaceY, int centerZ,
            SeededRandom rng, boolean preview) {
        return spawnPad(graph, centerX, surfaceY, centerZ, rng, preview, null);
    }

    public static SafePadState spawnPad(
            MazeGraph graph, int centerX, int surfaceY, int centerZ,
            SeededRandom rng, boolean preview, List<int[]> avoidWorldCenters) {

        List<int[]> pool = new ArrayList<>();
        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                if (!graph.isTraversable(r, c)) continue;
                int dist = Math.abs(r - Layouts.HALF) + Math.abs(c - Layouts.HALF);
                if (dist < 8) continue;
                pool.add(new int[]{r, c});
            }
        }
        if (pool.isEmpty()) {
            for (int r = 0; r < Layouts.SIZE; r++) {
                for (int c = 0; c < Layouts.SIZE; c++) {
                    if (graph.isTraversable(r, c)) pool.add(new int[]{r, c});
                }
            }
        }
        if (pool.isEmpty()) return null;

        List<int[]> best = new ArrayList<>();
        if (avoidWorldCenters != null && !avoidWorldCenters.isEmpty()) {
            for (int[] cell : pool) {
                int wx = Coordinates.worldX(centerX, cell[0]);
                int wz = Coordinates.worldZ(centerZ, cell[1]);
                boolean ok = true;
                for (int[] a : avoidWorldCenters) {
                    double dx = wx - a[0], dz = wz - a[1];
                    if (dx * dx + dz * dz < MIN_AVOID_DIST * MIN_AVOID_DIST) { ok = false; break; }
                }
                if (ok) best.add(cell);
            }
        }
        if (best.isEmpty()) best = pool;

        int[] chosen = best.get(0);
        double bestScore = -1;
        int samples = Math.min(32, best.size());
        for (int i = 0; i < samples; i++) {
            int[] cell = best.get(rng.nextInt(best.size()));
            double d = Math.hypot(cell[0] - Layouts.HALF, cell[1] - Layouts.HALF);
            if (d > bestScore) { bestScore = d; chosen = cell; }
        }

        int wx = Coordinates.worldX(centerX, chosen[0]);
        int wz = Coordinates.worldZ(centerZ, chosen[1]);
        SafePadState pad = new SafePadState(wx, wz, surfaceY, DECAY_FULL, !preview, preview);
        disablePadArea(graph, centerX, centerZ, pad);
        return pad;
    }

    public static void disablePadArea(MazeGraph graph, int centerX, int centerZ, SafePadState pad) {
        if (pad == null) return;
        int pr = Coordinates.layoutRow(centerX, pad.centerX);
        int pc = Coordinates.layoutCol(centerZ, pad.centerZ);
        for (int dr = -PAD_DISABLE_RADIUS; dr <= PAD_DISABLE_RADIUS; dr++) {
            for (int dc = -PAD_DISABLE_RADIUS; dc <= PAD_DISABLE_RADIUS; dc++) {
                graph.disable(pr + dr, pc + dc);
            }
        }
    }

    public static void enablePadArea(MazeGraph graph, int centerX, int centerZ, SafePadState pad) {
        if (pad == null) return;
        int pr = Coordinates.layoutRow(centerX, pad.centerX);
        int pc = Coordinates.layoutCol(centerZ, pad.centerZ);
        for (int dr = -PAD_DISABLE_RADIUS; dr <= PAD_DISABLE_RADIUS; dr++) {
            for (int dc = -PAD_DISABLE_RADIUS; dc <= PAD_DISABLE_RADIUS; dc++) {
                graph.enableIfRawPath(pr + dr, pc + dc);
            }
        }
    }
}
