package me.monstermaze.engine.game;

import me.monstermaze.engine.api.MazeMode;

/**
 * Phase timer rules from 1.8 mechanics docs.
 */
public final class StageTimer {

    private StageTimer() {}

    /** Max phase length in ticks for a stage (20 tps). */
    public static int maxTicks(MazeMode mode, int stage) {
        int startSec = (mode == MazeMode.MODERN) ? 35 : 60;
        int sec = Math.max(15, startSec - 2 * stage);
        return sec * 20;
    }

    /**
     * First-arrival shortened remaining time.
     * max(6, 16 - (stage - 1)) seconds, but not above current remaining.
     */
    public static int firstArrivalCapTicks(int stage, int currentRemainingTicks) {
        int sec = Math.max(6, 16 - Math.max(0, stage - 1));
        int cap = sec * 20;
        return Math.min(currentRemainingTicks, cap);
    }

    public static final int ALL_ON_PAD_SECONDS = 4;
    public static final int PREVIEW_SECONDS = 2;
    public static final int CENTER_DETERIORATION_START_TICKS = 20 * 20;

    public static int starterMonsters(MazeMode mode) {
        return mode == MazeMode.MODERN ? 225 : 150;
    }

    public static int monstersPerTransition(MazeMode mode) {
        return mode == MazeMode.MODERN ? 30 : 15;
    }
}
