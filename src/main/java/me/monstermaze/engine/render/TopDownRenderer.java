package me.monstermaze.engine.render;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.maze.Layouts;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Simple top-down PNG renderer of a GameState. */
public final class TopDownRenderer {

    private final int scale;

    public TopDownRenderer() {
        this(4);
    }

    public TopDownRenderer(int pixelsPerCell) {
        this.scale = Math.max(1, pixelsPerCell);
    }

    public BufferedImage render(GameState state) {
        int size = Layouts.SIZE * scale;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        int[][] raw = state.maze.raw;
        boolean[][] trav = state.maze.traversable;

        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                int cell = raw[r][c];
                Color color;
                if (cell == 0) color = new Color(15, 15, 20);
                else if (Layouts.isCenter(cell)) color = new Color(60, 60, 90);
                else if (Layouts.isSpawn(cell)) color = new Color(40, 120, 40);
                else if (trav[r][c]) color = new Color(50, 50, 55);
                else color = new Color(30, 30, 35);
                g.setColor(color);
                g.fillRect(c * scale, r * scale, scale, scale);
            }
        }

        drawPad(g, state.activePad, new Color(80, 200, 120, 180));
        drawPad(g, state.previewPad, new Color(200, 200, 80, 120));

        for (MonsterState m : state.monsters) {
            int[] rc = worldToCell(m.pos.x, m.pos.z);
            if (rc == null) continue;
            int px = rc[1] * scale + scale / 4;
            int py = rc[0] * scale + scale / 4;
            int s = Math.max(2, scale / 2);
            if (m.frozenTicks > 0) g.setColor(new Color(100, 180, 255));
            else if (m.launched) g.setColor(new Color(255, 180, 50));
            else g.setColor(new Color(220, 80, 80));
            g.fillOval(px, py, s, s);
        }

        int[] prc = worldToCell(state.player.pos.x, state.player.pos.z);
        if (prc != null) {
            g.setColor(state.player.onSafePad ? new Color(100, 255, 180) : new Color(80, 180, 255));
            int s = Math.max(3, scale);
            g.fillOval(prc[1] * scale, prc[0] * scale, s, s);
        }

        g.dispose();
        return img;
    }

    public void writePng(GameState state, Path path) throws IOException {
        ImageIO.write(render(state), "png", path.toFile());
    }

    private void drawPad(Graphics2D g, SafePadState pad, Color color) {
        if (pad == null) return;
        int row = pad.centerX + Layouts.HALF;
        int col = pad.centerZ + Layouts.HALF;
        g.setColor(color);
        int half = (int) (2.5 * scale);
        g.fillRect(col * scale - half, row * scale - half, half * 2, half * 2);
    }

    private int[] worldToCell(double wx, double wz) {
        int row = (int) Math.floor(wx) + Layouts.HALF;
        int col = (int) Math.floor(wz) + Layouts.HALF;
        if (row < 0 || row >= Layouts.SIZE || col < 0 || col >= Layouts.SIZE) return null;
        return new int[]{row, col};
    }
}
