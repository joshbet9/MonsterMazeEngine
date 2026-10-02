package me.monstermaze.engine.render;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.maze.Layouts;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Top-down renderer with optional camera follow + zoom. */
public final class TopDownRenderer {

    private final int baseScale;

    public TopDownRenderer() { this(4); }

    public TopDownRenderer(int pixelsPerCell) {
        this.baseScale = Math.max(1, pixelsPerCell);
    }

    public BufferedImage render(GameState state) {
        int size = Layouts.SIZE * baseScale;
        return renderCamera(state, new Dimension(size, size), 1.0);
    }

    public BufferedImage renderCamera(GameState state, Dimension viewSize, double zoom) {
        int vw = viewSize != null && viewSize.width > 0 ? viewSize.width : Layouts.SIZE * baseScale;
        int vh = viewSize != null && viewSize.height > 0 ? viewSize.height : Layouts.SIZE * baseScale;
        double scale = baseScale * Math.max(0.5, Math.min(8.0, zoom));

        BufferedImage img = new BufferedImage(vw, vh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(12, 12, 16));
        g.fillRect(0, 0, vw, vh);

        double playerRow = state.player.pos.x + Layouts.HALF;
        double playerCol = state.player.pos.z + Layouts.HALF;
        double originRow = playerRow - (vh / 2.0) / scale;
        double originCol = playerCol - (vw / 2.0) / scale;

        int[][] raw = state.maze.raw;
        boolean[][] trav = state.maze.traversable;
        int r0 = Math.max(0, (int) Math.floor(originRow) - 1);
        int r1 = Math.min(Layouts.SIZE - 1, (int) Math.ceil(originRow + vh / scale) + 1);
        int c0 = Math.max(0, (int) Math.floor(originCol) - 1);
        int c1 = Math.min(Layouts.SIZE - 1, (int) Math.ceil(originCol + vw / scale) + 1);

        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                int cell = raw[r][c];
                Color color;
                if (cell == 0) color = new Color(14, 14, 18);
                else if (!trav[r][c]) color = new Color(28, 22, 22);
                else if (Layouts.isSpawn(cell)) color = new Color(36, 110, 48);
                else if (Layouts.isCenter(cell)) color = new Color(55, 55, 85);
                else if (Layouts.isRawPath(cell)) {
                    boolean alt = ((r + c) & 1) == 0;
                    color = alt ? new Color(58, 58, 66) : new Color(48, 48, 56);
                } else color = new Color(30, 30, 35);
                int sx = (int) Math.round((c - originCol) * scale);
                int sy = (int) Math.round((r - originRow) * scale);
                int ss = Math.max(1, (int) Math.ceil(scale));
                g.setColor(color);
                g.fillRect(sx, sy, ss, ss);
            }
        }

        drawPad(g, state.activePad, originRow, originCol, scale,
                new Color(60, 220, 120, 200), new Color(180, 255, 200));
        drawPad(g, state.previewPad, originRow, originCol, scale,
                new Color(220, 200, 60, 140), new Color(255, 240, 120));

        for (MonsterState m : state.monsters) {
            int[] rc = worldToCell(m.pos.x, m.pos.z);
            if (rc == null) continue;
            int sx = (int) Math.round((rc[1] + 0.5 - originCol) * scale);
            int sy = (int) Math.round((rc[0] + 0.5 - originRow) * scale);
            int s = Math.max(3, (int) (scale * 0.7));
            if (m.frozenTicks > 0) g.setColor(new Color(120, 200, 255));
            else if (m.launched) g.setColor(new Color(255, 190, 60));
            else g.setColor(new Color(230, 70, 70));
            g.fillOval(sx - s / 2, sy - s / 2, s, s);
            g.setColor(new Color(0, 0, 0, 80));
            g.drawOval(sx - s / 2, sy - s / 2, s, s);
        }

        int[] prc = worldToCell(state.player.pos.x, state.player.pos.z);
        if (prc != null) {
            int sx = (int) Math.round((prc[1] + 0.5 - originCol) * scale);
            int sy = (int) Math.round((prc[0] + 0.5 - originRow) * scale);
            int s = Math.max(4, (int) (scale * 0.9));
            g.setColor(state.player.onSafePad ? new Color(120, 255, 190) : new Color(90, 190, 255));
            g.fillOval(sx - s / 2, sy - s / 2, s, s);
            g.setColor(Color.WHITE);
            g.drawOval(sx - s / 2, sy - s / 2, s, s);
            double yawRad = Math.toRadians(state.player.yaw);
            int fx = (int) Math.round(sx + Math.cos(yawRad) * s);
            int fy = (int) Math.round(sy - Math.sin(yawRad) * s);
            g.setStroke(new BasicStroke(2f));
            g.drawLine(sx, sy, fx, fy);
        }

        g.dispose();
        return img;
    }

    public void writePng(GameState state, Path path) throws IOException {
        ImageIO.write(render(state), "png", path.toFile());
    }

    private void drawPad(Graphics2D g, SafePadState pad, double originRow, double originCol,
                         double scale, Color fill, Color border) {
        if (pad == null) return;
        // Path/player centres are world block coordinate + 0.5. Keep the
        // Safe Pad centred on that same continuous coordinate system.
        double row = pad.centerX + Layouts.HALF + 0.5;
        double col = pad.centerZ + Layouts.HALF + 0.5;
        double half = 2.5;
        int sx = (int) Math.round((col - half - originCol) * scale);
        int sy = (int) Math.round((row - half - originRow) * scale);
        int size = (int) Math.round(5.0 * scale);
        g.setColor(fill);
        g.fillRoundRect(sx, sy, size, size, 6, 6);
        g.setColor(border);
        g.setStroke(new BasicStroke(Math.max(1f, (float) (scale * 0.25))));
        g.drawRoundRect(sx, sy, size, size, 6, 6);
        int cx = (int) Math.round((col - originCol) * scale);
        int cy = (int) Math.round((row - originRow) * scale);
        int bs = Math.max(2, (int) (scale * 0.5));
        g.setColor(Color.WHITE);
        g.fillOval(cx - bs / 2, cy - bs / 2, bs, bs);
    }

    private int[] worldToCell(double wx, double wz) {
        int row = (int) Math.floor(wx) + Layouts.HALF;
        int col = (int) Math.floor(wz) + Layouts.HALF;
        if (row < 0 || row >= Layouts.SIZE || col < 0 || col >= Layouts.SIZE) return null;
        return new int[]{row, col};
    }
}
