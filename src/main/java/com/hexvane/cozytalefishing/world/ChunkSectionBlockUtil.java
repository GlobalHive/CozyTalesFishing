package com.hexvane.cozytalefishing.world;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkFlag;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Chunk column and section block access without deprecated {@link World} chunk helpers or
 * {@link WorldChunk#setBlock}.
 */
public final class ChunkSectionBlockUtil {
    private ChunkSectionBlockUtil() {}

    @Nullable
    public static Ref<ChunkStore> chunkRefIfInMemory(@Nonnull World world, long chunkIndex) {
        Ref<ChunkStore> ref = world.getChunkStore().getChunkReference(chunkIndex);
        if (ref == null || !ref.isValid()) {
            return null;
        }
        return ref;
    }

    @Nullable
    public static Ref<ChunkStore> chunkRefIfInMemory(@Nonnull World world, int blockX, int blockZ) {
        return chunkRefIfInMemory(world, ChunkUtil.indexChunkFromBlock(blockX, blockZ));
    }

    @Nullable
    public static WorldChunk worldChunkIfInMemory(@Nonnull World world, long chunkIndex) {
        Ref<ChunkStore> ref = chunkRefIfInMemory(world, chunkIndex);
        if (ref == null) {
            return null;
        }
        return world.getChunkStore().getStore().getComponent(ref, WorldChunk.getComponentType());
    }

    @Nullable
    public static WorldChunk worldChunkIfInMemory(@Nonnull World world, int blockX, int blockZ) {
        return worldChunkIfInMemory(world, ChunkUtil.indexChunkFromBlock(blockX, blockZ));
    }

    public static boolean isChunkInMemory(@Nonnull World world, int blockX, int blockZ) {
        return chunkRefIfInMemory(world, blockX, blockZ) != null;
    }

    @Nullable
    public static WorldChunk worldChunkIfTicking(@Nonnull World world, long chunkIndex) {
        WorldChunk chunk = worldChunkIfInMemory(world, chunkIndex);
        return chunk != null && chunk.is(ChunkFlag.TICKING) ? chunk : null;
    }

    @Nullable
    public static WorldChunk worldChunkIfTicking(@Nonnull World world, int blockX, int blockZ) {
        return worldChunkIfTicking(world, ChunkUtil.indexChunkFromBlock(blockX, blockZ));
    }

    @Nullable
    public static BlockChunk blockChunkAt(@Nonnull World world, int blockX, int blockZ) {
        Ref<ChunkStore> ref = chunkRefIfInMemory(world, blockX, blockZ);
        if (ref == null) {
            return null;
        }
        return world.getChunkStore().getStore().getComponent(ref, BlockChunk.getComponentType());
    }

    @Nullable
    public static BlockSection blockSectionAt(@Nonnull World world, int worldX, int worldY, int worldZ) {
        Ref<ChunkStore> sectionRef = sectionRefAt(world, worldX, worldY, worldZ);
        if (sectionRef == null) {
            return null;
        }
        return world.getChunkStore().getStore().getComponent(sectionRef, BlockSection.getComponentType());
    }

    @Nullable
    public static BlockComponentSection blockComponentSectionAt(
        @Nonnull World world,
        int worldX,
        int worldY,
        int worldZ
    ) {
        Ref<ChunkStore> sectionRef = sectionRefAt(world, worldX, worldY, worldZ);
        if (sectionRef == null) {
            return null;
        }
        return world.getChunkStore().getStore().getComponent(sectionRef, BlockComponentSection.getComponentType());
    }

    @Nullable
    public static Ref<ChunkStore> blockEntityRefAt(@Nonnull World world, int worldX, int worldY, int worldZ) {
        BlockComponentSection section = blockComponentSectionAt(world, worldX, worldY, worldZ);
        if (section == null) {
            return null;
        }
        int index = ChunkUtil.indexBlock(worldX, worldY, worldZ);
        return section.getBlockReference(index);
    }

    public static int blockId(@Nonnull World world, int x, int y, int z) {
        BlockChunk chunk = blockChunkAt(world, x, z);
        if (chunk == null || y < ChunkUtil.MIN_Y || y >= ChunkUtil.HEIGHT) {
            return BlockType.EMPTY_ID;
        }
        return chunk.getBlock(x, y, z);
    }

    @Nullable
    public static BlockType blockType(@Nonnull World world, int x, int y, int z) {
        return BlockType.getAssetMap().getAsset(blockId(world, x, y, z));
    }

    public static int rotationIndex(@Nonnull World world, int x, int y, int z) {
        BlockSection section = blockSectionAt(world, x, y, z);
        if (section == null) {
            return RotationTuple.NONE_INDEX;
        }
        return section.getRotationIndex(x, y, z);
    }

    public static int environmentAt(@Nonnull World world, int x, int y, int z, int missingValue) {
        BlockChunk chunk = blockChunkAt(world, x, z);
        if (chunk == null) {
            return missingValue;
        }
        return chunk.getEnvironment(x, y, z);
    }

    public static boolean setBlock(
        @Nonnull World world,
        int x,
        int y,
        int z,
        int blockId,
        @Nonnull BlockType blockType,
        int rotation,
        int filler,
        int settings
    ) {
        Ref<ChunkStore> sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null || !sectionRef.isValid()) {
            return false;
        }
        return BlockOperations.setBlock(
            world.getChunkStore(),
            sectionRef,
            x,
            y,
            z,
            blockId,
            blockType,
            rotation,
            filler,
            settings
        );
    }

    public static boolean setBlockEmpty(@Nonnull World world, int x, int y, int z, int settings) {
        return setBlock(
            world,
            x,
            y,
            z,
            BlockType.EMPTY_ID,
            BlockType.EMPTY,
            RotationTuple.NONE_INDEX,
            FillerBlockUtil.NO_FILLER,
            settings
        );
    }

    @Nullable
    public static Ref<ChunkStore> sectionRefAt(@Nonnull World world, int worldX, int worldY, int worldZ) {
        if (worldY < ChunkUtil.MIN_Y || worldY > ChunkUtil.HEIGHT_MINUS_1) {
            return null;
        }
        Ref<ChunkStore> sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(worldX, worldY, worldZ);
        if (sectionRef == null || !sectionRef.isValid()) {
            return null;
        }
        return sectionRef;
    }
}
