package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.fishing.FishingRodRegistry;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public final class FishShadowProximity {
    private FishShadowProximity() {}

    public static boolean isHoldingFishingRod(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> playerRef) {
        ItemStack held = InventoryComponent.getItemInHand(commandBuffer, playerRef);
        return held != null && !held.isEmpty() && FishingRodRegistry.isFishingRod(held.getItemId());
    }

    /** True when any player holding the fishing rod is within horizontal range of the position. */
    public static boolean hasRodHolderNear(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Vector3d position,
        float radiusBlocks
    ) {
        double radiusSq = radiusBlocks * radiusBlocks;
        for (PlayerRef player : commandBuffer.getExternalData().getWorld().getPlayerRefs()) {
            Ref<EntityStore> playerRef = player.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() != commandBuffer.getStore()) {
                continue;
            }
            TransformComponent transform = commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
            if (transform == null || !isHoldingFishingRod(commandBuffer, playerRef)) {
                continue;
            }
            Vector3d playerPos = transform.getPosition();
            double dx = playerPos.x - position.x;
            double dz = playerPos.z - position.z;
            if (dx * dx + dz * dz <= radiusSq) {
                return true;
            }
        }
        return false;
    }
}
