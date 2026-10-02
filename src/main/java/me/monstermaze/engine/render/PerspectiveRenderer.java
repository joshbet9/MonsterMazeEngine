package me.monstermaze.engine.render;

import me.monstermaze.engine.api.GameState;
import me.monstermaze.engine.api.MazeState;
import me.monstermaze.engine.api.MonsterState;
import me.monstermaze.engine.api.SafePadState;
import me.monstermaze.engine.maze.Coordinates;
import me.monstermaze.engine.maze.Layouts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lightweight software 3D renderer for inspecting the pure simulation. */
public final class PerspectiveRenderer {
    private static final double FOV_DEGREES = 72.0;
    private static final double MAX_RENDER_DISTANCE = 34.0;

    public BufferedFrame render(GameState state, Dimension viewSize,
                                boolean firstPerson, float cameraYaw,
                                float cameraPitch, double cameraDistance) {
        int width = viewSize != null && viewSize.width > 0 ? viewSize.width : 960;
        int height = viewSize != null && viewSize.height > 0 ? viewSize.height : 720;

        var image = new java.awt.image.BufferedImage(
                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        g.setPaint(new GradientPaint(
                0, 0, new Color(105, 155, 205),
                0, height, new Color(26, 37, 50)));
        g.fillRect(0, 0, width, height);

        double targetX = state.player.pos.x;
        double targetY = state.player.pos.y + 0.9;
        double targetZ = state.player.pos.z;

        double camX;
        double camY;
        double camZ;
        if (firstPerson) {
            camX = state.player.pos.x;
            camY = state.player.pos.y + 1.62;
            camZ = state.player.pos.z;
        } else {
            double yaw = Math.toRadians(cameraYaw);
            double pitch = Math.toRadians(cameraPitch);
            camX = targetX - Math.sin(yaw) * Math.cos(pitch) * cameraDistance;
            camY = targetY - Math.sin(pitch) * cameraDistance;
            camZ = targetZ - Math.cos(yaw) * Math.cos(pitch) * cameraDistance;
        }

        Camera camera = new Camera(
                camX, camY, camZ,
                firstPerson ? state.player.yaw : cameraYaw,
                cameraPitch,
                width / 2.0,
                height / 2.0 + 24.0,
                height / (2.0 * Math.tan(Math.toRadians(FOV_DEGREES) / 2.0)));

        ArrayList<Face> faces = new ArrayList<>(5000);
        MazeState maze = state.maze;

        int minRow = Math.max(0,
                (int) Math.floor(Coordinates.layoutRow(
                        state.centerX, (int) Math.floor(targetX - MAX_RENDER_DISTANCE))) - 1);
        int maxRow = Math.min(Layouts.SIZE - 1,
                (int) Math.ceil(Coordinates.layoutRow(
                        state.centerX, (int) Math.ceil(targetX + MAX_RENDER_DISTANCE))) + 1);
        int minCol = Math.max(0,
                (int) Math.floor(Coordinates.layoutCol(
                        state.centerZ, (int) Math.floor(targetZ - MAX_RENDER_DISTANCE))) - 1);
        int maxCol = Math.min(Layouts.SIZE - 1,
                (int) Math.ceil(Coordinates.layoutCol(
                        state.centerZ, (int) Math.ceil(targetZ + MAX_RENDER_DISTANCE))) + 1);

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                if (!maze.physicalFloor[row][col]) continue;

                double cx = Coordinates.pathCenterX(state.centerX, row);
                double cz = Coordinates.pathCenterZ(state.centerZ, col);
                if (Math.hypot(cx - targetX, cz - targetZ) > MAX_RENDER_DISTANCE) continue;

                double topY = maze.padSurface[row][col]
                        ? state.centerY - 0.90
                        : state.centerY;
                double bottomY = topY - 0.18;

                addCube(faces, camera,
                        cx - 0.5, bottomY, cz - 0.5,
                        cx + 0.5, topY, cz + 0.5,
                        blockColor(maze, row, col));
            }
        }

        addCube(faces, camera,
                state.centerX - 70, state.centerY - 4.2, state.centerZ - 70,
                state.centerX + 70, state.centerY - 4.12, state.centerZ + 70,
                new Color(17, 21, 27));

        if (state.activePad != null) {
            addCube(faces, camera,
                    state.activePad.centerX - 2.5, state.activePad.surfaceY,
                    state.activePad.centerZ - 2.5,
                    state.activePad.centerX + 2.5, state.activePad.surfaceY + 0.15,
                    state.activePad.centerZ + 2.5,
                    new Color(210, 171, 60));
        }
        if (state.previewPad != null) {
            addCube(faces, camera,
                    state.previewPad.centerX - 2.5, state.previewPad.surfaceY,
                    state.previewPad.centerZ - 2.5,
                    state.previewPad.centerX + 2.5, state.previewPad.surfaceY + 0.12,
                    state.previewPad.centerZ + 2.5,
                    new Color(88, 200, 205));
        }

        for (MonsterState monster : state.monsters) {
            if (monster.removed) continue;
            if (Math.hypot(monster.pos.x - targetX, monster.pos.z - targetZ)
                    > MAX_RENDER_DISTANCE + 7) continue;

            Color body = monster.frozenTicks > 0
                    ? new Color(75, 205, 255)
                    : monster.launched
                    ? new Color(255, 110, 215)
                    : new Color(225, 225, 230);

            addCube(faces, camera,
                    monster.pos.x - 0.35, monster.pos.y,
                    monster.pos.z - 0.35,
                    monster.pos.x + 0.35, monster.pos.y + 1.0,
                    monster.pos.z + 0.35,
                    body);
            addCube(faces, camera,
                    monster.pos.x - 0.29, monster.pos.y + 1.0,
                    monster.pos.z - 0.29,
                    monster.pos.x + 0.29, monster.pos.y + 1.65,
                    monster.pos.z + 0.29,
                    body.brighter());
        }

        if (!firstPerson) {
            addCube(faces, camera,
                    state.player.pos.x - 0.30, state.player.pos.y,
                    state.player.pos.z - 0.30,
                    state.player.pos.x + 0.30, state.player.pos.y + 1.8,
                    state.player.pos.z + 0.30,
                    new Color(65, 145, 235));
            addCube(faces, camera,
                    state.player.pos.x - 0.28, state.player.pos.y + 1.8,
                    state.player.pos.z - 0.28,
                    state.player.pos.x + 0.28, state.player.pos.y + 2.2,
                    state.player.pos.z + 0.28,
                    new Color(238, 196, 145));
        }

        faces.sort(Comparator.comparingDouble((Face f) -> f.depth).reversed());
        for (Face face : faces) {
            g.setColor(face.color);
            g.fillPolygon(face.polygon);
        }

        drawPadRing(g, camera, state.activePad, new Color(255, 225, 75));
        drawPadRing(g, camera, state.previewPad, new Color(120, 255, 255));

        g.dispose();
        return new BufferedFrame(image);
    }

    private static Color blockColor(MazeState maze, int row, int col) {
        if (maze.padSurface[row][col]) return new Color(215, 177, 64);
        int raw = maze.raw[row][col];
        if (raw == 5 || raw == 6) {
            return maze.traversable[row][col]
                    ? new Color(105, 105, 118)
                    : new Color(77, 72, 82);
        }
        if (raw == 2) return new Color(150, 120, 94);
        if (!maze.traversable[row][col]) return new Color(70, 63, 66);
        return new Color(106, 87, 68);
    }

    private static void drawPadRing(Graphics2D g, Camera camera,
                                     SafePadState pad, Color color) {
        if (pad == null) return;
        P2 previous = null;
        g.setColor(color);
        g.setStroke(new BasicStroke(2.0f));
        for (int i = 0; i <= 32; i++) {
            double angle = 2.0 * Math.PI * i / 32.0;
            P2 current = camera.project(
                    pad.centerX + Math.cos(angle) * 2.45,
                    pad.surfaceY + 0.18,
                    pad.centerZ + Math.sin(angle) * 2.45);
            if (previous != null && current != null) {
                g.drawLine((int) previous.x, (int) previous.y,
                        (int) current.x, (int) current.y);
            }
            previous = current;
        }
    }

    private static void addCube(List<Face> faces, Camera camera,
                                double x0, double y0, double z0,
                                double x1, double y1, double z1,
                                Color color) {
        double[][] v = {
                {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1},
                {x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}
        };
        int[][] idx = {
                {0, 1, 2, 3},
                {4, 7, 6, 5},
                {0, 4, 5, 1},
                {1, 5, 6, 2},
                {2, 6, 7, 3},
                {3, 7, 4, 0}
        };
        double[] shade = {0.64, 1.00, 0.84, 0.76, 0.70, 0.79};

        for (int i = 0; i < idx.length; i++) {
            Polygon polygon = new Polygon();
            double depth = 0.0;
            boolean visible = true;
            for (int pointIndex : idx[i]) {
                P2 p = camera.project(v[pointIndex][0], v[pointIndex][1], v[pointIndex][2]);
                if (p == null) {
                    visible = false;
                    break;
                }
                polygon.addPoint((int) Math.round(p.x), (int) Math.round(p.y));
                depth += p.depth;
            }
            if (visible) {
                faces.add(new Face(polygon, depth / idx[i].length,
                        shade(color, shade[i])));
            }
        }
    }

    private static Color shade(Color color, double factor) {
        return new Color(
                clamp((int) Math.round(color.getRed() * factor)),
                clamp((int) Math.round(color.getGreen() * factor)),
                clamp((int) Math.round(color.getBlue() * factor)));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private record Face(Polygon polygon, double depth, Color color) {}
    public record BufferedFrame(java.awt.image.BufferedImage image) {}

    private record Camera(double x, double y, double z, float yaw, float pitch,
                          double screenX, double screenY, double focalLength) {
        P2 project(double wx, double wy, double wz) {
            double dx = wx - x;
            double dy = wy - y;
            double dz = wz - z;

            double yawRad = Math.toRadians(yaw);
            double pitchRad = Math.toRadians(pitch);

            double fx = Math.sin(yawRad) * Math.cos(pitchRad);
            double fy = Math.sin(pitchRad);
            double fz = Math.cos(yawRad) * Math.cos(pitchRad);

            double rx = Math.cos(yawRad);
            double rz = -Math.sin(yawRad);

            double ux = -Math.sin(pitchRad) * Math.sin(yawRad);
            double uy = Math.cos(pitchRad);
            double uz = -Math.sin(pitchRad) * Math.cos(yawRad);

            double cameraX = dx * rx + dz * rz;
            double cameraY = dx * ux + dy * uy + dz * uz;
            double cameraZ = dx * fx + dy * fy + dz * fz;

            if (cameraZ <= 0.15) return null;

            return new P2(
                    screenX + cameraX * focalLength / cameraZ,
                    screenY - cameraY * focalLength / cameraZ,
                    cameraZ);
        }
    }

    private record P2(double x, double y, double depth) {}
}
