package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Cleans up while the owner's component is still available, including disconnects and world transfers. */
public final class FishingLineRemovalSystem extends RefSystem<EntityStore> {
    @Override
    public Query<EntityStore> getQuery() {
        return FishingLineComponent.getComponentType();
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
        @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {}

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
        @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        FishingLineComponent line = commandBuffer.getComponent(ref, FishingLineComponent.getComponentType());
        if (line == null) return;
        SegmentEntityPool.despawnAll(commandBuffer, line);
        if (line.getBobberRef() != null) {
            commandBuffer.tryRemoveEntity(line.getBobberRef(), RemoveReason.REMOVE);
        }
        if (line.getHookedShadowRef() != null) {
            commandBuffer.tryRemoveEntity(line.getHookedShadowRef(), RemoveReason.REMOVE);
        }
        line.reset();
    }
}
