package me.monstermaze.engine.api;

/**
 * Snapshot of a Safe Pad (active or preview).
 */
public final class SafePadState {
    public final int centerX;
    public final int centerZ;
    public final int surfaceY;
    /** 0 = destroyed/gone, 11 = fully intact. */
    public final int decayStep;
    public final boolean active;
    public final boolean isPreview;

    public SafePadState(
            int centerX,
            int centerZ,
            int surfaceY,
            int decayStep,
            boolean active,
            boolean isPreview) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.surfaceY = surfaceY;
        this.decayStep = decayStep;
        this.active = active;
        this.isPreview = isPreview;
    }
}
