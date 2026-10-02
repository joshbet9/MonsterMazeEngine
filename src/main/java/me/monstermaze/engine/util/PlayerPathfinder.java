package me.monstermaze.engine.util;

import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;

import java.util.*;

/**
 * Physical player routing matching MonsterMazeAI's path semantics:
 * cardinal floor edges plus one-block gap-jump edges, with fewest turns as
 * the tie-breaker.
 */
public final class PlayerPathfinder {
    public List<int[]> shortestPathToRegion(MazeGraph maze, int startR, int startC,
                                            int goalR, int goalC, int radius) {
        if (radius < 0) throw new IllegalArgumentException("radius");
        return search(maze, startR, startC, goalR, goalC, radius, true, true);
    }

    public List<int[]> shortestPath(MazeGraph maze, int startR, int startC,
                                    int goalR, int goalC) {
        return search(maze, startR, startC, goalR, goalC, 0, true, false);
    }

    private List<int[]> search(MazeGraph maze, int startR, int startC,
                               int goalR, int goalC, int radius,
                               boolean allowGaps, boolean regionGoal) {
        if (!physical(maze, startR, startC)) return List.of();

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator
                .comparingInt((Node n) -> n.edges)
                .thenComparingInt(n -> n.turns)
                .thenComparingInt(n -> n.r)
                .thenComparingInt(n -> n.c)
                .thenComparingInt(n -> n.direction));

        Map<Key, Cost> best = new HashMap<>();
        Map<Key, Key> previous = new HashMap<>();

        Key startKey = new Key(startR, startC, -1);
        best.put(startKey, new Cost(0, 0));
        previous.put(startKey, null);
        open.add(new Node(startR, startC, -1, 0, 0));

        Node bestGoal = null;
        while (!open.isEmpty()) {
            Node current = open.poll();
            Key currentKey = new Key(current.r, current.c, current.direction);
            Cost known = best.get(currentKey);
            if (known == null || known.edges != current.edges || known.turns != current.turns) continue;

            boolean reached = regionGoal
                    ? Math.abs(current.r - goalR) <= radius
                        && Math.abs(current.c - goalC) <= radius
                    : current.r == goalR && current.c == goalC;

            if (reached) {
                if (bestGoal == null || compareGoal(current, bestGoal, goalR, goalC) < 0) {
                    bestGoal = current;
                }
                if (!regionGoal) break;
                if (!open.isEmpty() && open.peek().edges > current.edges) break;
                continue;
            }

            for (int direction = 0; direction < 4; direction++) {
                int nr = current.r;
                int nc = current.c;
                switch (direction) {
                    case 0 -> nr--;
                    case 1 -> nr++;
                    case 2 -> nc--;
                    default -> nc++;
                }
                relax(maze, current, nr, nc, direction, open, best, previous);
            }

            if (allowGaps) {
                int[] rs = {current.r - 2, current.r + 2, current.r, current.r};
                int[] cs = {current.c, current.c, current.c - 2, current.c + 2};
                for (int i = 0; i < 4; i++) {
                    if (isGapEdge(maze, current.r, current.c, rs[i], cs[i])) {
                        relax(maze, current, rs[i], cs[i], i, open, best, previous);
                    }
                }
            }
        }

        if (bestGoal == null) return List.of();

        ArrayList<int[]> path = new ArrayList<>();
        Key at = new Key(bestGoal.r, bestGoal.c, bestGoal.direction);
        while (at != null) {
            path.add(new int[]{at.r, at.c});
            at = previous.get(at);
        }
        Collections.reverse(path);
        return path;
    }

    private void relax(MazeGraph maze, Node current, int nr, int nc, int direction,
                       PriorityQueue<Node> open, Map<Key, Cost> best,
                       Map<Key, Key> previous) {
        if (!physical(maze, nr, nc)) return;

        int turns = current.turns;
        if (current.direction >= 0 && current.direction != direction) turns++;

        Key key = new Key(nr, nc, direction);
        Cost candidate = new Cost(current.edges + 1, turns);
        Cost prior = best.get(key);
        if (prior != null && compareCost(candidate, prior) >= 0) return;

        best.put(key, candidate);
        previous.put(key, new Key(current.r, current.c, current.direction));
        open.add(new Node(nr, nc, direction, candidate.edges, candidate.turns));
    }

    private static int compareCost(Cost a, Cost b) {
        int edges = Integer.compare(a.edges, b.edges);
        return edges != 0 ? edges : Integer.compare(a.turns, b.turns);
    }

    private static int compareGoal(Node a, Node b, int goalR, int goalC) {
        if (a.edges != b.edges) return Integer.compare(a.edges, b.edges);
        if (a.turns != b.turns) return Integer.compare(a.turns, b.turns);
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

    private static boolean isGapEdge(MazeGraph maze, int r1, int c1, int r2, int c2) {
        int dr = r2 - r1;
        int dc = c2 - c1;
        if (!((Math.abs(dr) == 2 && dc == 0) || (Math.abs(dc) == 2 && dr == 0))) {
            return false;
        }
        int mr = r1 + Integer.signum(dr);
        int mc = c1 + Integer.signum(dc);
        return physical(maze, r1, c1)
                && !physical(maze, mr, mc)
                && physical(maze, r2, c2);
    }

    private record Key(int r, int c, int direction) {}
    private record Cost(int edges, int turns) {}
    private record Node(int r, int c, int direction, int edges, int turns) {}
}
