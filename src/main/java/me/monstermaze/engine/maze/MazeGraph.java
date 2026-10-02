package me.monstermaze.engine.maze;

import me.monstermaze.engine.api.MazeState;

import java.util.ArrayList;
import java.util.List;

/**
 * Raw source layout plus dynamic physical floor and monster waypoint state.
 * Raw topology never changes; Safe Pads and center deterioration alter the
 * dynamic overlays separately.
 */
public final class MazeGraph {
    private final int layoutId;
    private final int[][] raw;
    private final boolean[][] disabled;
    private final boolean[][] physicalFloor;
    private final boolean[][] padSurface;

    public MazeGraph(int layoutId) {
        this.layoutId = layoutId;
        this.raw = Layouts.copy(layoutId);
        this.disabled = new boolean[Layouts.SIZE][Layouts.SIZE];
        this.physicalFloor = new boolean[Layouts.SIZE][Layouts.SIZE];
        this.padSurface = new boolean[Layouts.SIZE][Layouts.SIZE];
        for (int r = 0; r < Layouts.SIZE; r++) {
            for (int c = 0; c < Layouts.SIZE; c++) {
                physicalFloor[r][c] = raw[r][c] != 0;
            }
        }
    }

    public int layoutId() { return layoutId; }
    public int raw(int row, int col) { return raw[row][col]; }

    public boolean isRawPath(int row, int col) {
        return Coordinates.inBounds(row, col) && Layouts.isRawPath(raw[row][col]);
    }

    public boolean isTraversable(int row, int col) {
        return Coordinates.inBounds(row, col)
                && isRawPath(row, col)
                && !disabled[row][col];
    }

    public void setTraversable(int row, int col, boolean value) {
        if (Coordinates.inBounds(row, col)) disabled[row][col] = !value;
    }

    public void disable(int row, int col) {
        if (Coordinates.inBounds(row, col)) disabled[row][col] = true;
    }

    public void enableIfRawPath(int row, int col) {
        if (Coordinates.inBounds(row, col) && isRawPath(row, col)) disabled[row][col] = false;
    }

    public boolean isDisabled(int row, int col) {
        return Coordinates.inBounds(row, col) && disabled[row][col];
    }

    public void setPhysicalFloor(int row, int col, boolean value) {
        if (Coordinates.inBounds(row, col)) physicalFloor[row][col] = value;
    }

    public boolean isPhysicalFloor(int row, int col) {
        return Coordinates.inBounds(row, col)
                && (physicalFloor[row][col] || padSurface[row][col]);
    }

    public void setPadSurface(int row, int col, boolean value) {
        if (Coordinates.inBounds(row, col)) padSurface[row][col] = value;
    }

    public boolean hasPadSurface(int row, int col) {
        return Coordinates.inBounds(row, col) && padSurface[row][col];
    }

    public List<int[]> traversableCardinals(int row, int col) {
        List<int[]> out = new ArrayList<>(4);
        int[][] ds = {{-1,0},{1,0},{0,1},{0,-1}};
        for (int[] d : ds) {
            int r = row + d[0], c = col + d[1];
            if (isTraversable(r, c)) out.add(new int[]{r, c});
        }
        return out;
    }

    public List<int[]> physicalCardinals(int row, int col) {
        List<int[]> out = new ArrayList<>(4);
        int[][] ds = {{-1,0},{1,0},{0,1},{0,-1}};
        for (int[] d : ds) {
            int r = row + d[0], c = col + d[1];
            if (isPhysicalFloor(r, c)) out.add(new int[]{r, c});
        }
        return out;
    }

    public MazeState toMazeState() {
        int[][] rawCopy = new int[Layouts.SIZE][];
        boolean[][] travCopy = new boolean[Layouts.SIZE][];
        boolean[][] floorCopy = new boolean[Layouts.SIZE][];
        boolean[][] padCopy = new boolean[Layouts.SIZE][];
        for (int r = 0; r < Layouts.SIZE; r++) {
            rawCopy[r] = raw[r].clone();
            travCopy[r] = new boolean[Layouts.SIZE];
            floorCopy[r] = physicalFloor[r].clone();
            padCopy[r] = padSurface[r].clone();
            for (int c = 0; c < Layouts.SIZE; c++) {
                travCopy[r][c] = isTraversable(r, c);
            }
        }
        return new MazeState(layoutId, rawCopy, travCopy, floorCopy, padCopy);
    }
}
