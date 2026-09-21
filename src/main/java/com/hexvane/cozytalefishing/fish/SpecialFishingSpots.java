package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Transient, world-owned state: unloading a world discards its schools and timers. */
public final class SpecialFishingSpots implements Resource<EntityStore> {
    private static ResourceType<EntityStore, SpecialFishingSpots> resourceType;
    final Map<UUID, SpecialFishingSpot> spots = new LinkedHashMap<>();
    float updateTimer;
    float spawnTimer;

    public static void register(ComponentRegistryProxy<EntityStore> registry) {
        resourceType = registry.registerResource(SpecialFishingSpots.class, SpecialFishingSpots::new);
    }

    public static ResourceType<EntityStore, SpecialFishingSpots> getResourceType() { return resourceType; }

    public static boolean canCatch(com.hypixel.hytale.component.ComponentAccessor<EntityStore> accessor, FishShadowComponent shadow) {
        return shadow.getSpecialSpotId() == null || accessor.getResource(resourceType).active(shadow.getSpecialSpotId()) != null;
    }

    SpecialFishingSpot active(UUID id) {
        SpecialFishingSpot spot = spots.get(id);
        return spot != null && !spot.depleted() ? spot : null;
    }

    void caught(FishShadowComponent shadow) {
        UUID id = shadow.getSpecialSpotId();
        SpecialFishingSpot spot = active(id);
        if (spot != null) spot.caught++;
        // Prevent duplicate accounting if the catch callback is visited again.
        shadow.setSpecialSpotId(null);
    }

    @Override
    public SpecialFishingSpots clone() {
        SpecialFishingSpots copy = new SpecialFishingSpots();
        spots.forEach((id, spot) -> copy.spots.put(id, spot.copy()));
        copy.updateTimer = updateTimer;
        copy.spawnTimer = spawnTimer;
        return copy;
    }
}
