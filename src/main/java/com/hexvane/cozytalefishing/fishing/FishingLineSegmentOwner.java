package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/** Runtime ownership for a temporary rope segment; deliberately has no persistence codec. */
public final class FishingLineSegmentOwner implements Component<EntityStore> {
    private static ComponentType<EntityStore, FishingLineSegmentOwner> componentType;
    private final Ref<EntityStore> ownerRef;
    private final int segmentIndex;

    public FishingLineSegmentOwner() {
        this(null, -1);
    }

    public FishingLineSegmentOwner(Ref<EntityStore> ownerRef, int segmentIndex) {
        this.ownerRef = ownerRef;
        this.segmentIndex = segmentIndex;
    }

    public static void register(ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(FishingLineSegmentOwner.class, FishingLineSegmentOwner::new);
    }

    public static ComponentType<EntityStore, FishingLineSegmentOwner> getComponentType() {
        return componentType;
    }

    public Ref<EntityStore> getOwnerRef() { return ownerRef; }
    public int getSegmentIndex() { return segmentIndex; }

    @Override
    public FishingLineSegmentOwner clone() {
        return new FishingLineSegmentOwner(ownerRef, segmentIndex);
    }
}
