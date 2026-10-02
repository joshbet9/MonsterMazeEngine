package me.monstermaze.engine.physics;

/** Minimal immutable axis-aligned bounding box used by the 1.8 floor collider. */
public record Aabb(double minX, double minY, double minZ,
                   double maxX, double maxY, double maxZ) {
    public Aabb expand(double x, double y, double z) {
        return new Aabb(minX - x, minY - y, minZ - z,
                maxX + x, maxY + y, maxZ + z);
    }
    public Aabb offset(double x, double y, double z) {
        return new Aabb(minX + x, minY + y, minZ + z,
                maxX + x, maxY + y, maxZ + z);
    }
    public double clipX(Aabb moving, double delta) {
        if (moving.maxY <= minY || moving.minY >= maxY
                || moving.maxZ <= minZ || moving.minZ >= maxZ) return delta;
        if (delta > 0.0 && moving.maxX <= minX) {
            double d = minX - moving.maxX;
            return d < delta ? d : delta;
        }
        if (delta < 0.0 && moving.minX >= maxX) {
            double d = maxX - moving.minX;
            return d > delta ? d : delta;
        }
        return delta;
    }
    public double clipY(Aabb moving, double delta) {
        if (moving.maxX <= minX || moving.minX >= maxX
                || moving.maxZ <= minZ || moving.minZ >= maxZ) return delta;
        if (delta > 0.0 && moving.maxY <= minY) {
            double d = minY - moving.maxY;
            return d < delta ? d : delta;
        }
        if (delta < 0.0 && moving.minY >= maxY) {
            double d = maxY - moving.minY;
            return d > delta ? d : delta;
        }
        return delta;
    }
    public double clipZ(Aabb moving, double delta) {
        if (moving.maxX <= minX || moving.minX >= maxX
                || moving.maxY <= minY || moving.minY >= maxY) return delta;
        if (delta > 0.0 && moving.maxZ <= minZ) {
            double d = minZ - moving.maxZ;
            return d < delta ? d : delta;
        }
        if (delta < 0.0 && moving.minZ >= maxZ) {
            double d = maxZ - moving.minZ;
            return d > delta ? d : delta;
        }
        return delta;
    }
}
