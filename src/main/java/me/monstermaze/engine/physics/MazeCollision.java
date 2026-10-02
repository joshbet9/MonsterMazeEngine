package me.monstermaze.engine.physics;

import me.monstermaze.engine.api.PlayerState;
import me.monstermaze.engine.api.Vec3;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.maze.MazeGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft 1.8-style AABB collision against the actual maze floor.
 */
public final class MazeCollision {
    public static final double PLAYER_WIDTH = 0.6;
    public static final double PLAYER_HEIGHT = 1.8;
    public static final double STEP_HEIGHT = 0.6;

    private final MazeGraph maze;
    private final int centerX, centerY, centerZ;

    public MazeCollision(MazeGraph maze, int centerX, int centerY, int centerZ) {
        this.maze = maze;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
    }

    public double floorY() {
        return centerY;
    }

    public boolean hasPhysicalFloor(double x, double z) {
        final double halfWidth = PLAYER_WIDTH / 2.0;
        final double minX = x - halfWidth;
        final double maxX = x + halfWidth;
        final double minZ = z - halfWidth;
        final double maxZ = z + halfWidth;
        int minRow = Coordinates.layoutRow(centerX, (int) Math.floor(minX));
        int maxRow = Coordinates.layoutRow(centerX, (int) Math.floor(Math.nextDown(maxX)));
        int minCol = Coordinates.layoutCol(centerZ, (int) Math.floor(minZ));
        int maxCol = Coordinates.layoutCol(centerZ, (int) Math.floor(Math.nextDown(maxZ)));
        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                if (maze.isPhysicalFloor(r, c)) return true;
            }
        }
        return false;
    }

    public void move(PlayerState p, double dx, double dy, double dz) {
        Aabb original = playerBox(p);
        List<Aabb> boxes = colliders(original.expand(Math.abs(dx), Math.abs(dy), Math.abs(dz)));

        double clippedY = dy;
        for (Aabb b : boxes) clippedY = b.clipY(original, clippedY);
        Aabb afterY = original.offset(0, clippedY, 0);

        double clippedX = dx;
        for (Aabb b : boxes) clippedX = b.clipX(afterY, clippedX);
        Aabb afterX = afterY.offset(clippedX, 0, 0);

        double clippedZ = dz;
        for (Aabb b : boxes) clippedZ = b.clipZ(afterX, clippedZ);

        boolean horizontalBlocked = clippedX != dx || clippedZ != dz;
        double directDistanceSq = clippedX * clippedX + clippedZ * clippedZ;

        if (horizontalBlocked && p.onGround) {
            List<Aabb> stepBoxes = colliders(original.expand(
                    Math.abs(dx), STEP_HEIGHT + Math.abs(dy), Math.abs(dz)));

            double sy = STEP_HEIGHT;
            for (Aabb b : stepBoxes) sy = b.clipY(original, sy);
            Aabb stepY = original.offset(0, sy, 0);

            double sx = dx;
            for (Aabb b : stepBoxes) sx = b.clipX(stepY, sx);
            Aabb stepX = stepY.offset(sx, 0, 0);

            double sz = dz;
            for (Aabb b : stepBoxes) sz = b.clipZ(stepX, sz);
            double stepDistanceSq = sx * sx + sz * sz;

            if (stepDistanceSq > directDistanceSq) {
                Aabb stepFinal = stepX.offset(0, 0, sz);
                p.pos = new Vec3(
                        (stepFinal.minX() + stepFinal.maxX()) / 2.0,
                        stepFinal.minY(),
                        (stepFinal.minZ() + stepFinal.maxZ()) / 2.0);
                p.onGround = true;
                double vx = sx == dx ? p.vel.x : 0.0;
                double vy = (dy < 0 || sy != dy) ? 0.0 : p.vel.y;
                double vz = sz == dz ? p.vel.z : 0.0;
                p.vel = new Vec3(vx, vy, vz);
                return;
            }
        }

        p.pos = new Vec3(
                p.pos.x + clippedX, p.pos.y + clippedY, p.pos.z + clippedZ);
        p.onGround = dy < 0.0 && clippedY != dy;

        double vx = clippedX != dx ? 0.0 : p.vel.x;
        double vy = clippedY != dy ? 0.0 : p.vel.y;
        double vz = clippedZ != dz ? 0.0 : p.vel.z;
        p.vel = new Vec3(vx, vy, vz);
    }

    private Aabb playerBox(PlayerState p) {
        double half = PLAYER_WIDTH / 2.0;
        return new Aabb(
                p.pos.x - half, p.pos.y, p.pos.z - half,
                p.pos.x + half, p.pos.y + PLAYER_HEIGHT, p.pos.z + half);
    }

    private List<Aabb> colliders(Aabb swept) {
        List<Aabb> out = new ArrayList<>();
        int minX = (int) Math.floor(swept.minX()) - 1;
        int maxX = (int) Math.floor(swept.maxX()) + 1;
        int minZ = (int) Math.floor(swept.minZ()) - 1;
        int maxZ = (int) Math.floor(swept.maxZ()) + 1;
        for (int worldX = minX; worldX <= maxX; worldX++) {
            for (int worldZ = minZ; worldZ <= maxZ; worldZ++) {
                int row = Coordinates.layoutRow(centerX, worldX);
                int col = Coordinates.layoutCol(centerZ, worldZ);
                if (!Coordinates.inBounds(row, col) || !maze.isPhysicalFloor(row, col)) continue;
                out.add(new Aabb(worldX, centerY - 1, worldZ,
                        worldX + 1, centerY, worldZ + 1));
            }
        }
        return out;
    }
}
