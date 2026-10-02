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
 * Source-derived MonsterManager movement simulator. It follows the same
 * 1.8 corridor compression, 0.4 waypoint tolerance, 30-degree turning,
 * snowman movement attribute and U-turn avoidance used by MonsterMazeAI.
 */
public final class MonsterSimulator {
    private static final double WAYPOINT_TOLERANCE = 0.4;
    /*
     * CreatureMoveFast's public 1.4 value is a controller input, not blocks/tick.
     * The current 1.21 Monster Maze controller realizes movement with a 0.175
     * conversion after its 0.8 move command, i.e. 0.14 blocks/tick (~2.8 b/s).
     * Keep the common simulator on that realized scale rather than integrating
     * the command as raw velocity. This also prevents artificial acceleration
     * from making mobs many times faster than the real game.
     */
    private static final double REALIZED_MOVE_SCALE = 0.10D;
    private static final double MAX_REALIZED_MOVE_PER_TICK = 0.14D;
    private static final double GRAVITY = 0.08D;
    private static final double AIR_DRAG = 0.9800000190734863D;
    private final MazeGraph maze;
    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final SeededRandom random;
    private final double speed;
    private final long seed;

    public MonsterSimulator(MazeGraph maze, int centerX, int centerY, int centerZ,
                            SeededRandom random, double speed, long seed) {
        if (maze == null || random == null) throw new IllegalArgumentException();
        this.maze = maze;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.random = random;
        this.speed = speed;
        this.seed = seed;
    }

    public void tick(List<MonsterState> monsters, long tick) {
        for (MonsterState m : monsters) {
            if (m.removed || m.frozen(tick)) continue;
            if (m.launched(tick)) {
                tickLaunched(m, tick);
                continue;
            }

            if (m.pos.y < centerY) {
                int row = nearestRow(m.pos.x);
                int col = nearestColumn(m.pos.z);
                if (Coordinates.inBounds(row, col) && maze.isRawPath(row, col)) {
                    m.pos = new Vec3(Coordinates.pathCenterX(centerX, row), centerY,
                            Coordinates.pathCenterZ(centerZ, col));
                    m.vel = Vec3.ZERO;
                    m.targetWaypointX = row;
                    m.targetWaypointZ = col;
                }
            }

            int tr = m.targetWaypointX;
            int tc = m.targetWaypointZ;

            // A Safe Pad disables monster waypoints immediately. Invalidate any
            // stale target selected before the pad appeared so an existing route
            // cannot carry the mob through the pad.
            if (tr >= 0 && tc >= 0 && !maze.isTraversable(tr, tc)) {
                m.targetWaypointX = -1;
                m.targetWaypointZ = -1;
                m.direction = -1;
                tr = tc = -1;
            }

            if (tr < 0 || tc < 0 || atWaypoint(m)) {
                int row = nearestRow(m.pos.x);
                int col = nearestColumn(m.pos.z);
                if (Coordinates.inBounds(row, col) && maze.isTraversable(row, col)) {
                    int[] next = chooseNextWaypoint(m, row, col);
                    if (next == null) continue;
                    tr = next[0];
                    tc = next[1];
                } else {
                    continue;
                }
            }

            double tx = Coordinates.pathCenterX(centerX, tr);
            double tz = Coordinates.pathCenterZ(centerZ, tc);
            double dx = tx - m.pos.x;
            double dz = tz - m.pos.z;
            double dist = Math.hypot(dx, dz);
            if (dist < 1e-9) continue;

            float desiredYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            float currentYaw = m.direction < 0 ? desiredYaw : yawFromDirection(m.direction);
            float yaw = approachAngle(currentYaw, desiredYaw, 30.0F);

            double movementInput = Math.min(MAX_REALIZED_MOVE_PER_TICK, speed * REALIZED_MOVE_SCALE);
            double rad = Math.toRadians(yaw);
            double fx = -Math.sin(rad);
            double fz = Math.cos(rad);

            double vx = fx * movementInput;
            double vz = fz * movementInput;
            double nx = m.pos.x + vx;
            double nz = m.pos.z + vz;

            int nextRow = nearestRow(nx);
            int nextCol = nearestColumn(nz);
            // Do not let the mob's continuous movement cross into a disabled
            // waypoint/pad cell merely because its current target is beyond it.
            if (!Coordinates.inBounds(nextRow, nextCol)
                    || !maze.isTraversable(nextRow, nextCol)) {
                m.targetWaypointX = -1;
                m.targetWaypointZ = -1;
                m.direction = -1;
                m.vel = Vec3.ZERO;
                continue;
            }

            m.vel = new Vec3(vx, 0.0, vz);
            if (!hasPhysicalSupport(nx, nz)) {
                m.pos = new Vec3(nx, centerY - 0.08D, nz);
            } else {
                m.pos = new Vec3(nx, centerY, nz);
            }

            // A pad is a monster exclusion surface. This also protects against
            // a mob reaching the pad through an unusual launch/collision path.
            int landedRow = nearestRow(m.pos.x);
            int landedCol = nearestColumn(m.pos.z);
            if (Coordinates.inBounds(landedRow, landedCol)
                    && maze.hasPadSurface(landedRow, landedCol)) {
                m.removed = true;
                m.vel = Vec3.ZERO;
            }
        }
    }

    private int[] chooseNextWaypoint(MonsterState m, int row, int col) {
        List<int[]> choices = new ArrayList<>(maze.traversableCardinals(row, col));
        int currentDirection = m.direction >= 0
                ? m.direction
                : directionFromDelta(m.targetWaypointX - row, m.targetWaypointZ - col);
        if (choices.size() > 1 && currentDirection >= 0) {
            choices.removeIf(n -> directionFromDelta(n[0] - row, n[1] - col) == opposite(currentDirection));
        }
        if (choices.isEmpty()) {
            choices = new ArrayList<>(maze.traversableCardinals(row, col));
        }
        if (choices.isEmpty()) {
            m.targetWaypointX = -1;
            m.targetWaypointZ = -1;
            return null;
        }

        int[] chosen = choices.get(random.nextInt(choices.size()));
        int dr = chosen[0] - row, dc = chosen[1] - col;
        int dir = directionFromDelta(dr, dc);
        int tr = chosen[0], tc = chosen[1];

        int cr = row, cc = col;
        while (true) {
            int nr = cr + drSign(dir);
            int nc = cc + dcSign(dir);
            if (!maze.isTraversable(nr, nc)) break;
            tr = nr; tc = nc;

            int alternatives = 0;
            for (int[] n : maze.traversableCardinals(nr, nc)) {
                int nd = directionFromDelta(n[0] - nr, n[1] - nc);
                if (nd != dir) alternatives++;
            }
            if (alternatives > 1) break;
            cr = nr; cc = nc;
        }

        m.targetWaypointX = tr;
        m.targetWaypointZ = tc;
        m.direction = dir;
        return new int[]{tr, tc};
    }

    private void tickLaunched(MonsterState m, long tick) {
        m.pos = m.pos.add(m.vel);
        m.vel = new Vec3(m.vel.x * AIR_DRAG, (m.vel.y - GRAVITY) * AIR_DRAG,
                m.vel.z * AIR_DRAG);
        if (m.pos.y <= centerY) {
            m.pos = new Vec3(m.pos.x, centerY, m.pos.z);
            m.vel = Vec3.ZERO;
            if (tick - m.launchedAtTick >= 10) {
                m.removed = true;
            } else {
                m.launched = false;
                m.launchedUntilTick = 0L;
                m.launchedAtTick = 0L;
            }
        } else if (tick - m.launchedAtTick >= 30) {
            m.removed = true;
        }
    }

    private boolean hasPhysicalSupport(double x, double z) {
        final double halfWidth = 0.35D;
        double minX = x - halfWidth, maxX = x + halfWidth;
        double minZ = z - halfWidth, maxZ = z + halfWidth;
        int minRow = nearestRow(minX), maxRow = nearestRow(Math.nextDown(maxX));
        int minCol = nearestColumn(minZ), maxCol = nearestColumn(Math.nextDown(maxZ));
        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                if (Coordinates.inBounds(r, c) && maze.isPhysicalFloor(r, c)) return true;
            }
        }
        return false;
    }

    private int nearestRow(double worldX) {
        return (int) Math.floor(Coordinates.layoutRow(centerX, (int) Math.floor(worldX)));
    }

    private int nearestColumn(double worldZ) {
        return (int) Math.floor(Coordinates.layoutCol(centerZ, (int) Math.floor(worldZ)));
    }

    private boolean atWaypoint(MonsterState m) {
        if (m.targetWaypointX < 0 || m.targetWaypointZ < 0) return false;
        return Math.hypot(
                m.pos.x - Coordinates.pathCenterX(centerX, m.targetWaypointX),
                m.pos.z - Coordinates.pathCenterZ(centerZ, m.targetWaypointZ)) < WAYPOINT_TOLERANCE;
    }

    private static int directionFromDelta(int dr, int dc) {
        if (dr < 0) return 0;
        if (dr > 0) return 2;
        if (dc > 0) return 1;
        if (dc < 0) return 3;
        return -1;
    }

    private static int opposite(int d) { return d < 0 ? -1 : (d + 2) % 4; }

    private static int drSign(int d) {
        return d == 0 ? -1 : d == 2 ? 1 : 0;
    }

    private static int dcSign(int d) {
        return d == 1 ? 1 : d == 3 ? -1 : 0;
    }

    private static float yawFromDirection(int direction) {
        return switch (direction) {
            case 0 -> 0.0F;
            case 1 -> -90.0F;
            case 2 -> 180.0F;
            case 3 -> 90.0F;
            default -> 0.0F;
        };
    }

    private static int directionFromWorldYaw(float yaw) {
        float a = normalise(yaw);
        if (a >= -45 && a < 45) return 0;
        if (a >= -135 && a < -45) return 1;
        if (a >= 45 && a < 135) return 3;
        return 2;
    }

    private static float approachAngle(float current, float target, float maxDelta) {
        float delta = normalise(target - current);
        if (delta > maxDelta) delta = maxDelta;
        if (delta < -maxDelta) delta = -maxDelta;
        return normalise(current + delta);
    }

    private static float normalise(float angle) {
        while (angle >= 180.0F) angle -= 360.0F;
        while (angle < -180.0F) angle += 360.0F;
        return angle;
    }

    public long seed() { return seed; }
    public double speed() { return speed; }
}
