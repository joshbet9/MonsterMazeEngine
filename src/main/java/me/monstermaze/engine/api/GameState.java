package me.monstermaze.engine.api;

import java.util.Collections;
import java.util.List;

/**
 * Full immutable snapshot of a Monster Maze game at a single tick.
 */
public final class GameState {
    public final long tick;
    public final MazeMode mode;
    public final GamePhase phase;
    public final int stage;
    public final int phaseTimerTicks;
    public final int phaseTimerMax;
    /** 0 = not started, up to 11 = fully deteriorated. */
    public final int centerDeteriorationStep;
    public final MazeState maze;
    public final PlayerState player;
    public final List<MonsterState> monsters;
    public final SafePadState activePad;   // nullable
    public final SafePadState previewPad;  // nullable

    public GameState(
            long tick,
            MazeMode mode,
            GamePhase phase,
            int stage,
            int phaseTimerTicks,
            int phaseTimerMax,
            int centerDeteriorationStep,
            MazeState maze,
            PlayerState player,
            List<MonsterState> monsters,
            SafePadState activePad,
            SafePadState previewPad) {
        this.tick = tick;
        this.mode = mode;
        this.phase = phase;
        this.stage = stage;
        this.phaseTimerTicks = phaseTimerTicks;
        this.phaseTimerMax = phaseTimerMax;
        this.centerDeteriorationStep = centerDeteriorationStep;
        this.maze = maze;
        this.player = player;
        this.monsters = monsters == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(monsters);
        this.activePad = activePad;
        this.previewPad = previewPad;
    }
}
