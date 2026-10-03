package me.monstermaze.engine.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable simulation snapshot whose fields correspond to the state exposed by
 * MonsterMazeAI. The local UI may retain references between ticks; callers
 * should treat a returned TickResult as the authoritative next snapshot.
 *
 * <p>The maze object is a snapshot value, not a per-tick scratch buffer. Engine
 * ticks may safely share the same MazeState instance until the dynamic maze
 * overlay actually changes. The engine replaces it with a new snapshot when
 * a pad/centre mutation occurs.
 */
public final class GameState {
    public long tick;
    public final int centerX;
    public final int centerY;
    public final int centerZ;
    public MazeMode mode;
    public GamePhase phase;
    public int stage = 1;
    public int phaseTimerTicks;
    public int phaseTimerMax;
    public int phaseSecondAccumulatorTicks;
    public int liveSeconds;
    public int centerDeteriorationStep = 11;
    public boolean previewPadRequested;
    public int pendingMonsterSpawns;

    public MazeState maze;
    public PlayerState player;
    public final List<MonsterState> monsters = new ArrayList<>();
    public final List<SafePadState> oldPads = new ArrayList<>();
    public final java.util.Map<String, Integer> oldPadDecaySeconds = new java.util.HashMap<>();

    public SafePadState activePad;
    public SafePadState previewPad;

    public boolean alive = true;
    public boolean completed;
    public boolean inMonsterMaze = true;
    public boolean padReached;

    public GameState copy() {
        GameState s = new GameState(centerX, centerY, centerZ);
        s.tick = tick;
        s.mode = mode;
        s.phase = phase;
        s.stage = stage;
        s.phaseTimerTicks = phaseTimerTicks;
        s.phaseTimerMax = phaseTimerMax;
        s.phaseSecondAccumulatorTicks = phaseSecondAccumulatorTicks;
        s.liveSeconds = liveSeconds;
        s.centerDeteriorationStep = centerDeteriorationStep;
        s.previewPadRequested = previewPadRequested;
        s.pendingMonsterSpawns = pendingMonsterSpawns;
        // MazeState is an immutable-by-convention snapshot. Its arrays are
        // replaced by the engine rather than mutated during a tick, so sharing
        // the reference here avoids cloning 99x99 cells on every action.
        s.maze = maze;
        s.player = player == null ? null : player.copy();
        for (MonsterState m : monsters) s.monsters.add(m.copy());
        s.oldPads.addAll(oldPads);
        s.oldPadDecaySeconds.putAll(oldPadDecaySeconds);
        s.activePad = activePad;
        s.previewPad = previewPad;
        s.alive = alive;
        s.completed = completed;
        s.inMonsterMaze = inMonsterMaze;
        s.padReached = padReached;
        return s;
    }

    public GameState() {
        this(0, 64, 0);
    }

    public GameState(int centerX, int centerY, int centerZ) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
    }

}
