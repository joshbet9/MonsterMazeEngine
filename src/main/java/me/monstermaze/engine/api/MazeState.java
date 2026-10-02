package me.monstermaze.engine.api;

/** Static and dynamic maze snapshot. */
public final class MazeState {
    public final int layoutId;
    public final int[][] raw;
    public final boolean[][] traversable;
    public final boolean[][] physicalFloor;
    public final boolean[][] padSurface;

    public MazeState(int layoutId, int[][] raw, boolean[][] traversable) {
        this(layoutId, raw, traversable, traversable, new boolean[raw.length][raw[0].length]);
    }

    public MazeState(int layoutId, int[][] raw, boolean[][] traversable,
                     boolean[][] physicalFloor, boolean[][] padSurface) {
        this.layoutId = layoutId;
        this.raw = raw;
        this.traversable = traversable;
        this.physicalFloor = physicalFloor;
        this.padSurface = padSurface;
    }
}
