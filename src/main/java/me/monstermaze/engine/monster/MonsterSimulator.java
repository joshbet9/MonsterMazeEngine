package me.monstermaze.engine.monster;

import me.monstermaze.engine.api.MonsterState;
import me.monstermaze.engine.api.Vec3;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;
import me.monstermaze.engine.util.SeededRandom;

import java.util.ArrayList;
import java.util.List;

/**
 * Random-walk monsters matching 1.8 MonsterManager:
 * at waypoint within 0.4, pick random cardinal path neighbour,
 * avoid U-turn when alternatives exist; move at 1.4 * speedMult toward target.
 */
public final class MonsterSimulator {

    public static final double REACH_THRESHOLD = 0.4;
    public static final float BASE_SPEED = 1.4f;
    public static final int DECISION_INTERVAL = 2;

    private MonsterSimulator() {}

    public static List<MonsterState> spawnInitial(
            MazeGraph graph,
            int count,
            int centerX,
            int centerY,
            int centerZ,
            SeededRandom rng) {
        List<int[]> pathCells = new ArrayList<>();
        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                if (graph.isTraversable(r, c)) {
                    pathCells.add(new int[]{r, c});
                }
            }
        }
        List<MonsterState> out = new ArrayList<>();
        if (pathCells.isEmpty()) return out;
        for (int i = 0; i < count; i++) {
            int[] cell = pathCells.get(rng.nextInt(pathCells.size()));
            double x = Coordinates.pathCenterX(centerX, cell[0]);
            double z = Coordinates.pathCenterZ(centerZ, cell[1]);
            out.add(new MonsterState(
                    i,
                    new Vec3(x, centerY, z),
                    Vec3.ZERO,
                    cell[0], cell[1],
                    -1,
                    false,
                    0
            ));
        }
        return out;
    }

    public static List<MonsterState> stepAll(
            List<MonsterState> monsters,
            MazeGraph graph,
            int centerX,
            int centerY,
            int centerZ,
            float speedMultiplier,
            long tick,
            SeededRandom rng) {

        List<MonsterState> next = new ArrayList<>(monsters.size());
        float speed = BASE_SPEED * speedMultiplier;

        for (MonsterState m : monsters) {
            if (m.frozenTicks > 0) {
                next.add(new MonsterState(
                        m.id, m.pos, Vec3.ZERO,
                        m.targetWaypointX, m.targetWaypointZ, m.direction,
                        m.launched, m.frozenTicks - 1));
                continue;
            }
            if (m.launched) {
                Vec3 vel = m.vel.scale(0.91).add(new Vec3(0, -0.08, 0));
                Vec3 pos = m.pos.add(vel);
                if (pos.y <= centerY) {
                    pos = new Vec3(pos.x, centerY, pos.z);
                    next.add(new MonsterState(
                            m.id, pos, Vec3.ZERO,
                            m.targetWaypointX, m.targetWaypointZ, m.direction,
                            false, 0));
                } else {
                    next.add(new MonsterState(
                            m.id, pos, vel,
                            m.targetWaypointX, m.targetWaypointZ, m.direction,
                            true, 0));
                }
                continue;
            }

            int tr = m.targetWaypointX;
            int tc = m.targetWaypointZ;
            double tx = Coordinates.pathCenterX(centerX, tr);
            double tz = Coordinates.pathCenterZ(centerZ, tc);

            double dx = tx - m.pos.x;
            double dz = tz - m.pos.z;
            double dist = Math.sqrt(dx * dx + dz * dz);

            int dir = m.direction;
            int ntr = tr;
            int ntc = tc;

            if (dist < REACH_THRESHOLD && tick % DECISION_INTERVAL == 0) {
                int[][] neigh = graph.traversableCardinals(tr, tc);
                if (neigh.length == 0) {
                    next.add(m);
                    continue;
                }
                List<int[]> options = new ArrayList<>();
                for (int[] n : neigh) {
                    int nd = directionOf(tr, tc, n[0], n[1]);
                    if (neigh.length > 1 && isOpposite(dir, nd)) continue;
                    options.add(n);
                }
                if (options.isEmpty()) {
                    for (int[] n : neigh) options.add(n);
                }
                int[] chosen = options.get(rng.nextInt(options.size()));
                ntr = chosen[0];
                ntc = chosen[1];
                dir = directionOf(tr, tc, ntr, ntc);
                tx = Coordinates.pathCenterX(centerX, ntr);
                tz = Coordinates.pathCenterZ(centerZ, ntc);
                dx = tx - m.pos.x;
                dz = tz - m.pos.z;
                dist = Math.sqrt(dx * dx + dz * dz);
            }

            double step = Math.min(speed, dist);
            double nx = m.pos.x;
            double nz = m.pos.z;
            if (dist > 1e-9) {
                nx += (dx / dist) * step;
                nz += (dz / dist) * step;
            }
            next.add(new MonsterState(
                    m.id,
                    new Vec3(nx, centerY, nz),
                    new Vec3((dx / Math.max(dist, 1e-9)) * step, 0,
                            (dz / Math.max(dist, 1e-9)) * step),
                    ntr, ntc, dir,
                    false, 0
            ));
        }
        return next;
    }

    private static int directionOf(int fr, int fc, int tr, int tc) {
        if (tr < fr) return 0;
        if (tr > fr) return 2;
        if (tc > fc) return 1;
        if (tc < fc) return 3;
        return -1;
    }

    private static boolean isOpposite(int a, int b) {
        if (a < 0 || b < 0) return false;
        return (a + 2) % 4 == b;
    }
}
