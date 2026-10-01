package me.monstermaze.engine.api;

/**
 * Simple immutable 3D vector used throughout the engine.
 */
public final class Vec3 {
    public final double x;
    public final double y;
    public final double z;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static final Vec3 ZERO = new Vec3(0, 0, 0);

    public Vec3 add(Vec3 o) {
        return new Vec3(x + o.x, y + o.y, z + o.z);
    }

    public Vec3 scale(double s) {
        return new Vec3(x * s, y * s, z * s);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public Vec3 normalize() {
        double len = length();
        if (len < 1e-9) return ZERO;
        return scale(1.0 / len);
    }

    public Vec3 withY(double newY) {
        return new Vec3(x, newY, z);
    }

    @Override
    public String toString() {
        return String.format("Vec3(%.4f, %.4f, %.4f)", x, y, z);
    }
}
