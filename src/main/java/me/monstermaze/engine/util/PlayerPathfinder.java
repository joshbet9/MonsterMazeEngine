package me.monstermaze.engine.util;

import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Player route semantics matching MonsterMazeAI's PlayerPathfinder exactly:
 * ordinary cardinal floor edges first, then one-cell gap edges, using BFS.
 */
public final class PlayerPathfinder {

    public List<int[]> shortestPathToRegion(MazeGraph maze, int startR, int startC,
                                            int goalR, int goalC, int radius) {
        if (radius < 0) throw new IllegalArgumentException("radius must be non-negative");
        return shortestPathToRegion(maze, startR, startC, goalR, goalC, radius, true);
    }

    public List<int[]> shortestPath(MazeGraph maze, int startR, int startC,
                                    int goalR, int goalC) {
        return shortestPath(maze, startR, startC, goalR, goalC, true);
    }

    private List<int[]> shortestPath(MazeGraph maze, int startR, int startC,
                                     int goalR, int goalC, boolean allowGaps) {
        if (!physical(maze, startR, startC) || !physical(maze, goalR, goalC)) return List.of();

        ArrayDeque<Cell> queue = new ArrayDeque<>();
        Map<Cell, Cell> previous = new HashMap<>();
        queue.add(new Cell(startR, startC));
        previous.put(new Cell(startR, startC), null);

        while (!queue.isEmpty()) {
            Cell current = queue.removeFirst();
            if (current.r == goalR && current.c == goalC) {
                return reconstruct(previous, current);
            }

            int r = current.r, c = current.c;
            add(maze, current, new Cell(r - 1, c), queue, previous);
            add(maze, current, new Cell(r + 1, c), queue, previous);
            add(maze, current, new Cell(r, c - 1), queue, previous);
            add(maze, current, new Cell(r, c + 1), queue, previous);

            if (allowGaps) {
                addMovement(maze, current, new Cell(r - 2, c), queue, previous);
                addMovement(maze, current, new Cell(r + 2, c), queue, previous);
                addMovement(maze, current, new Cell(r, c - 2), queue, previous);
                addMovement(maze, current, new Cell(r, c + 2), queue, previous);
            }
        }
        return List.of();
    }

    private List<int[]> shortestPathToRegion(MazeGraph maze, int startR, int startC,
                                             int goalR, int goalC, int radius,
                                             boolean allowGaps) {
        if (!physical(maze, startR, startC)) return List.of();

        ArrayDeque<Cell> queue = new ArrayDeque<>();
        Map<Cell, Cell> previous = new HashMap<>();
        Map<Cell, Integer> distance = new HashMap<>();
        Cell start = new Cell(startR, startC);
        queue.add(start);
        previous.put(start, null);
        distance.put(start, 0);

        int bestDistance = Integer.MAX_VALUE;
        Cell bestGoal = null;

        while (!queue.isEmpty()) {
            Cell current = queue.removeFirst();
            int currentDistance = distance.get(current);
            if (currentDistance > bestDistance) break;

            if (Math.abs(current.r - goalR) <= radius
                    && Math.abs(current.c - goalC) <= radius) {
                if (bestGoal == null || compareRegionGoal(current, bestGoal, goalR, goalC) < 0) {
                    bestGoal = current;
                    bestDistance = currentDistance;
                }
                continue;
            }

            int r = current.r, c = current.c;
            add(maze, current, new Cell(r - 1, c), queue, previous, distance, currentDistance + 1);
            add(maze, current, new Cell(r + 1, c), queue, previous, distance, currentDistance + 1);
            add(maze, current, new Cell(r, c - 1), queue, previous, distance, currentDistance + 1);
            add(maze, current, new Cell(r, c + 1), queue, previous, distance, currentDistance + 1);

            if (allowGaps) {
                addMovement(maze, current, new Cell(r - 2, c), queue, previous, distance, currentDistance + 1);
                addMovement(maze, current, new Cell(r + 2, c), queue, previous, distance, currentDistance + 1);
                addMovement(maze, current, new Cell(r, c - 2), queue, previous, distance, currentDistance + 1);
                addMovement(maze, current, new Cell(r, c + 2), queue, previous, distance, currentDistance + 1);
            }
        }

        return bestGoal == null ? List.of() : reconstruct(previous, bestGoal);
    }

    private static int compareRegionGoal(Cell a, Cell b, int goalR, int goalC) {
        int da = Math.abs(a.r - goalR) + Math.abs(a.c - goalC);
        int db = Math.abs(b.r - goalR) + Math.abs(b.c - goalC);
        if (da != db) return Integer.compare(da, db);
        if (a.r != b.r) return Integer.compare(a.r, b.r);
        return Integer.compare(a.c, b.c);
    }

    private static boolean physical(MazeGraph maze, int r, int c) {
        return r >= 0 && c >= 0 && r < Layouts.SIZE && c < Layouts.SIZE
                && maze.isPhysicalFloor(r, c);
    }

    private static void add(MazeGraph maze, Cell current, Cell next,
                            ArrayDeque<Cell> queue, Map<Cell, Cell> previous) {
        if (!physical(maze, next.r, next.c) || previous.containsKey(next)) return;
        previous.put(next, current);
        queue.addLast(next);
    }

    private static void addMovement(MazeGraph maze, Cell current, Cell next,
                                    ArrayDeque<Cell> queue, Map<Cell, Cell> previous) {
        if (!isGapEdge(maze, current, next) || previous.containsKey(next)) return;
        previous.put(next, current);
        queue.addLast(next);
    }

    private static void add(MazeGraph maze, Cell current, Cell next,
                            ArrayDeque<Cell> queue, Map<Cell, Cell> previous,
                            Map<Cell, Integer> distance, int nextDistance) {
        if (!physical(maze, next.r, next.c) || previous.containsKey(next)) return;
        previous.put(next, current);
        distance.put(next, nextDistance);
        queue.addLast(next);
    }

    private static void addMovement(MazeGraph maze, Cell current, Cell next,
                                    ArrayDeque<Cell> queue, Map<Cell, Cell> previous,
                                    Map<Cell, Integer> distance, int nextDistance) {
        if (!isGapEdge(maze, current, next) || previous.containsKey(next)) return;
        previous.put(next, current);
        distance.put(next, nextDistance);
        queue.addLast(next);
    }

    private static boolean isGapEdge(MazeGraph maze, Cell from, Cell to) {
        int dr = to.r - from.r, dc = to.c - from.c;
        if (!((Math.abs(dr) == 2 && dc == 0) || (Math.abs(dc) == 2 && dr == 0))) return false;
        int middleR = from.r + Integer.signum(dr);
        int middleC = from.c + Integer.signum(dc);
        return physical(maze, from.r, from.c)
                && !physical(maze, middleR, middleC)
                && physical(maze, to.r, to.c);
    }

    private static List<int[]> reconstruct(Map<Cell, Cell> previous, Cell goal) {
        ArrayList<int[]> path = new ArrayList<>();
        for (Cell at = goal; at != null; at = previous.get(at)) {
            path.add(new int[]{at.r, at.c});
        }
        java.util.Collections.reverse(path);
        return path;
    }

    private static final class Cell {
        final int r, c;
        Cell(int r, int c) { this.r = r; this.c = c; }

        @Override public boolean equals(Object o) {
            return o instanceof Cell other && r == other.r && c == other.c;
        }

        @Override public int hashCode() {
            return 31 * r + c;
        }
    }
}
