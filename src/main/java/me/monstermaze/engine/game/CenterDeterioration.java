package me.monstermaze.engine.game;

import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;

/**
 * Center deterioration: up to 11 steps starting 20s after LIVE.
 * Each step expands a Manhattan radius around the layout centre
 * and disables those cells for traversability.
 */
public final class CenterDeterioration {

    private CenterDeterioration() {}

    public static void apply(MazeGraph graph, int step) {
        if (step <= 0 || graph == null) return;
        int radius = Math.min(step * 2, Layouts.HALF);
        for (int r = Layouts.HALF - radius; r <= Layouts.HALF + radius; r++) {
            for (int c = Layouts.HALF - radius; c <= Layouts.HALF + radius; c++) {
                if (!Coordinates.inBounds(r, c)) continue;
                int man = Math.abs(r - Layouts.HALF) + Math.abs(c - Layouts.HALF);
                if (man <= radius) {
                    graph.setTraversable(r, c, false);
                }
            }
        }
    }
}
