package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.fishing.FishingSplashEffects;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.joml.Vector3d;

/** Checks loaded water near all players, independently of their held item. */
public final class SpecialFishingSpotSystem extends TickingSystem<EntityStore> {
    @Override
    public void tick(float dt, int systemIndex, Store<EntityStore> store) {
        SpecialFishingSpots state = store.getResource(SpecialFishingSpots.getResourceType());
        state.updateTimer += dt;
        if (state.updateTimer < 1.0f) return;
        float elapsed = state.updateTimer;
        state.updateTimer = 0;
        FishingModConfig config = FishingModConfig.get();
        if (!config.isSpecialSpotsEnabled()) {
            state.spots.clear();
            return;
        }
        World world = store.getExternalData().getWorld();
        List<Vector3d> players = new ArrayList<>();
        store.forEachChunk(Query.and(Player.getComponentType(), TransformComponent.getComponentType()), (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                players.add(new Vector3d(chunk.getComponent(i, TransformComponent.getComponentType()).getPosition()));
            }
        });
        Map<UUID, Integer> living = new HashMap<>();
        store.forEachChunk(FishShadowComponent.getComponentType(), (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                UUID id = chunk.getComponent(i, FishShadowComponent.getComponentType()).getSpecialSpotId();
                if (id != null) living.merge(id, 1, Integer::sum);
            }
        });
        state.spots.values().removeIf(spot -> {
            boolean nearby = players.stream().anyMatch(p -> p.distanceSquared(spot.center) <= 80 * 80);
            return spot.shouldExpire(elapsed, nearby, living.getOrDefault(spot.id, 0),
                hasWater(world, spot.center), config.getSpecialSpotLifetimeSeconds());
        });
        for (SpecialFishingSpot spot : state.spots.values()) {
            // One finite area burst per update. Radius and density live in the particle asset.
            FishingSplashEffects.playSpecialSpotSparkles(store, new Vector3d(spot.center).add(0, 0.22, 0));
        }
        state.spawnTimer += elapsed;
        if (state.spawnTimer < config.getSpecialSpotCheckIntervalSeconds()) return;
        state.spawnTimer = 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        // Fixed world cap and separation prevent clustered players multiplying schools.
        for (Vector3d player : players) {
            if (state.spots.size() >= 8) break;
            if (state.spots.values().stream().anyMatch(s -> s.center.distanceSquared(player) < 64 * 64)) continue;
            if (random.nextDouble() >= config.getSpecialSpotSpawnChance()) continue;
            for (int attempt = 0; attempt < 12; attempt++) {
                double angle = random.nextDouble(Math.PI * 2);
                double distance = random.nextDouble(12, 40);
                int x = (int) Math.floor(player.x + Math.sin(angle) * distance);
                int z = (int) Math.floor(player.z + Math.cos(angle) * distance);
                var column = FishShadowSpawnHelper.findWaterColumnAt(world, x, z, 2, player.y, FishFluidHelper.ColumnFluid.WATER);
                if (column == null || Math.abs(column.spawnY() - player.y) > 24) continue;
                Vector3d center = new Vector3d(x + 0.5, column.spawnY(), z + 0.5);
                if (state.spots.values().stream().anyMatch(s -> s.center.distanceSquared(center) < 64 * 64)) continue;
                List<FishShadowSpawnHelper.WaterColumn> water = collectSchoolWater(world, center);
                if (water.size() < 16) continue;
                SpecialFishingSpot spot = new SpecialFishingSpot(center, random.nextInt(4, 7));
                // Commit only complete schools. Failed models/species cannot create half-empty spots.
                List<com.hypixel.hytale.component.Ref<EntityStore>> spawned = new ArrayList<>();
                for (int fish = 0; fish < spot.capacity; fish++) {
                    var fishColumn = water.remove(random.nextInt(water.size()));
                    var selection = FishShadowSpawner.pickSpecialSpotSpecies(world, fishColumn, config);
                    if (selection == null) break;
                    FishSpeciesAsset species = selection.species();
                    float[] scales = species.getShadowScaleRange();
                    var ref = FishShadowEntityPool.spawnShadow(store, species, selection.bodyType(), null,
                        new Vector3d(fishColumn.blockX() + 0.5, fishColumn.spawnY(), fishColumn.blockZ() + 0.5),
                        random.nextFloat() * (float) (Math.PI * 2), scales[0] + random.nextFloat() * (scales[1] - scales[0]));
                    if (ref == null) break;
                    store.getComponent(ref, FishShadowComponent.getComponentType()).setSpecialSpotId(spot.id);
                    spawned.add(ref);
                }
                if (spawned.size() == spot.capacity) {
                    spot.spawned = spawned.size();
                    state.spots.put(spot.id, spot);
                    break;
                }
                for (var ref : spawned) if (ref.isValid()) store.removeEntity(ref, com.hypixel.hytale.component.RemoveReason.REMOVE);
            }
        }
    }

    private static boolean hasWater(World world, Vector3d center) {
        var fluid = FishFluidHelper.fluidTypeAt(world, (int) Math.floor(center.x),
            (int) Math.floor(center.y - 1.0), (int) Math.floor(center.z));
        return fluid != null && fluid.sameAs(FishFluidHelper.ColumnFluid.WATER);
    }

    private static List<FishShadowSpawnHelper.WaterColumn> collectSchoolWater(World world, Vector3d center) {
        List<FishShadowSpawnHelper.WaterColumn> columns = new ArrayList<>();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz > 9) continue;
                var column = FishShadowSpawnHelper.findWaterColumnAt(world, (int) Math.floor(center.x) + dx,
                    (int) Math.floor(center.z) + dz, 2, center.y, FishFluidHelper.ColumnFluid.WATER);
                if (column != null && Math.abs(column.spawnY() - center.y) < 0.25) columns.add(column);
            }
        }
        return columns;
    }
}
