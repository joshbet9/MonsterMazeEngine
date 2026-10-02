package me.monstermaze.engine.api;

/** Mutable per-monster simulation state matching MonsterMazeAI's model. */
public final class MonsterState {
    public final int id;
    public Vec3 pos;
    public Vec3 vel;
    public int targetWaypointX;
    public int targetWaypointZ;
    public int direction;
    public boolean launched;
    public int frozenTicks;
    public long frozenUntilTick;
    public long launchedUntilTick;
    public long launchedAtTick;
    public boolean removed;

    public MonsterState(int id, Vec3 pos, Vec3 vel,
                        int targetWaypointX, int targetWaypointZ,
                        int direction, boolean launched, int frozenTicks) {
        this.id = id;
        this.pos = pos;
        this.vel = vel;
        this.targetWaypointX = targetWaypointX;
        this.targetWaypointZ = targetWaypointZ;
        this.direction = direction;
        this.launched = launched;
        this.frozenTicks = frozenTicks;
        this.frozenUntilTick = 0L;
        this.launchedUntilTick = 0L;
        this.launchedAtTick = 0L;
        this.removed = false;
    }

    public MonsterState copy() {
        MonsterState m = new MonsterState(id, pos, vel, targetWaypointX, targetWaypointZ,
                direction, launched, frozenTicks);
        m.frozenUntilTick = frozenUntilTick;
        m.launchedUntilTick = launchedUntilTick;
        m.launchedAtTick = launchedAtTick;
        m.removed = removed;
        return m;
    }

    public boolean frozen(long tick) {
        return frozen || frozenUntilTick > tick;
    }

    private boolean frozen = false;
    public boolean launched(long tick) {
        return launched || launchedUntilTick > tick;
    }
}
