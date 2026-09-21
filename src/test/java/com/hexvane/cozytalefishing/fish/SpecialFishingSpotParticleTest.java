package com.hexvane.cozytalefishing.fish;

import static org.junit.jupiter.api.Assertions.*;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleAnimationFrame;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

class SpecialFishingSpotParticleTest {
    @Test
    void sparkleAnimationHasVisibleFramesAfterMultiplyingInitialOpacity() throws Exception {
        var asset = BsonDocument.parse(Files.readString(Path.of(
            "src/main/resources/Server/Particles/CozyTalesFishing/CozyFishing_SpecialSpot_Sparkles.particlespawner")));
        var particle = asset.getDocument("Particle");
        var initial = ParticleAnimationFrame.CODEC.decode(particle.getDocument("InitialAnimationFrame"), new ExtraInfo()).toPacket();
        boolean visible = false;
        for (var entry : particle.getDocument("Animation").entrySet()) {
            var frame = ParticleAnimationFrame.CODEC.decode(entry.getValue().asDocument(), new ExtraInfo()).toPacket();
            // Catch fully invisible effects without imposing a minimum artistic opacity.
            if (frame.opacity > 0 && initial.opacity * frame.opacity > 0) visible = true;
        }
        assertTrue(visible, "Animation opacity multiplies initial opacity; a zero base makes every frame invisible");
        assertTrue(asset.getDocument("ParticleLifeSpan").getNumber("Min").doubleValue() > 1,
            "Bursts must overlap the one second spot update to avoid gaps");
        assertTrue(asset.getDocument("TotalParticles").getNumber("Max").intValue() > 0,
            "Finite bursts stop when their owning spot expires");
    }
}
