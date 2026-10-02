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

        record Key(int r, int c, int dir) {}
        record Cost(int edges, int turns) {}
        record Node(int r, int c, int dir, int edges, int turns) {}

        Comparator<Node> cmp = Comparator.comparingInt(Node::edges)
                .thenComparingInt(Node::turns)
                .thenComparingInt(Node::r)
                .thenComparingInt(Node::c)
                .thenComparingInt(Node::dir);
        PriorityQueue<Node> open = new PriorityQueue<>(cmp);
        Map<Key, Cost> best = new HashMap<>();
        Map<Key, Key> previous = new HashMap<>();

        Key start = new Key(startR, startC, -1);
        best.put(start, new Cost(0, 0));
        previous.put(start, null);
        open.add(new Node(startR, startC, -1, 0, 0));

        Node goal = null;
        while (!open.isEmpty()) {
            Node cur = open.poll();
            Cost known = best.get(new Key(cur.r, cur.c, cur.dir));
            if (known == null || known.edges != cur.edges || known.turns != cur.turns) continue;

            if ((!regionGoal && cur.r == goalR && cur.c == goalC)
                    || (regionGoal
                        && Math.abs(cur.r - goalR) <= radius
                        && Math.abs(cur.c - goalC) <= radius)) {
                if (goal == null || compareGoal(cur, goal, goalR, goalC) < 0) goal = cur;
                if (regionGoal && !open.isEmpty() && open.peek().edges > cur.edges) break;
                if (!regionGoal) break;
            }

            for (int dir = 0; dir < 4; dir++) {
                int nr = cur.r, nc = cur.c;
                switch (dir) {
                    case 0 -> nr--;
                    case 1 -> nr++;
                    case 2 -> nc--;
                    default -> nc++;
                }
                relax(maze, cur, nr, nc, dir, open, best, previous);
            }

            if (allowGaps) {
                int[] rs = {cur.r - 2, cur.r + 2, cur.r, cur.r};
                int[] cs = {cur.c, cur.c, cur.c - 2, cur.c + 2};
                for (int i = 0; i < 4; i++) {
                    if (isGapEdge(maze, cur.r, cur.c, rs[i], cs[i])) {
                        relax(maze, cur, rs[i], cs[i], i, open, best, previous);
                    }
                }
            }
        }

        if (goal == null) return List.of();

        ArrayList<int[]> path = new ArrayList<>();
        Key at = new Key(goal.r, goal.c, goal.dir);
        while (at != null) {
            path.add(new int[]{at.r, at.c});
            at = previous.get(at);
        }
        Collections.reverse(path);
        return path;
    }

    private void relax(MazeGraph maze, Object currentObj, int nr, int nc, int dir,
                       PriorityQueue<?> ignored, Map<?,?> ignoredBest, Map<?,?> ignoredPrevious) {
        // This overload intentionally never executes; generic records are scoped in search().
        throw new AssertionError("unreachable");
    }

    private static void relax(MazeGraph maze, Object currentNode,
                              int nr, int nc, int dir,
                              PriorityQueue open, Map bestRaw, Map previousRaw) {
        @SuppressWarnings("unchecked")
        PriorityQueue<Object> pq = (PriorityQueue<Object>) open;
        @SuppressWarnings("unchecked")
        Map<Object,Object> best = (Map<Object,Object>) bestRaw;
        @SuppressWarnings("unchecked")
        Map<Object,Object> previous = (Map<Object,Object>) previousRaw;

        if (!physical(maze, nr, nc)) return;

        try {
            var nodeClass = currentNode.getClass();
            int cr = (int) nodeClass.getRecordComponents()[0].getAccessor().invoke(currentNode);
            int cc = (int) nodeClass.getRecordComponents()[1].getAccessor().invoke(currentNode);
            int cd = (int) nodeClass.getRecordComponents()[2].getAccessor().invoke(currentNode);
            int ce = (int) nodeClass.getRecordComponents()[3].getAccessor().invoke(currentNode);
            int ct = (int) nodeClass.getRecordComponents()[4].getAccessor().invoke(currentNode);
            Object key = Class.forName("me.monstermaze.engine.util.PlayerPathfinder$1Key");
            throw new UnsupportedOperationException();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static int compareGoal(Object a, Object b, int gr, int gc) {
        return 0;
    }

    private static boolean physical(MazeGraph maze, int r, int c) {
        return r >= 0 && c >= 0 && r < Layouts.SIZE && c < Layouts.SIZE
                && maze.isPhysicalFloor(r, c);
    }

    private static boolean isGapEdge(MazeGraph maze, int r1, int c1, int r2, int c2) {
        int dr = r2-r1, dc = c2-c1;
        if (!((Math.abs(dr)==2 && dc==0) || (Math.abs(dc)==2 && dr==0))) return false;
        int mr=r1+Integer.signum(dr), mc=c1+Integer.signum(dc);
        return physical(maze,r1,c1) && !physical(maze,mr,mc) && physical(maze,r2,c2);
    }
}
