package me.monstermaze.engine.api;

/**
 * Snapshot of a single monster at a given tick.
 */
public final class MonsterState {
    public final int id;
    public final Vec3 pos;
    public final Vec3 vel;
    /** Grid coordinates of the current target waypoint. */
    public final int targetWaypointX;
    public final int targetWaypointZ;
    /** 0=N, 1=E, 2=S, 3=W, or -1 if none. */
    public final int direction;
    public final boolean launched;
    public final int frozenTicks;

    public MonsterState(
            int id,
            Vec3 pos,
            Vec3 vel,
            int targetWaypointX,
            int targetWaypointZ,
            int direction,
            boolean launched,
            int frozenTicks) {
        this.id = id;
        this.pos = pos;
        this.vel = vel;
        this.targetWaypointX = targetWaypointX;
        this.targetWaypointZ = targetWaypointZ;
        this.direction = direction;
        this.launched = launched;
        this.frozenTicks = frozenTicks;
    }
}
