package com.hexvane.cozytalefishing.fishing;

import org.joml.Vector3f;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.EmptyResourceStorage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.ComponentRegistry;
import static org.junit.jupiter.api.Assertions.*;

import com.hexvane.cozytalefishing.fish.FishingModConfig;
import com.hypixel.hytale.builtin.beam.AttachedBeam;
import com.hypixel.hytale.codec.ExtraInfo;
import org.bson.BsonDocument;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class FishingBeamConfigurationTest {
    private FishingModConfig decode(String json) {
        return FishingModConfig.CODEC.decode(BsonDocument.parse(json), ExtraInfo.THREAD_LOCAL.get());
    }

    @Test
    void existingConfigsKeepOriginalRenderer() {
        var config = decode("{}");
        assertFalse(config.isFishingLineBeamEnabled());
        assertEquals("Rope", config.getFishingLineBeamAsset());
    }

    @Test
    void beamSettingsSurviveCodecRoundTrip() {
        var config = decode("{\"FishingLineBeamEnabled\":true,\"FishingLineBeamAsset\":\"Chain\"}");
        var encoded = FishingModConfig.CODEC.encode(config, ExtraInfo.THREAD_LOCAL.get());
        var restored = FishingModConfig.CODEC.decode(encoded, ExtraInfo.THREAD_LOCAL.get());
        assertTrue(restored.isFishingLineBeamEnabled());
        assertEquals("Chain", restored.getFishingLineBeamAsset());
    }

    @Test
    void blankAssetDoesNotSilentlyDisableBeamModeOrSelectDefaultAsset() {
        var config = decode("{\"FishingLineBeamEnabled\":true,\"FishingLineBeamAsset\":\"  \"}");
        assertTrue(config.isFishingLineBeamEnabled());
        assertEquals("", config.getFishingLineBeamAsset());
    }

    @Test
    void legacyTipSettingUsesPersistentHandAttachmentAndConfiguredOffset() {
        var config = decode("{\"FishingLineBeamWidth\":0.15,\"FishingLineBeamTargetNode\":\"Tip\"}");
        var registry = new ComponentRegistry<EntityStore>();
        try {
            var store = registry.addStore(null, EmptyResourceStorage.get());
            var player = store.addEntity(registry.newHolder(), AddReason.SPAWN);
            var configured = decode("{\"FishingLineBeamWidth\":0.15,\"FishingLineBeamTargetNode\":\"Tip\",\"FishingLineBeamOffsetX\":0.2,\"FishingLineBeamOffsetY\":1.5,\"FishingLineBeamOffsetZ\":-0.1}");
            var beam = FishingLineBeamRenderer.createBeam(1, player, configured);
            assertEquals("R-Attachment", beam.targetNode());
            assertNull(beam.targetPosition());
            assertEquals(new Vector3f(0.2f, 1.5f, -0.1f), beam.targetOffset());
            assertEquals(beam, FishingLineBeamRenderer.createBeam(1, player, configured),
                "Unchanged settings must not replace a persistent beam");
            assertEquals(0.15f, beam.sourceScale());
            assertEquals(0.15f, beam.targetScale());
            assertSame(player, beam.targetEntity());
        } finally {
            registry.shutdown();
        }
    }

    @Test
    void beamWidthDefaultsAndInvalidValuesAreSafe() {
        assertEquals(0.10f, decode("{}").getFishingLineBeamWidth());
        assertEquals(0.10f, decode("{\"FishingLineBeamWidth\":-1}").getFishingLineBeamWidth());
        assertEquals(2f, decode("{\"FishingLineBeamWidth\":100}").getFishingLineBeamWidth());
    }

    @Test
    void componentClonePreservesBeamOwnershipAndResetDropsIt() {
        var line = new FishingLineComponent();
        line.fishingBeam = AttachedBeam.toPosition(1, 0.025f, null, new Vector3d(1, 2, 3));
        line.missingBeamAsset = "Unknown";
        var copy = (FishingLineComponent) line.clone();
        assertSame(line.fishingBeam, copy.fishingBeam);
        assertEquals("Unknown", copy.missingBeamAsset);
        copy.reset();
        assertNull(copy.fishingBeam);
        assertNull(copy.fishingBeamOwner);
        assertNull(copy.missingBeamAsset);
        assertNotNull(line.fishingBeam);
    }
}
