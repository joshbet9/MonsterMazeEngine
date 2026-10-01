package me.monstermaze.engine.maze;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * Ripped maze layouts from original Mineplex Monster Maze (via MonsterMaze 1.8 plugin).
 * Cell values: 0 empty; 1 path; 2 path+spawn; 3/4 center; 5/6 center+path; 4/6 barrier.
 */
public final class Layouts {

    public static final int SIZE = 99;
    public static final int HALF = 49;
    public static final int LAYOUT_COUNT = 3;

    public static final int[][][] ALL;

    static {
        ALL = new int[][][] {
            parse(unpack(MazeData1.PACKED)),
            parse(unpack(MazeData2.PACKED)),
            parse(unpack(MazeData3.PACKED))
        };
    }

    private static String unpack(String b64) {
        try {
            byte[] gz = Base64.getDecoder().decode(b64);
            try (GZIPInputStream gin = new GZIPInputStream(new ByteArrayInputStream(gz));
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = gin.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static int[][] parse(String text) {
        String[] rows = text.trim().split("\\R");
        if (rows.length != SIZE) {
            throw new ExceptionInInitializerError("expected " + SIZE + " rows, got " + rows.length);
        }
        int[][] maze = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            String row = rows[r].trim();
            if (row.length() != SIZE) {
                throw new ExceptionInInitializerError("row " + r + " length " + row.length());
            }
            for (int c = 0; c < SIZE; c++) {
                maze[r][c] = row.charAt(c) - '0';
            }
        }
        return maze;
    }

    public static boolean isRawPath(int cell) {
        return cell == 1 || cell == 2 || cell == 5 || cell == 6;
    }

    public static boolean isSpawn(int cell) {
        return cell == 2;
    }

    public static boolean isCenter(int cell) {
        return cell >= 3 && cell <= 6;
    }

    public static boolean isBarrier(int cell) {
        return cell == 4 || cell == 6;
    }

    public static int[][] copy(int layoutId) {
        if (layoutId < 0 || layoutId >= ALL.length) {
            throw new IllegalArgumentException("layoutId must be 0.." + (ALL.length - 1));
        }
        int[][] src = ALL[layoutId];
        int[][] dst = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i].clone();
        }
        return dst;
    }

    private Layouts() {}
}
