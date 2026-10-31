package com.hexvane.cozytalefishing.fishing;

import com.hexvane.cozytalefishing.fish.FishShadowComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The world's bobbers and fish shadows, so tick systems can read them without iterating the Store. A Store iteration
 * takes a pooled CommandBuffer and consumes it on return; inside a tick, any write already queued in that buffer stops
 * the world with "Store is currently processing!".
 */
public final class FishingEntityIndex implements Resource<EntityStore> {
    private static ResourceType<EntityStore, FishingEntityIndex> resourceType;

    private final Set<Ref<EntityStore>> bobbers = new LinkedHashSet<>();
    private final Set<Ref<EntityStore>> shadows = new LinkedHashSet<>();

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        resourceType = registry.registerResource(FishingEntityIndex.class, FishingEntityIndex::new);
    }

    @Nonnull
    public static FishingEntityIndex get(@Nonnull ComponentAccessor<EntityStore> accessor) {
        return accessor.getResource(resourceType);
    }

    @Nonnull
    public Collection<Ref<EntityStore>> getBobbers() {
        return Collections.unmodifiableSet(bobbers);
    }

    @Nonnull
    public Collection<Ref<EntityStore>> getShadows() {
        return Collections.unmodifiableSet(shadows);
    }

    @Override
    public FishingEntityIndex clone() {
        FishingEntityIndex copy = new FishingEntityIndex();
        copy.bobbers.addAll(bobbers);
        copy.shadows.addAll(shadows);
        return copy;
    }

    /** Bobbers loaded with their world, and bobbers leaving it. */
    public static final class BobberEntitySystem extends RefSystem<EntityStore> {
        @Override
        public Query<EntityStore> getQuery() {
            return FishingBobberComponent.getComponentType();
        }

        @Override
        public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            get(store).bobbers.add(ref);
        }

        @Override
        public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            get(store).bobbers.remove(ref);
        }
    }

    /** A cast adds the bobber component to a projectile that already exists, which RefSystem does not report. */
    public static final class BobberComponentSystem extends RefChangeSystem<EntityStore, FishingBobberComponent> {
        @Nonnull
        @Override
        public ComponentType<EntityStore, FishingBobberComponent> componentType() {
            return FishingBobberComponent.getComponentType();
        }

        @Override
        public Query<EntityStore> getQuery() {
            return FishingBobberComponent.getComponentType();
        }

        @Override
        public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull FishingBobberComponent component,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            get(store).bobbers.add(ref);
        }

        @Override
        public void onComponentSet(@Nonnull Ref<EntityStore> ref, @Nullable FishingBobberComponent oldComponent,
            @Nonnull FishingBobberComponent newComponent, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {}

        @Override
        public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull FishingBobberComponent component,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            get(store).bobbers.remove(ref);
        }
    }

    public static final class ShadowEntitySystem extends RefSystem<EntityStore> {
        @Override
        public Query<EntityStore> getQuery() {
            return FishShadowComponent.getComponentType();
        }

        @Override
        public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            get(store).shadows.add(ref);
        }

        @Override
        public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            get(store).shadows.remove(ref);
        }
    }
}
