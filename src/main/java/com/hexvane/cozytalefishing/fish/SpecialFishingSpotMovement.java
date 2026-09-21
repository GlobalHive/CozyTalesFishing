package com.hexvane.cozytalefishing.fish;

import org.joml.Vector3d;

/** Surface-aware wandering within a school. Hooked fish can still be reeled out. */
final class SpecialFishingSpotMovement {
    private static final double[] TURNS = {0, Math.PI / 4, -Math.PI / 4,
        Math.PI / 2, -Math.PI / 2, Math.PI * 3 / 4, -Math.PI * 3 / 4, Math.PI};

    @FunctionalInterface
    interface SurfaceMove {
        boolean move(Vector3d position, double dx, double dz);
    }

    static double distanceSquared(Vector3d position, Vector3d center) {
        double dx = position.x - center.x;
        double dz = position.z - center.z;
        return dx * dx + dz * dz;
    }

    static float move(Vector3d position, Vector3d center, float yaw, double step, SurfaceMove surface) {
        double radius = SpecialFishingSpot.RADIUS - 0.25;
        double before = distanceSquared(position, center);
        // Movement uses +sin(yaw) on X, unlike the engine's viewing-yaw convention.
        if (before >= (radius - 0.6) * (radius - 0.6)) {
            yaw = FishShadowVision.swimYaw((float) (center.x - position.x), (float) (center.z - position.z));
        }
        if (step <= 0) return yaw;
        // Probe ahead so a fish turns before touching the bank, instead of jittering against it.
        double probeDistance = Math.max(step, 0.5);
        for (double turn : TURNS) {
            float direction = (float) (yaw + turn);
            double dx = Math.sin(direction), dz = Math.cos(direction);
            Vector3d probe = new Vector3d(position);
            if (!surface.move(probe, dx * probeDistance, dz * probeDistance)) continue;
            double after = distanceSquared(probe, center);
            if (after > radius * radius && after >= before) continue;
            if (before <= radius * radius && after > radius * radius) continue;
            if (Math.abs(probe.y - center.y) > 0.25) continue;
            Vector3d next = new Vector3d(position);
            if (surface.move(next, dx * step, dz * step) && Math.abs(next.y - center.y) <= 0.25) {
                position.set(next);
                return direction;
            }
        }
        // No safe heading: hold still without changing heading every frame.
        return yaw;
    }
}
