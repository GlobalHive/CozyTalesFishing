package com.hexvane.cozytalefishing.fish;

import java.util.UUID;
import org.joml.Vector3d;

/** One shared, finite school. Escaped fish consume supply too; catches never replenish it. */
final class SpecialFishingSpot {
    static final double RADIUS = 3.5;
    final UUID id;
    final Vector3d center;
    final int capacity;
    int spawned;
    int caught;
    float age;
    float unattended;

    SpecialFishingSpot(Vector3d center, int capacity) {
        this(UUID.randomUUID(), center, capacity);
    }

    private SpecialFishingSpot(UUID id, Vector3d center, int capacity) {
        this.id = id;
        this.center = new Vector3d(center);
        this.capacity = capacity;
    }

    boolean depleted() { return caught >= capacity; }

    boolean shouldExpire(float elapsed, boolean playerNearby, int livingFish, boolean waterPresent, double lifetime) {
        age += elapsed;
        unattended = playerNearby ? 0 : unattended + elapsed;
        return depleted() || age >= lifetime || unattended >= 45 || livingFish == 0 || !waterPresent;
    }

    SpecialFishingSpot copy() {
        SpecialFishingSpot copy = new SpecialFishingSpot(id, center, capacity);
        copy.spawned = spawned;
        copy.caught = caught;
        copy.age = age;
        copy.unattended = unattended;
        return copy;
    }
}
