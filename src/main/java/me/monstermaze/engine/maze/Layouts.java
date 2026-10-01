package me.monstermaze.engine.maze;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Ripped maze layouts from original Mineplex Monster Maze (via MonsterMaze 1.8 plugin).
 * Cell values: 0 empty; 1 path; 2 path+spawn; 3/4 center; 5/6 center+path; 4/6 barrier.
 * Digit-row data lives under {@code /layouts/MAZE_N.txt} on the classpath.
 */
public final class Layouts {

    public static final int SIZE = 99;
    public static final int HALF = 49;
    public static final int LAYOUT_COUNT = 3;

    public static final int[][][] ALL;

    static {
        ALL = new int[][][] {
            load("layouts/MAZE_1.txt"),
            load("layouts/MAZE_2.txt"),
            load("layouts/MAZE_3.txt")
        };
    }

    private static int[][] load(String resource) {
        InputStream in = Layouts.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new ExceptionInInitializerError("Missing resource: " + resource);
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> rows = new ArrayList<>(SIZE);
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    rows.add(line);
                }
            }
            if (rows.size() != SIZE) {
                throw new ExceptionInInitializerError(
                        resource + " expected " + SIZE + " rows, got " + rows.size());
            }
            int[][] maze = new int[SIZE][SIZE];
            for (int r = 0; r < SIZE; r++) {
                String row = rows.get(r);
                if (row.length() != SIZE) {
                    throw new ExceptionInInitializerError(
                            resource + " row " + r + " length " + row.length());
                }
                for (int c = 0; c < SIZE; c++) {
                    maze[r][c] = row.charAt(c) - '0';
                }
            }
            return maze;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** True if cell type is a walkable path for monsters/players (raw). */
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
