package me.monstermaze.engine.game;

import me.monstermaze.engine.api.MazeMode;

/** Source-derived phase timing for Monster Maze 1.8. */
public final class StageTimer {
    public static final int PREVIEW_SECONDS = 2;
    public static final int ALL_ON_PAD_SECONDS = 4;
    public static final int CENTER_DETERIORATION_START_SECONDS = 20;

    private StageTimer() {}

    public static int maxTicks(MazeMode mode, int stage) {
        return initialTicks(mode, stage);
    }

    public static int initialTicks(MazeMode mode, int stage) {
        int safe = Math.max(1, stage);
        int seconds;
        if (mode == MazeMode.ORIGINAL || mode == MazeMode.SPEED) {
            seconds = Math.max(15, 60 - ((safe - 1) * 2));
        } else {
            // Same progression used by MonsterMazeAI's modern simulator.
            seconds = Math.max(15, 35 - ((safe - 1) * 20 / 9));
        }
        return seconds * 20;
    }

    public static int firstArrivalCapTicks(int stage, int currentRemainingTicks) {
        int seconds = Math.max(6, 16 - Math.max(0, stage - 1));
        return Math.min(currentRemainingTicks, seconds * 20);
    }

    public static int starterMonsters(MazeMode mode) {
        return mode == MazeMode.MODERN ? 225 : 150;
    }

    public static int monstersPerTransition(MazeMode mode) {
        return mode == MazeMode.MODERN ? 30 : 15;
    }
}
