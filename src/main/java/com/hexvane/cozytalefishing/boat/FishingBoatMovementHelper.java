package com.hexvane.cozytalefishing.boat;

import com.hexvane.cozytalefishing.fish.FishingModConfig;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementConfig;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Applies fishing boat mount movement settings, including config-driven speed scaling. */
public final class FishingBoatMovementHelper {
  private FishingBoatMovementHelper() {}

  public static void applyBoatMountMovement(
      @Nonnull MovementManager movementManager,
      @Nullable MovementConfig movementConfig,
      @Nonnull PhysicsValues physicsValues,
      @Nonnull GameMode gameMode,
      @Nonnull PacketHandler packetHandler
  ) {
    if (movementConfig == null) {
      return;
    }

    movementManager.setDefaultSettings(movementConfig, physicsValues, gameMode);
    MovementSettings settings = movementManager.getDefaultSettings();
    float multiplier = FishingModConfig.get().getBoatSpeedMultiplier();
    if (multiplier != 1.0f) {
      settings.baseSpeed *= multiplier;
      settings.acceleration = Math.min(1.0f, settings.acceleration * multiplier);
    }
    movementManager.applyDefaultSettings();
    movementManager.update(packetHandler);
  }
}
