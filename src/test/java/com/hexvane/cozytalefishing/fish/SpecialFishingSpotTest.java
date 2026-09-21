package com.hexvane.cozytalefishing.fish;

import static org.junit.jupiter.api.Assertions.*;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class SpecialFishingSpotTest {
    @Test
    void catchesShareOneFiniteSupplyAndDoNotCountTwice() {
        SpecialFishingSpots state = new SpecialFishingSpots();
        SpecialFishingSpot spot = new SpecialFishingSpot(new Vector3d(), 4);
        spot.spawned = 4;
        state.spots.put(spot.id, spot);
        for (int i = 0; i < 4; i++) {
            FishShadowComponent fish = new FishShadowComponent();
            fish.setSpecialSpotId(spot.id);
            state.caught(fish);
            state.caught(fish);
            assertEquals(i + 1, spot.caught);
            assertEquals(4, spot.spawned, "Catching cannot replenish the school");
        }
        assertNull(state.active(spot.id));
    }

    @Test
    void abandonmentGraceResetsForAnyReturningPlayer() {
        SpecialFishingSpot spot = new SpecialFishingSpot(new Vector3d(), 6);
        assertFalse(spot.shouldExpire(44, false, 6, true, 600));
        assertFalse(spot.shouldExpire(1, true, 6, true, 600));
        assertFalse(spot.shouldExpire(44, false, 6, true, 600));
        assertTrue(spot.shouldExpire(1, false, 6, true, 600));
    }

    @Test
    void expiryHandlesLostFishDrainedWaterAndLifetime() {
        assertTrue(new SpecialFishingSpot(new Vector3d(), 4).shouldExpire(1, true, 0, true, 600));
        assertTrue(new SpecialFishingSpot(new Vector3d(), 4).shouldExpire(1, true, 4, false, 600));
        assertTrue(new SpecialFishingSpot(new Vector3d(), 4).shouldExpire(600, true, 4, true, 600));
    }

    @Test
    void worldResourcesAreIndependentAndExpiredFishCannotBeCaught() {
        ComponentRegistry<EntityStore> registry = new ComponentRegistry<>();
        try {
            SpecialFishingSpots.register(new ComponentRegistryProxy<>(new ArrayList<>(), registry));
            Store<EntityStore> first = registry.addStore(null, EmptyResourceStorage.get());
            Store<EntityStore> second = registry.addStore(null, EmptyResourceStorage.get());
            SpecialFishingSpots state = first.getResource(SpecialFishingSpots.getResourceType());
            SpecialFishingSpot spot = new SpecialFishingSpot(new Vector3d(), 5);
            state.spots.put(spot.id, spot);
            FishShadowComponent fish = new FishShadowComponent();
            fish.setSpecialSpotId(spot.id);
            assertTrue(SpecialFishingSpots.canCatch(first, fish));
            assertFalse(SpecialFishingSpots.canCatch(second, fish));
            state.spots.clear();
            assertFalse(SpecialFishingSpots.canCatch(first, fish));
            fish.setSpecialSpotId(null);
            assertTrue(SpecialFishingSpots.canCatch(first, fish), "Normal fish remain catchable");
        } finally {
            registry.shutdown();
        }
    }

    @Test
    void clonesKeepOwnershipButDoNotShareMutableSpotState() {
        SpecialFishingSpots state = new SpecialFishingSpots();
        SpecialFishingSpot spot = new SpecialFishingSpot(new Vector3d(1, 2, 3), 4);
        state.spots.put(spot.id, spot);
        SpecialFishingSpots copy = state.clone();
        FishShadowComponent fish = new FishShadowComponent();
        fish.setSpecialSpotId(spot.id);
        FishShadowComponent clonedFish = (FishShadowComponent) fish.clone();
        assertEquals(spot.id, clonedFish.getSpecialSpotId());
        copy.caught(clonedFish);
        copy.spots.get(spot.id).center.x = 50;
        assertEquals(0, spot.caught);
        assertEquals(1, spot.center.x);
        assertEquals(spot.id, fish.getSpecialSpotId());
    }

    @Test
    void rareFishGainRelativeProbabilityAtEveryRarityStep() {
        float previous = 0;
        for (FishRarity rarity : FishRarity.values()) {
            float boost = FishShadowSpawner.specialSpotRarityBoost(rarity);
            assertTrue(boost > previous);
            previous = boost;
        }
    }
}
