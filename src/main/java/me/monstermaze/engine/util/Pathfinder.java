package me.monstermaze.engine.util;

import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;

import java.util.ArrayDeque;
import java.util.Arrays;

/** Cardinal BFS on traversable graph. Returns next step toward goal, or null. */
public final class Pathfinder {

    private Pathfinder() {}

    public static int[] nextStep(MazeGraph graph, int startR, int startC, int goalR, int goalC) {
        if (startR == goalR && startC == goalC) return null;
        if (!graph.isTraversable(goalR, goalC)) {
            int[] near = nearestTraversable(graph, goalR, goalC);
            if (near == null) return null;
            goalR = near[0];
            goalC = near[1];
        }
        if (!graph.isTraversable(startR, startC)) {
            int[] near = nearestTraversable(graph, startR, startC);
            if (near == null) return null;
            startR = near[0];
            startC = near[1];
        }

        int n = Layouts.SIZE;
        int[][] prevR = new int[n][n];
        int[][] prevC = new int[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(prevR[i], -2);
            Arrays.fill(prevC[i], -2);
        }
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{startR, startC});
        prevR[startR][startC] = -1;
        prevC[startR][startC] = -1;

        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        boolean found = false;
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            if (cur[0] == goalR && cur[1] == goalC) {
                found = true;
                break;
            }
            for (int[] d : dirs) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (!graph.isTraversable(nr, nc)) continue;
                if (prevR[nr][nc] != -2) continue;
                prevR[nr][nc] = cur[0];
                prevC[nr][nc] = cur[1];
                q.add(new int[]{nr, nc});
            }
        }
        if (!found) return null;

        int r = goalR, c = goalC;
        int pr = prevR[r][c], pc = prevC[r][c];
        while (pr != -1) {
            if (pr == startR && pc == startC) return new int[]{r, c};
            r = pr; c = pc;
            pr = prevR[r][c];
            pc = prevC[r][c];
        }
        return null;
    }

    public static int[] nearestTraversable(MazeGraph graph, int r, int c) {
        if (graph.isTraversable(r, c)) return new int[]{r, c};
        for (int rad = 1; rad < 20; rad++) {
            for (int dr = -rad; dr <= rad; dr++) {
                for (int dc = -rad; dc <= rad; dc++) {
                    if (Math.abs(dr) != rad && Math.abs(dc) != rad) continue;
                    if (graph.isTraversable(r + dr, c + dc)) return new int[]{r + dr, c + dc};
                }
            }
        }
        return null;
    }
}
