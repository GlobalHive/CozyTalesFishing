package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hexvane.cozytalefishing.fish.FishingModConfig;
import com.hypixel.hytale.builtin.beam.AttachedBeam;
import com.hypixel.hytale.builtin.beam.BeamComponent;
import com.hypixel.hytale.builtin.beam.asset.Beam;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3f;

/** A bobber-owned beam dies with the bobber, including recall, disconnect and world transfer. */
final class FishingLineBeamRenderer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private FishingLineBeamRenderer() {}

    static void update(CommandBuffer<EntityStore> buffer, Ref<EntityStore> player, FishingLineComponent line) {
        String assetId = FishingModConfig.get().getFishingLineBeamAsset();
        int assetIndex = assetId.isEmpty() ? -1 : Beam.getAssetMap().getIndex(assetId);
        Ref<EntityStore> bobber = line.getBobberRef();
        if (assetIndex < 0 || Beam.getAssetMap().getAsset(assetId) == null) {
            remove(buffer, line);
            if (!assetId.equals(line.missingBeamAsset)) {
                LOGGER.atWarning().log("FishingLineBeamAsset '%s' was not found; beam mode will not render a line (no segment fallback).", assetId);
                line.missingBeamAsset = assetId;
            }
            return;
        }
        line.missingBeamAsset = null;
        if (bobber == null || !bobber.isValid()) {
            remove(buffer, line);
            return;
        }
        // The client follows both entities and the animated attachment. Only
        // configuration/owner changes replace the beam, not player movement.
        AttachedBeam desired = createBeam(assetIndex, player, FishingModConfig.get());
        BeamComponent current = buffer.getComponent(bobber, BeamComponent.getComponentType());
        if (bobber.equals(line.fishingBeamOwner) && desired.equals(line.fishingBeam)
            && current != null && current.getBeams().contains(line.fishingBeam)) {
            return;
        }
        boolean sameOwner = bobber.equals(line.fishingBeamOwner);
        AttachedBeam previous = sameOwner ? line.fishingBeam : null;
        if (!sameOwner) remove(buffer, line);
        // CommandBuffer reads do not include queued puts. Remove and add on ONE
        // copy, then queue ONE put; rereading after remove resurrects the old beam.
        buffer.putComponent(bobber, BeamComponent.getComponentType(), replaceBeam(current, previous, desired));
        line.fishingBeam = desired;
        line.fishingBeamOwner = bobber;
    }

    static void remove(CommandBuffer<EntityStore> buffer, FishingLineComponent line) {
        if (line.fishingBeam != null && line.fishingBeamOwner != null && line.fishingBeamOwner.isValid()) {
            BeamComponent current = buffer.getComponent(line.fishingBeamOwner, BeamComponent.getComponentType());
            if (current != null && current.getBeams().contains(line.fishingBeam)) {
                BeamComponent next = new BeamComponent(current);
                next.remove(line.fishingBeam);
                buffer.putComponent(line.fishingBeamOwner, BeamComponent.getComponentType(), next);
            }
        }
        line.fishingBeam = null;
        line.fishingBeamOwner = null;
    }

    static BeamComponent replaceBeam(BeamComponent current, AttachedBeam previous, AttachedBeam desired) {
        BeamComponent next = current == null ? new BeamComponent() : new BeamComponent(current);
        if (previous != null) next.remove(previous);
        next.add(desired);
        return next;
    }

    static AttachedBeam createBeam(int assetIndex, Ref<EntityStore> player, FishingModConfig config) {
        return AttachedBeam.toEntity(assetIndex, config.getFishingLineBeamWidth(), null,
            new Vector3f(), player, config.getFishingLineBeamTargetNode(),
            new Vector3f(config.getFishingLineBeamOffsetX(), config.getFishingLineBeamOffsetY(), config.getFishingLineBeamOffsetZ()));
    }
}
