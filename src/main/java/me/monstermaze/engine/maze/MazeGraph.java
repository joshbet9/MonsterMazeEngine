package me.monstermaze.engine.maze;

import me.monstermaze.engine.api.MazeState;

/**
 * Holds raw layout data and current traversability.
 * Traversability starts as all raw path cells and can be disabled
 * (Safe Pads, center deterioration, explicit waypoint disables).
 */
public final class MazeGraph {

    private final int layoutId;
    private final int[][] raw;
    private final boolean[][] traversable;

    public MazeGraph(int layoutId) {
        this.layoutId = layoutId;
        this.raw = Layouts.copy(layoutId);
        this.traversable = new boolean[Layouts.SIZE][Layouts.SIZE];
        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                traversable[r][c] = Layouts.isRawPath(raw[r][c]);
            }
        }
    }

    public int layoutId() {
        return layoutId;
    }

    public int raw(int row, int col) {
        return raw[row][col];
    }

    public boolean isTraversable(int row, int col) {
        return Coordinates.inBounds(row, col) && traversable[row][col];
    }

    public void setTraversable(int row, int col, boolean value) {
        if (Coordinates.inBounds(row, col)) {
            traversable[row][col] = value;
        }
    }

    /** Disable a cell for monster pathing (e.g. under an active Safe Pad). */
    public void disable(int row, int col) {
        setTraversable(row, col, false);
    }

    public void enableIfRawPath(int row, int col) {
        if (Coordinates.inBounds(row, col) && Layouts.isRawPath(raw[row][col])) {
            traversable[row][col] = true;
        }
    }

    public MazeState toMazeState() {
        int[][] rawCopy = new int[Layouts.SIZE][];
        boolean[][] travCopy = new boolean[Layouts.SIZE][];
        for (int i = 0; i < Layouts.SIZE; i++) {
            rawCopy[i] = raw[i].clone();
            travCopy[i] = traversable[i].clone();
        }
        return new MazeState(layoutId, rawCopy, travCopy);
    }

    /** Collect all spawn cell centres (cell type 2) as layout row/col pairs. */
    public int[][] spawnCells() {
        int count = 0;
        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                if (Layouts.isSpawn(raw[r][c])) count++;
            }
        }
        int[][] out = new int[count][2];
        int i = 0;
        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                if (Layouts.isSpawn(raw[r][c])) {
                    out[i][0] = r;
                    out[i][1] = c;
                    i++;
                }
            }
        }
        return out;
    }

    /** Cardinal neighbours that are currently traversable. */
    public int[][] traversableCardinals(int row, int col) {
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        int n = 0;
        int[][] tmp = new int[4][2];
        for (int[] d : dirs) {
            int nr = row + d[0];
            int nc = col + d[1];
            if (isTraversable(nr, nc)) {
                tmp[n][0] = nr;
                tmp[n][1] = nc;
                n++;
            }
        }
        int[][] out = new int[n][2];
        for (int i = 0; i < n; i++) {
            out[i][0] = tmp[i][0];
            out[i][1] = tmp[i][1];
        }
        return out;
    }
}
