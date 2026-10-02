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
 * Safe Pad geometry and source-style candidate selection.
 */
public final class SafePadSimulator {
    public static final int DECAY_FULL = 11;
    public static final double HALF_EXTENT = 2.5;
    public static final int HEIGHT_BAND = 5;
    public static final double MIN_AVOID_DIST = 40.0;

    private SafePadSimulator() {}

    public static boolean isOn(SafePadState pad, Vec3 pos) {
        if (pad == null || !pad.active) return false;
        double dx = pos.x - (pad.centerX + 0.5);
        double dz = pos.z - (pad.centerZ + 0.5);
        return dx > -HALF_EXTENT && dx < HALF_EXTENT
                && dz > -HALF_EXTENT && dz < HALF_EXTENT
                && pos.y > pad.surfaceY
                && pos.y < pad.surfaceY + HEIGHT_BAND;
    }

    public static SafePadState initialPad(
            MazeGraph graph, int centerX, int centerY, int centerZ, SeededRandom rng) {
        CellPools pools = new CellPools(graph);
        int[] cell = furthest(new int[]{Layouts.HALF, Layouts.HALF}, pools.valid, rng);
        SafePadState pad = toPad(cell, centerX, centerY, centerZ, false);
        installSurface(graph, centerX, centerZ, pad);
        return pad;
    }

    public static SafePadState nextPad(
            MazeGraph graph, int centerX, int centerY, int centerZ,
            SeededRandom rng, List<SafePadState> avoid) {
        CellPools pools = new CellPools(graph);
        ArrayList<int[]> candidates = new ArrayList<>();
        for (int[] cell : pools.valid) {
            boolean ok = true;
            for (SafePadState existing : avoid == null ? List.<SafePadState>of() : avoid) {
                if (existing == null) continue;
                int er = Coordinates.layoutRow(centerX, existing.centerX);
                int ec = Coordinates.layoutCol(centerZ, existing.centerZ);
                if (distanceSq(cell, new int[]{er, ec}) < MIN_AVOID_DIST * MIN_AVOID_DIST) {
                    ok = false;
                    break;
                }
            }
            if (ok) candidates.add(cell);
        }
        if (candidates.isEmpty()) candidates.addAll(pools.valid);
        if (candidates.isEmpty()) return null;

        int[] chosen = candidates.get(rng.nextInt(candidates.size()));
        SafePadState pad = toPad(chosen, centerX, centerY, centerZ, false);
        installSurface(graph, centerX, centerZ, pad);
        return pad;
    }

    /** Preview uses exactly the same placement rules but is flagged as preview. */
    public static SafePadState previewPad(
            MazeGraph graph, int centerX, int centerY, int centerZ,
            SeededRandom rng, List<SafePadState> avoid) {
        SafePadState pad = nextPad(graph, centerX, centerY, centerZ, rng, avoid);
        if (pad == null) return null;
        // Source GameManager keeps nextSafePad active immediately after spawning it.
        SafePadState preview = new SafePadState(
                pad.centerX, pad.centerZ, pad.surfaceY, pad.decayStep, true, true);
        installSurface(graph, centerX, centerZ, preview);
        return preview;
    }

    public static void installSurface(
            MazeGraph graph, int centerX, int centerZ, SafePadState pad) {
        if (pad == null) return;
        int row = Coordinates.layoutRow(centerX, pad.centerX);
        int col = Coordinates.layoutCol(centerZ, pad.centerZ);
        for (int r = row - 2; r <= row + 2; r++) {
            for (int c = col - 2; c <= col + 2; c++) {
                graph.setPadSurface(r, c, true);
                // Safe Pad cells remain a physical player surface but are not
                // monster waypoints in the source implementation.
                graph.disable(r, c);
            }
        }
    }

    public static void enableWaypointArea(
            MazeGraph graph, int centerX, int centerZ, SafePadState pad) {
        if (pad == null) return;
        int row = Coordinates.layoutRow(centerX, pad.centerX);
        int col = Coordinates.layoutCol(centerZ, pad.centerZ);
        for (int r = row - 2; r <= row + 2; r++) {
            for (int c = col - 2; c <= col + 2; c++) {
                graph.enableIfRawPath(r, c);
            }
        }
    }

    public static void decayOldPad(
            MazeGraph graph, int centerX, int centerZ, SafePadState pad) {
        if (pad == null) return;
        int row = Coordinates.layoutRow(centerX, pad.centerX);
        int col = Coordinates.layoutCol(centerZ, pad.centerZ);
        for (int r = row - 2; r <= row + 2; r++) {
            for (int c = col - 2; c <= col + 2; c++) {
                graph.setPadSurface(r, c, false);
                graph.enableIfRawPath(r, c);
                if (!Coordinates.inBounds(r, c)) continue;
                // Raw void cells remain void; raw path cells regain their source floor.
                if (!Layouts.isRawPath(graph.raw(r, c))) graph.setPhysicalFloor(r, c, false);
            }
        }
    }

    private static SafePadState toPad(
            int[] cell, int centerX, int centerY, int centerZ, boolean preview) {
        return new SafePadState(
                Coordinates.worldX(centerX, cell[0]),
                Coordinates.worldZ(centerZ, cell[1]),
                centerY - 1,
                DECAY_FULL,
                !preview,
                preview);
    }

    private static int[] furthest(int[] from, List<int[]> cells, SeededRandom rng) {
        if (cells.isEmpty()) return null;
        double best = -1;
        ArrayList<int[]> ties = new ArrayList<>();
        for (int[] c : cells) {
            double d = distanceSq(from, c);
            if (d > best) {
                best = d;
                ties.clear();
                ties.add(c);
            } else if (Double.compare(d, best) == 0) {
                ties.add(c);
            }
        }
        return ties.get(rng.nextInt(ties.size()));
    }

    private static double distanceSq(int[] a, int[] b) {
        double dr = a[0] - b[0], dc = a[1] - b[1];
        return dr * dr + dc * dc;
    }

    private static final class CellPools {
        final List<int[]> valid;

        CellPools(MazeGraph graph) {
            ArrayList<int[]> paths = new ArrayList<>();
            ArrayList<int[]> spawns = new ArrayList<>();
            ArrayList<int[]> glass = new ArrayList<>();
            for (int r = 0; r < Layouts.SIZE; r++) {
                for (int c = 0; c < Layouts.SIZE; c++) {
                    int v = graph.raw(r, c);
                    if (Layouts.isRawPath(v)) paths.add(new int[]{r, c});
                    if (v == 2) spawns.add(new int[]{r, c});
                    if (v == 4 || v == 6) glass.add(new int[]{r, c});
                }
            }

            ArrayList<int[]> filtered = new ArrayList<>();
            for (int[] p : paths) {
                boolean ok = true;
                for (int[] s : spawns) {
                    if (distanceSq(p, s) < 100.0) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) continue;
                for (int[] g : glass) {
                    if (distanceSq(p, g) < 49.0) {
                        ok = false;
                        break;
                    }
                }
                if (ok) filtered.add(p);
            }

            ArrayList<int[]> candidates = new ArrayList<>(filtered);
            ArrayList<int[]> safeZones = new ArrayList<>();
            int[] center = {Layouts.HALF, Layouts.HALF};
            for (int i = 0; i < 8 && !candidates.isEmpty(); i++) {
                ArrayList<int[]> away = new ArrayList<>(safeZones);
                away.add(center);
                int[] selected = furthestFromSet(candidates, away);
                safeZones.add(selected);
                candidates.removeIf(c -> distanceSq(selected, c) <= 36.0);
            }

            ArrayList<int[]> result = new ArrayList<>();
            for (int[] p : filtered) {
                boolean ok = true;
                for (int[] zone : safeZones) {
                    if (distanceSq(p, zone) < 49.0) {
                        ok = false;
                        break;
                    }
                }
                if (ok) result.add(p);
            }
            valid = List.copyOf(result.isEmpty() ? filtered : result);
        }

        private static int[] furthestFromSet(List<int[]> locations, List<int[]> away) {
            int[] best = null;
            double bestDistance = -1.0;
            for (int[] location : locations) {
                double closest = Double.POSITIVE_INFINITY;
                for (int[] a : away) closest = Math.min(closest, distanceSq(location, a));
                if (closest > bestDistance) {
                    bestDistance = closest;
                    best = location;
                }
            }
            return best;
        }
    }
}
