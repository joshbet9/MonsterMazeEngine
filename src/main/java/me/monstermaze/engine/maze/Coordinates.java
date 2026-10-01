package me.monstermaze.engine.maze;

/**
 * Maps layout row/column indices to world coordinates and back.
 *
 * <p>Matches the 1.8 plugin / AI mechanics docs:
 * <pre>
 *   worldX = centerBlockX - 49 + layoutRow
 *   worldZ = centerBlockZ - 49 + layoutColumn
 * </pre>
 * Path entities sit at block centres ( +0.5, centerY, +0.5 ).
 */
public final class Coordinates {

    public static final int SIZE = Layouts.SIZE;
    public static final int HALF = Layouts.HALF;

    /** Default arena centre used by the pure engine (arbitrary origin). */
    public static final int DEFAULT_CENTER_X = 0;
    public static final int DEFAULT_CENTER_Y = 64;
    public static final int DEFAULT_CENTER_Z = 0;

    private Coordinates() {}

    public static int worldX(int centerBlockX, int layoutRow) {
        return centerBlockX - HALF + layoutRow;
    }

    public static int worldZ(int centerBlockZ, int layoutCol) {
        return centerBlockZ - HALF + layoutCol;
    }

    public static int layoutRow(int centerBlockX, int worldBlockX) {
        return worldBlockX - centerBlockX + HALF;
    }

    public static int layoutCol(int centerBlockZ, int worldBlockZ) {
        return worldBlockZ - centerBlockZ + HALF;
    }

    public static boolean inBounds(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    /** Block-centre position for a path cell. */
    public static double pathCenterX(int centerBlockX, int layoutRow) {
        return worldX(centerBlockX, layoutRow) + 0.5;
    }

    public static double pathCenterZ(int centerBlockZ, int layoutCol) {
        return worldZ(centerBlockZ, layoutCol) + 0.5;
    }
}
