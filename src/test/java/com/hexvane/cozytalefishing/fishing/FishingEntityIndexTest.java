package com.hexvane.cozytalefishing.fishing;

import static org.junit.jupiter.api.Assertions.*;

import com.hexvane.cozytalefishing.fish.FishShadowComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FishingEntityIndexTest {
    private static final UUID OWNER = UUID.randomUUID();

    /** Ticks one marker entity and hands the tick's store and buffer to the test. */
    private static final class Marker implements Component<EntityStore> {
        private static ComponentType<EntityStore, Marker> type;
        @Override public Marker clone() { return new Marker(); }
    }

    private static final class TickProbe extends EntityTickingSystem<EntityStore> {
        Consumer<CommandBuffer<EntityStore>> action = buffer -> {};

        @Override public Query<EntityStore> getQuery() { return Marker.type; }

        @Override
        public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
            action.accept(buffer);
        }
    }

    private ComponentRegistry<EntityStore> registry;
    private Store<EntityStore> store;
    private TickProbe probe;

    @BeforeEach
    void setup() {
        registry = new ComponentRegistry<>();
        var proxy = new ComponentRegistryProxy<EntityStore>(new ArrayList<>(), registry);
        FishingBobberComponent.register(proxy);
        FishShadowComponent.register(proxy);
        FishingEntityIndex.register(proxy);
        Marker.type = registry.registerComponent(Marker.class, Marker::new);
        registry.registerSystem(new FishingEntityIndex.BobberEntitySystem());
        registry.registerSystem(new FishingEntityIndex.BobberComponentSystem());
        registry.registerSystem(new FishingEntityIndex.ShadowEntitySystem());
        probe = new TickProbe();
        registry.registerSystem(probe);
        store = registry.addStore(null, EmptyResourceStorage.get());
        var marker = registry.newHolder();
        marker.addComponent(Marker.type, new Marker());
        store.addEntity(marker, AddReason.SPAWN);
    }

    @AfterEach
    void shutdown() { if (registry != null) registry.shutdown(); }

    private FishingEntityIndex index() { return FishingEntityIndex.get(store); }

    private Ref<EntityStore> emptyEntity() {
        var holder = registry.newHolder();
        holder.addComponent(registry.getNonSerializedComponentType(), NonSerialized.get());
        return store.addEntity(holder, AddReason.SPAWN);
    }

    private Holder<EntityStore> shadowHolder() {
        var holder = registry.newHolder();
        holder.addComponent(FishShadowComponent.getComponentType(), new FishShadowComponent());
        holder.ensureComponent(registry.getNonSerializedComponentType());
        return holder;
    }

    private static void scan(CommandBuffer<EntityStore> buffer) {
        buffer.getStore().forEachEntityParallel(FishingBobberComponent.getComponentType(), (index, chunk, ignored) -> {});
    }

    /**
     * One tick with a Store scan leaves two buffers pooled, oldest first: the scan's, then the tick's. Writing to the
     * tick's finished buffer puts the write where the next tick's scan, if it has one, will take it.
     */
    private void leaveWriteInPooledBuffer(Ref<EntityStore> target) {
        List<CommandBuffer<EntityStore>> finished = new ArrayList<>();
        probe.action = buffer -> {
            scan(buffer);
            finished.add(buffer);
        };
        store.tick(0.05f);
        finished.get(0).addComponent(target, FishingBobberComponent.getComponentType(), new FishingBobberComponent(OWNER));
    }

    /** The way a cast makes a bobber: the projectile exists first and gets its component later. */
    @Test
    void bobberIsFoundOnceACastGivesTheProjectileItsComponent() {
        Ref<EntityStore> projectile = emptyEntity();
        assertNull(FishingLineService.findBobberForOwner(store, OWNER));

        store.putComponent(projectile, FishingBobberComponent.getComponentType(), new FishingBobberComponent(OWNER));

        assertSame(projectile, FishingLineService.findBobberForOwner(store, OWNER));
        assertNull(FishingLineService.findBobberForOwner(store, UUID.randomUUID()));
    }

    @Test
    void bobberLeavesTheIndexWithItsEntity() {
        Ref<EntityStore> projectile = emptyEntity();
        store.putComponent(projectile, FishingBobberComponent.getComponentType(), new FishingBobberComponent(OWNER));

        store.removeEntity(projectile, RemoveReason.REMOVE);

        assertTrue(index().getBobbers().isEmpty());
        assertNull(FishingLineService.findBobberForOwner(store, OWNER));
    }

    @Test
    void bobberReloadedFromAChunkIsFound() {
        var holder = registry.newHolder();
        holder.addComponent(FishingBobberComponent.getComponentType(), new FishingBobberComponent(OWNER));

        Ref<EntityStore> loaded = store.addEntity(holder, AddReason.LOAD);

        assertSame(loaded, FishingLineService.findBobberForOwner(store, OWNER));
    }

    @Test
    void shadowsAreTrackedUntilRemoved() {
        Ref<EntityStore> shadow = store.addEntity(shadowHolder(), AddReason.SPAWN);
        assertTrue(index().getShadows().contains(shadow));

        store.removeEntity(shadow, RemoveReason.REMOVE);

        assertTrue(index().getShadows().isEmpty());
    }

    /** A shadow parked by a chunk move comes back with LOAD but must keep swimming. */
    @Test
    void nonSerializedShadowSurvivesAnUnloadAndLoadCycle() {
        Ref<EntityStore> shadow = store.addEntity(shadowHolder(), AddReason.SPAWN);

        Holder<EntityStore> parked = store.removeEntity(shadow, RemoveReason.UNLOAD);
        Ref<EntityStore> back = store.addEntity(parked, AddReason.LOAD);

        assertTrue(back.isValid());
        assertTrue(index().getShadows().contains(back));
    }

    /**
     * The crash: a write queued on a buffer already back in the pool. A Store scan during a tick takes that
     * buffer and runs the write while the store is processing. The index lookup takes no buffer, so the same write
     * simply runs later.
     */
    @Test
    void lookupsDuringATickSurviveAWriteLeftInAPooledBuffer() {
        Ref<EntityStore> target = emptyEntity();
        leaveWriteInPooledBuffer(target);

        probe.action = buffer -> FishingLineService.findBobberForOwner(buffer, OWNER);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 8; i++) store.tick(0.05f);
        });

        assertSame(target, FishingLineService.findBobberForOwner(store, OWNER));
    }

    /** Pins the diagnosis against the engine this mod compiles with: the old scan fails exactly as the crash did. */
    @Test
    void aStoreScanDuringATickDiesOnAWriteLeftInAPooledBuffer() {
        Ref<EntityStore> target = emptyEntity();
        leaveWriteInPooledBuffer(target);

        probe.action = FishingEntityIndexTest::scan;
        IllegalStateException crash = assertThrows(IllegalStateException.class, () -> store.tick(0.05f));
        assertTrue(crash.getMessage().startsWith("Store is currently processing!"), crash.getMessage());
    }
}
