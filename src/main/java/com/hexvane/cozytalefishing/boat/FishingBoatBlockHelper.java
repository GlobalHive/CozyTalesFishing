package com.hexvane.cozytalefishing.boat;

import com.hexvane.cozytalefishing.world.ChunkSectionBlockUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import javax.annotation.Nonnull;
import org.joml.Vector3i;

/** Place/remove for parked fishing boat blocks above the water surface. */
public final class FishingBoatBlockHelper {
  private FishingBoatBlockHelper() {}

  public static boolean isPlacedBoatBlock(@Nonnull World world, @Nonnull Vector3i blockPos) {
    BlockType blockType = ChunkSectionBlockUtil.blockType(world, blockPos.x, blockPos.y, blockPos.z);
    return blockType != null && isPlacedBoatBlockId(blockType.getId());
  }

  public static boolean isPlacedBoatBlockId(@Nonnull String blockId) {
    return FishingBoatConstants.PLACED_BLOCK_ID.equals(blockId);
  }

  public static boolean canPlaceParkedBoatBlock(
      @Nonnull World world,
      int x,
      int y,
      int z,
      float yawRadians
  ) {
    int topWaterY = BoatWaterHelper.topWaterBlockY(world, x, z);
    if (topWaterY == Integer.MIN_VALUE || y != topWaterY + 1) {
      return false;
    }

    if (!ChunkSectionBlockUtil.isChunkInMemory(world, x, z)) {
      return false;
    }

    BoatWaterHelper.WaterPlacement placement =
        BoatWaterHelper.resolveWaterPlacement(world, x, topWaterY, z);
    if (!BoatWaterHelper.isValidPlacement(world, placement)) {
      return false;
    }

    BlockType blockType = BlockType.getAssetMap().getAsset(FishingBoatConstants.PLACED_BLOCK_ID);
    if (blockType == null) {
      return false;
    }

    BlockSection section = ChunkSectionBlockUtil.blockSectionAt(world, x, y, z);
    if (section == null) {
      return false;
    }

    int rotationIndex = rotationIndexFromYaw(yawRadians);
    return BlockOperations.testPlaceBlock(
        world.getChunkStore().getStore(),
        section,
        x,
        y,
        z,
        blockType,
        rotationIndex
    );
  }

  public static boolean placeParkedBoatBlock(
      @Nonnull World world,
      int x,
      int y,
      int z,
      float yawRadians
  ) {
    if (!canPlaceParkedBoatBlock(world, x, y, z, yawRadians)) {
      return false;
    }

    BlockType blockType = BlockType.getAssetMap().getAsset(FishingBoatConstants.PLACED_BLOCK_ID);
    if (blockType == null) {
      return false;
    }
    int index = BlockType.getAssetMap().getIndex(FishingBoatConstants.PLACED_BLOCK_ID);
    if (index < 0) {
      return false;
    }

    return ChunkSectionBlockUtil.setBlock(
        world,
        x,
        y,
        z,
        index,
        blockType,
        rotationIndexFromYaw(yawRadians),
        FillerBlockUtil.NO_FILLER,
        SetBlockSettings.PERFORM_BLOCK_UPDATE
    );
  }

  @Nonnull
  public static org.joml.Vector3d standPositionOnParkedBlock(int blockX, int parkedBlockY, int blockZ) {
    return new org.joml.Vector3d(blockX + 0.5, parkedBlockY + 1.0, blockZ + 0.5);
  }

  public static boolean removeBoatBlockKeepingWater(@Nonnull World world, int x, int y, int z) {
    int blockIndex = BlockType.getAssetMap().getIndex(FishingBoatConstants.PLACED_BLOCK_ID);
    if (blockIndex < 0 || ChunkSectionBlockUtil.blockId(world, x, y, z) != blockIndex) {
      return false;
    }

    return ChunkSectionBlockUtil.setBlockEmpty(world, x, y, z, SetBlockSettings.PERFORM_BLOCK_UPDATE);
  }

  public static float yawFromBlockRotation(@Nonnull World world, @Nonnull Vector3i blockPos) {
    int rotationIndex = ChunkSectionBlockUtil.rotationIndex(world, blockPos.x, blockPos.y, blockPos.z);
    return (float) RotationTuple.get(rotationIndex).yaw().getRadians();
  }

  public static int rotationIndexFromYaw(float yawRadians) {
    float degrees = (float) Math.toDegrees(yawRadians);
    degrees = (degrees % 360.0f + 360.0f) % 360.0f;
    Rotation rotation;
    if (degrees >= 45.0f && degrees < 135.0f) {
      rotation = Rotation.Ninety;
    } else if (degrees >= 135.0f && degrees < 225.0f) {
      rotation = Rotation.OneEighty;
    } else if (degrees >= 225.0f && degrees < 315.0f) {
      rotation = Rotation.TwoSeventy;
    } else {
      rotation = Rotation.None;
    }
    return RotationTuple.of(rotation, Rotation.None).index();
  }

  /** Block model forward is opposite the NPC entity model forward. */
  public static float entityYawToParkedBlockYaw(float entityYawRadians) {
    return entityYawRadians + (float) Math.PI;
  }

  /**
   * Searches outward from the origin for a valid parked boat placement.
   *
   * @return {@code [blockX, parkedY, blockZ]} or {@code null} if none found
   */
  @javax.annotation.Nullable
  public static int[] findNearbyParkedBoatPlacement(
      @Nonnull World world,
      int originX,
      int originZ,
      float yawRadians,
      int searchRadius
  ) {
    for (int radius = 0; radius <= searchRadius; radius++) {
      for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) {
          if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
            continue;
          }
          int candidateX = originX + dx;
          int candidateZ = originZ + dz;
          int parkedY = BoatWaterHelper.parkedBlockY(world, candidateX, candidateZ);
          if (parkedY == Integer.MIN_VALUE) {
            continue;
          }
          if (canPlaceParkedBoatBlock(world, candidateX, parkedY, candidateZ, yawRadians)) {
            return new int[] {candidateX, parkedY, candidateZ};
          }
        }
      }
    }
    return null;
  }
}
