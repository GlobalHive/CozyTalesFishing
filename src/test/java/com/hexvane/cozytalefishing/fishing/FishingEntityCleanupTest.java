package com.hexvane.cozytalefishing.fishing;

import static org.junit.jupiter.api.Assertions.*;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FishingEntityCleanupTest {
    private ComponentRegistry<EntityStore> registry;
    private Store<EntityStore> store;
    private Ref<EntityStore> owner;
    private FishingLineComponent line;

    @BeforeEach
    void setup() {
        registry = new ComponentRegistry<>();
        var proxy = new ComponentRegistryProxy<EntityStore>(new ArrayList<>(), registry);
        FishingLineComponent.register(proxy);
        FishingLineSegmentOwner.register(proxy);
        registry.registerSystem(new FishingLineRemovalSystem());
        registry.registerSystem(new FishingLineSegmentCleanupSystem());
        store = registry.addStore(null, EmptyResourceStorage.get());
        line = new FishingLineComponent();
        var holder = registry.newHolder();
        holder.addComponent(FishingLineComponent.getComponentType(), line);
        owner = store.addEntity(holder, AddReason.SPAWN);
    }

    @AfterEach
    void shutdown() { if (registry != null) registry.shutdown(); }

    private Holder<EntityStore> segmentHolder(int slot) {
        var holder = registry.newHolder();
        holder.addComponent(FishingLineSegmentOwner.getComponentType(), new FishingLineSegmentOwner(owner, slot));
        return holder;
    }

    private Ref<EntityStore> addSegment(int slot) {
        var ref = store.addEntity(segmentHolder(slot), AddReason.SPAWN);
        line.getSegmentRefs()[slot] = ref;
        return ref;
    }

    @Test
    void removalWaitsForQueuedSpawnAndToleratesDuplicateCleanup() {
        var spawned = new AtomicReference<Ref<EntityStore>>();
        store.forEachChunk(FishingLineComponent.getComponentType(), (chunk, buffer) -> {
            var pending = buffer.addEntity(segmentHolder(0), AddReason.SPAWN);
            spawned.set(pending);
            assertFalse(pending.isValid(), "Engine refs are invalid until the queued spawn executes");
            SegmentEntityPool.despawnSegment(buffer, pending);
            SegmentEntityPool.despawnSegment(buffer, pending);
        });
        assertFalse(spawned.get().isValid());
        assertEquals(1, store.getEntityCount(), "Only the owner should remain");
    }

    @Test
    void lostReferenceIsRemovedButTrackedNeighborSurvives() {
        var lost = addSegment(0);
        var kept = addSegment(1);
        line.getSegmentRefs()[0] = null;
        store.tick(0.05f);
        assertFalse(lost.isValid());
        assertTrue(kept.isValid());
    }

    @Test
    void resetCannotLeaveLiveRopeBehind() {
        var refs = new ArrayList<Ref<EntityStore>>();
        for (int i = 0; i < line.getSegmentRefs().length; i++) refs.add(addSegment(i));
        line.reset();
        store.tick(0.05f);
        assertTrue(refs.stream().noneMatch(Ref::isValid));
        assertEquals(1, store.getEntityCount());
    }

    @Test
    void segmentReloadedWithNewReferenceIsRemovedWithoutDeletingReplacement() {
        var old = addSegment(0);
        var unloaded = store.removeEntity(old, RemoveReason.UNLOAD);
        var replacement = addSegment(0);
        var reloaded = store.addEntity(unloaded, AddReason.LOAD);
        store.tick(0.05f);
        assertFalse(reloaded.isValid());
        assertTrue(replacement.isValid());
    }

    @ParameterizedTest
    @EnumSource(value = RemoveReason.class, names = {"REMOVE", "UNLOAD"})
    void removingOwnerCleansRopeBobberAndShadow(RemoveReason reason) {
        var segment = addSegment(0);
        var bobber = store.addEntity(segmentHolder(-1), AddReason.SPAWN);
        var shadow = store.addEntity(segmentHolder(-1), AddReason.SPAWN);
        line.setBobberRef(bobber);
        line.setHookedShadowRef(shadow);
        store.removeEntity(owner, reason);
        assertFalse(segment.isValid());
        assertFalse(bobber.isValid());
        assertFalse(shadow.isValid());
        assertEquals(0, store.getEntityCount());
    }

    @Test
    void queuedCleanupOfAlreadyRemovedSegmentDoesNotAbortLaterRemovals() {
        var first = addSegment(0);
        var last = addSegment(1);
        store.removeEntity(first, RemoveReason.REMOVE);
        store.forEachChunk(FishingLineComponent.getComponentType(), (chunk, buffer) -> {
            SegmentEntityPool.despawnAll(buffer, line);
        });
        assertFalse(last.isValid());
        assertEquals(1, store.getEntityCount());
    }

    @Test
    void invalidOwnershipCannotLeakOrDeleteAnotherPlayersLine() {
        var otherLine = new FishingLineComponent();
        var otherHolder = registry.newHolder();
        otherHolder.addComponent(FishingLineComponent.getComponentType(), otherLine);
        var otherOwner = store.addEntity(otherHolder, AddReason.SPAWN);
        var keptHolder = registry.newHolder();
        keptHolder.addComponent(FishingLineSegmentOwner.getComponentType(), new FishingLineSegmentOwner(otherOwner, 0));
        var kept = store.addEntity(keptHolder, AddReason.SPAWN);
        otherLine.getSegmentRefs()[0] = kept;
        var malformed = new ArrayList<Ref<EntityStore>>();
        for (int slot : new int[]{-1, FishingConstants.SEGMENT_COUNT, Integer.MAX_VALUE}) {
            malformed.add(store.addEntity(segmentHolder(slot), AddReason.SPAWN));
        }
        var missingOwner = registry.newHolder();
        missingOwner.addComponent(FishingLineSegmentOwner.getComponentType(), new FishingLineSegmentOwner());
        malformed.add(store.addEntity(missingOwner, AddReason.SPAWN));
        store.tick(0.05f);
        assertTrue(malformed.stream().noneMatch(Ref::isValid));
        assertTrue(kept.isValid());
        store.removeEntity(owner, RemoveReason.REMOVE);
        store.tick(0.05f);
        assertTrue(otherOwner.isValid());
        assertTrue(kept.isValid());
    }

    @Test
    void randomizedMultiPlayerCastCleanupLeavesNoSegmentsAcrossFiveHundredCycles() {
        var random = new Random(0xF15);
        long spawned = 0;
        // Each cycle creates four independent players/casts, with mixed rope lengths,
        // removed segments, lost references, and unload/reload with replacement entities.
        store.removeEntity(owner, RemoveReason.REMOVE);
        for (int cycle = 0; cycle < 500; cycle++) {
            var owners = new ArrayList<Ref<EntityStore>>();
            var lines = new ArrayList<FishingLineComponent>();
            var allSegments = new ArrayList<Ref<EntityStore>>();
            for (int player = 0; player < 4; player++) {
                line = new FishingLineComponent();
                var holder = registry.newHolder();
                holder.addComponent(FishingLineComponent.getComponentType(), line);
                owner = store.addEntity(holder, AddReason.SPAWN);
                owners.add(owner);
                lines.add(line);
                int count = random.nextInt(1, FishingConstants.SEGMENT_COUNT + 1);
                for (int slot = 0; slot < count; slot++) {
                    var segment = addSegment(slot);
                    allSegments.add(segment);
                    spawned++;
                    switch (random.nextInt(8)) {
                        case 0 -> {
                            var unloaded = store.removeEntity(segment, RemoveReason.UNLOAD);
                            allSegments.add(store.addEntity(unloaded, AddReason.LOAD));
                            allSegments.add(addSegment(slot));
                            spawned += 2;
                        }
                        case 1 -> store.removeEntity(segment, RemoveReason.REMOVE);
                        case 2 -> line.getSegmentRefs()[slot] = null;
                        default -> { }
                    }
                }
            }
            store.tick(0.05f);
            for (var activeLine : lines) {
                for (var ref : activeLine.getSegmentRefs()) {
                    if (ref != null && ref.isValid()) {
                        assertSame(activeLine, store.getComponent(
                            store.getComponent(ref, FishingLineSegmentOwner.getComponentType()).getOwnerRef(),
                            FishingLineComponent.getComponentType()));
                    }
                }
            }
            // Mix recall/reset, duplicate queued cleanup, and disconnect/world transfer.
            lines.get(0).reset();
            store.forEachChunk(FishingLineComponent.getComponentType(), (chunk, buffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    if (chunk.getReferenceTo(i) == owners.get(1)) {
                        SegmentEntityPool.despawnAll(buffer, lines.get(1));
                        SegmentEntityPool.despawnAll(buffer, lines.get(1));
                        lines.get(1).reset();
                    }
                }
            });
            store.removeEntity(owners.get(2), RemoveReason.REMOVE);
            store.removeEntity(owners.get(3), RemoveReason.UNLOAD);
            store.tick(0.05f);
            assertTrue(allSegments.stream().noneMatch(Ref::isValid), "cycle " + cycle);
            assertEquals(2, store.getEntityCount(), "Only the two connected owners should remain");
            store.removeEntity(owners.get(0), RemoveReason.REMOVE);
            store.removeEntity(owners.get(1), RemoveReason.REMOVE);
            assertEquals(0, store.getEntityCount(), "cycle " + cycle);
        }
        System.out.println("Fishing stress: 2,000 casts across 500 cycles; " + spawned + " segment spawns/loads, zero leftovers.");
    }
}
