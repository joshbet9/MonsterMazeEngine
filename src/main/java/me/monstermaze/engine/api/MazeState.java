package me.monstermaze.engine.api;

/**
 * Static + dynamic maze representation.
 * raw[row][col] holds the original cell type from the layout.
 * traversable[row][col] reflects current path availability (pads, decay, disabled waypoints).
 */
public final class MazeState {
    public final int layoutId;
    public final int[][] raw;
    public final boolean[][] traversable;

    public MazeState(int layoutId, int[][] raw, boolean[][] traversable) {
        this.layoutId = layoutId;
        this.raw = raw;
        this.traversable = traversable;
    }
}
