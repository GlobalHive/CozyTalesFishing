package com.hexvane.cozytalefishing.fishing;

import static org.junit.jupiter.api.Assertions.*;

import com.hypixel.hytale.builtin.beam.AttachedBeam;
import com.hypixel.hytale.builtin.beam.BeamComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.concurrent.atomic.AtomicReference;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class FishingBeamUpdateTest {
    @Test
    void movingEndpointReplacesOneBeamThroughDeferredBufferAndPreservesOtherBeams() {
        var registry = new ComponentRegistry<EntityStore>();
        try {
            var type = registry.registerComponent(BeamComponent.class, BeamComponent::new);
            var store = registry.addStore(null, EmptyResourceStorage.get());
            var otherBeam = AttachedBeam.toPosition(2, 0.1f, null, new Vector3d(99, 0, 0));
            var holder = registry.newHolder();
            holder.addComponent(type, new BeamComponent(otherBeam));
            var bobber = store.addEntity(holder, AddReason.SPAWN);
            var previous = new AtomicReference<AttachedBeam>();
            for (int tick = 0; tick < 200; tick++) {
                var desired = AttachedBeam.toPosition(1, 0.1f, null, new Vector3d(tick, 2, 3));
                store.forEachChunk(type, (chunk, buffer) -> {
                    var current = buffer.getComponent(bobber, type);
                    buffer.putComponent(bobber, type,
                        FishingLineBeamRenderer.replaceBeam(current, previous.get(), desired));
                    previous.set(desired);
                });
                var beams = store.getComponent(bobber, type).getBeams();
                assertEquals(2, beams.size(), "Only our current line and the unrelated beam may exist");
                assertTrue(beams.contains(otherBeam));
                assertTrue(beams.contains(desired));
            }
        } finally {
            registry.shutdown();
        }
    }
}
