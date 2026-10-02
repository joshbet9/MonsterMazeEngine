package me.monstermaze.engine.ml;

import me.monstermaze.engine.api.GameState;
import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.MonsterState;
import me.monstermaze.engine.api.Action;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.util.PlayerPathfinder;

import java.util.List;

/**
 * Exact 52-element policy feature contract used by MonsterMazeAI.
 *
 * Feature indexes 38..43 are the six action dimensions:
 * forward, strafe, jump, sprint, yawDelta, ability.
 */
public final class PolicyLearningFeatures {
    public static final String[] NAMES = {
            "stage_norm", "health_ratio", "horizontal_speed", "forward_speed",
            "lateral_speed", "vertical_speed", "grounded", "phase_ticks_norm",
            "ability_charges_norm", "ability_active_norm", "pad_distance_norm",
            "pad_direction_cos", "pad_direction_sin", "old_pad_count_norm",
            "preview_pad_distance_norm", "local_floor_north", "local_floor_south",
            "local_floor_east", "local_floor_west", "local_floor_northeast",
            "local_floor_northwest", "local_floor_southeast", "local_floor_southwest",
            "mode_speed", "mode_modern", "kit_jumper", "kit_maverick",
            "kit_slowballer", "kit_repulsor", "kit_body_builder",
            "monster_count_12_norm", "monster_count_20_norm",
            "nearest_monster_distance_norm", "nearest_monster_closing_norm",
            "nearest_monster_forward_norm", "nearest_monster_lateral_norm",
            "max_monster_closing_norm", "min_time_to_contact_norm",
            "action_forward", "action_strafe", "action_jump", "action_sprint",
            "action_yaw_delta", "action_ability", "route_length_norm",
            "route_next_forward", "route_next_strafe", "route_next_turn_distance_norm",
            "route_next_turn_sign", "route_gap_soon", "route_gap_distance_norm",
            "route_turn_count_norm"
    };

    private PolicyLearningFeatures() {}

    public static double[] extract(GameState state, Action action) {
        throw new UnsupportedOperationException(
                "Use extract(state, action, engineGraph) so route features use the authoritative dynamic graph");
    }

    public static double[] extract(GameState state, Action action,
                                   me.monstermaze.engine.maze.MazeGraph graph) {
        if (state == null || state.player == null || action == null) {
            throw new IllegalArgumentException("state, player and action are required");
        }

        double[] f = new double[NAMES.length];
        f[0] = clamp01(state.stage / 100.0);
        f[1] = clamp01(state.player.health / Math.max(1.0, state.player.maxHealth));

        double yaw = Math.toRadians(state.player.yaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double strafeX = Math.cos(yaw);
        double strafeZ = Math.sin(yaw);

        double horizontalSpeed = Math.hypot(state.player.vel.x, state.player.vel.z);
        f[2] = clamp(horizontalSpeed / 0.60, 0.0, 3.0);
        f[3] = clamp((state.player.vel.x * forwardX + state.player.vel.z * forwardZ) / 0.60, -3.0, 3.0);
        f[4] = clamp((state.player.vel.x * strafeX + state.player.vel.z * strafeZ) / 0.60, -3.0, 3.0);
        f[5] = clamp(state.player.vel.y / 0.50, -4.0, 4.0);
        f[6] = state.player.onGround ? 1.0 : 0.0;
        f[7] = clamp(state.phaseTimerTicks / 1200.0, 0.0, 2.0);

        double abilityCharges = state.player.kit == KitType.JUMPER
                ? state.player.jumpCharges
                : state.player.abilityCharges;
        f[8] = clamp(abilityCharges / 3.0, 0.0, 1.0);
        f[9] = clamp(Math.max(0L, state.player.abilityActiveUntilTick - state.tick) / 600.0, 0.0, 1.0);

        int activeRow = -1;
        int activeCol = -1;
        if (state.activePad != null) {
            activeRow = Coordinates.layoutRow(state.centerX, state.activePad.centerX);
            activeCol = Coordinates.layoutCol(state.centerZ, state.activePad.centerZ);
            double dx = activeRow + 0.5 - logicalX(state.player.pos.x, state.centerX);
            double dz = activeCol + 0.5 - logicalZ(state.player.pos.z, state.centerZ);
            double distance = Math.hypot(dx, dz);
            f[10] = clamp(distance / 100.0, 0.0, 2.0);
            if (distance > 1.0E-6) {
                f[11] = dx / distance * forwardX + dz / distance * forwardZ;
                f[12] = dx / distance * strafeX + dz / distance * strafeZ;
            }
        }

        f[13] = clamp(state.oldPads.size() / 10.0, 0.0, 1.0);

        if (state.previewPad != null) {
            int previewRow = Coordinates.layoutRow(state.centerX, state.previewPad.centerX);
            int previewCol = Coordinates.layoutCol(state.centerZ, state.previewPad.centerZ);
            double dx = previewRow + 0.5 - logicalX(state.player.pos.x, state.centerX);
            double dz = previewCol + 0.5 - logicalZ(state.player.pos.z, state.centerZ);
            f[14] = clamp(Math.hypot(dx, dz) / 100.0, 0.0, 2.0);
        }

        int playerRow = (int) Math.floor(logicalX(state.player.pos.x, state.centerX));
        int playerCol = (int) Math.floor(logicalZ(state.player.pos.z, state.centerZ));
        int[][] offsets = {
                {-1,0},{1,0},{0,1},{0,-1},
                {-1,1},{-1,-1},{1,1},{1,-1}
        };
        for (int i = 0; i < offsets.length; i++) {
            int r = playerRow + offsets[i][0];
            int c = playerCol + offsets[i][1];
            boolean floor = inBounds(r,c) && state.maze != null && state.maze.physicalFloor[r][c];
            f[15+i] = floor ? 1.0 : 0.0;
        }

        f[23] = state.mode == me.monstermaze.engine.api.MazeMode.SPEED ? 1.0 : 0.0;
        f[24] = state.mode == me.monstermaze.engine.api.MazeMode.MODERN ? 1.0 : 0.0;
        f[25] = kit(state, KitType.JUMPER);
        f[26] = kit(state, KitType.MAVERICK);
        f[27] = kit(state, KitType.SLOWBALL);
        f[28] = kit(state, KitType.REPULSOR);
        f[29] = kit(state, KitType.BODY_BUILDER);

        int within12 = 0;
        int within20 = 0;
        double nearest = Double.POSITIVE_INFINITY;
        double nearestClosing = 0.0;
        double nearestForward = 0.0;
        double nearestLateral = 0.0;
        double maxClosing = 0.0;
        double minTtc = Double.POSITIVE_INFINITY;

        for (MonsterState monster : state.monsters) {
            if (monster.removed || monster.launched(state.tick) || monster.frozen(state.tick)) continue;

            double dx = monster.pos.x - state.player.pos.x;
            double dz = monster.pos.z - state.player.pos.z;
            double distance = Math.hypot(dx, dz);
            if (distance <= 12.0) within12++;
            if (distance <= 20.0) within20++;

            double closing = 0.0;
            if (distance > 1.0E-6) {
                closing = (monster.vel.x * -dx + monster.vel.z * -dz) / distance;
            }
            if (distance < nearest) {
                nearest = distance;
                nearestClosing = closing;
                nearestForward = dx * forwardX + dz * forwardZ;
                nearestLateral = dx * strafeX + dz * strafeZ;
            }
            maxClosing = Math.max(maxClosing, Math.max(0.0, closing));
            if (closing > 1.0E-6) minTtc = Math.min(minTtc, distance / closing);
        }

        f[30] = clamp(within12 / 30.0, 0.0, 1.0);
        f[31] = clamp(within20 / 80.0, 0.0, 1.0);
        f[32] = Double.isFinite(nearest) ? clamp(nearest / 20.0, 0.0, 2.0) : 2.0;
        f[33] = clamp(nearestClosing / 0.60, -3.0, 3.0);
        f[34] = clamp(nearestForward / 20.0, -2.0, 2.0);
        f[35] = clamp(nearestLateral / 20.0, -2.0, 2.0);
        f[36] = clamp(maxClosing / 0.60, 0.0, 3.0);
        f[37] = Double.isFinite(minTtc) ? clamp(minTtc / 20.0, 0.0, 2.0) : 2.0;

        f[38] = action.forward;
        f[39] = action.strafe;
        f[40] = action.jump ? 1.0 : 0.0;
        f[41] = action.sprint ? 1.0 : 0.0;
        f[42] = clamp(action.yawDelta / 30.0, -1.0, 1.0);
        f[43] = action.useAbility ? 1.0 : 0.0;

        double[] route = routeContext(state, graph, playerRow, playerCol,
                activeRow, activeCol, forwardX, forwardZ, strafeX, strafeZ);
        System.arraycopy(route, 0, f, 44, route.length);
        return f;
    }

    private static double[] routeContext(GameState state,
                                         me.monstermaze.engine.maze.MazeGraph graph,
                                         int playerRow, int playerCol,
                                         int activeRow, int activeCol,
                                         double forwardX, double forwardZ,
                                         double strafeX, double strafeZ) {
        double[] out = new double[8];
        if (state.maze == null || activeRow < 0 || activeCol < 0 || !inBounds(playerRow, playerCol)) {
            return out;
        }
        if (!state.maze.physicalFloor[playerRow][playerCol]) return out;

        if (graph == null) return out;
        List<int[]> path = new PlayerPathfinder().shortestPathToRegion(
                graph, playerRow, playerCol, activeRow, activeCol, 2);
        if (path.isEmpty()) {
            path = new PlayerPathfinder().shortestPath(
                    graph, playerRow, playerCol, activeRow, activeCol);
        }
        if (path.isEmpty()) return out;

        int edges = Math.max(0, path.size() - 1);
        out[0] = clamp(edges / 80.0, 0.0, 2.0);

        if (path.size() >= 2) {
            int[] a0 = path.get(0), a1 = path.get(1);
            double dx = Integer.signum(a1[0] - a0[0]);
            double dz = Integer.signum(a1[1] - a0[1]);
            out[1] = dx * forwardX + dz * forwardZ;
            out[2] = dx * strafeX + dz * strafeZ;

            double turnDistance = 0.0;
            double currentDx = dx, currentDz = dz;
            double turnSign = 0.0;
            for (int i = 1; i < path.size() - 1; i++) {
                int[] a = path.get(i), b = path.get(i + 1);
                double ndx = Integer.signum(b[0] - a[0]);
                double ndz = Integer.signum(b[1] - a[1]);
                if (ndx == currentDx && ndz == currentDz) {
                    turnDistance++;
                    continue;
                }
                turnSign = Math.signum(currentDx * ndz - currentDz * ndx);
                break;
            }
            out[3] = clamp(turnDistance / 12.0, 0.0, 2.0);
            out[4] = turnSign;

            int gapIndex = -1;
            for (int i = 0; i < path.size() - 1 && i < 9; i++) {
                int dr = Math.abs(path.get(i+1)[0] - path.get(i)[0]);
                int dc = Math.abs(path.get(i+1)[1] - path.get(i)[1]);
                if ((dr == 2 && dc == 0) || (dc == 2 && dr == 0)) {
                    gapIndex = i;
                    break;
                }
            }
            out[5] = gapIndex >= 0 ? 1.0 : 0.0;
            out[6] = gapIndex >= 0 ? clamp(gapIndex / 8.0, 0.0, 1.0) : 1.0;

            int turns = 0;
            int prevDr = Integer.signum(path.get(1)[0] - path.get(0)[0]);
            int prevDc = Integer.signum(path.get(1)[1] - path.get(0)[1]);
            for (int i = 1; i < path.size() - 1; i++) {
                int dr = Integer.signum(path.get(i+1)[0] - path.get(i)[0]);
                int dc = Integer.signum(path.get(i+1)[1] - path.get(i)[1]);
                if (dr != prevDr || dc != prevDc) turns++;
                prevDr = dr; prevDc = dc;
            }
            out[7] = clamp(turns / 20.0, 0.0, 1.0);
        }
        return out;
    }

    private static double logicalX(double worldX, int centerX) {
        return worldX - centerX + Layouts.HALF;
    }

    private static double logicalZ(double worldZ, int centerZ) {
        return worldZ - centerZ + Layouts.HALF;
    }

    private static boolean inBounds(int r, int c) {
        return r >= 0 && c >= 0 && r < Layouts.SIZE && c < Layouts.SIZE;
    }

    private static double kit(GameState state, KitType kit) {
        return state.player.kit == kit ? 1.0 : 0.0;
    }

    public static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public static double clamp01(double v) {
        return clamp(v, 0.0, 1.0);
    }
}
