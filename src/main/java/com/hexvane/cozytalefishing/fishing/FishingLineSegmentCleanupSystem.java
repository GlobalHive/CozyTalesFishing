package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Removes lost segments, including segments reloaded with a new Ref after crossing a chunk boundary. */
public final class FishingLineSegmentCleanupSystem extends EntityTickingSystem<EntityStore> {
    @Override
    public Query<EntityStore> getQuery() {
        return FishingLineSegmentOwner.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
        @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        FishingLineSegmentOwner ownership = chunk.getComponent(index, FishingLineSegmentOwner.getComponentType());
        Ref<EntityStore> segmentRef = chunk.getReferenceTo(index);
        Ref<EntityStore> ownerRef = ownership.getOwnerRef();
        FishingLineComponent line = ownerRef != null && ownerRef.isValid() && ownerRef.getStore() == store
            ? commandBuffer.getComponent(ownerRef, FishingLineComponent.getComponentType()) : null;
        int slot = ownership.getSegmentIndex();
        if (line == null || slot < 0 || slot >= line.getSegmentRefs().length
            || line.getSegmentRefs()[slot] != segmentRef) {
            commandBuffer.tryRemoveEntity(segmentRef, RemoveReason.REMOVE);
        }
    }
}
