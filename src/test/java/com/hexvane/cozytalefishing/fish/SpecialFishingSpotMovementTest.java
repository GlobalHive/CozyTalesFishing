package com.hexvane.cozytalefishing.fish;

import static org.junit.jupiter.api.Assertions.*;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class SpecialFishingSpotMovementTest {
    private static final SpecialFishingSpotMovement.SurfaceMove OPEN_WATER = (position, dx, dz) -> {
        position.add(dx, 0, dz);
        return true;
    };

    @Test
    void escapedFishReturnFromEveryDirectionIncludingEastAndWest() {
        Vector3d center = new Vector3d(17, 115.1, -8);
        for (int degrees = 0; degrees < 360; degrees += 15) {
            double angle = Math.toRadians(degrees);
            Vector3d fish = new Vector3d(center).add(Math.sin(angle) * 20, 0, Math.cos(angle) * 20);
            float yaw = (float) angle;
            for (int i = 0; i < 190; i++) {
                double before = SpecialFishingSpotMovement.distanceSquared(fish, center);
                yaw = SpecialFishingSpotMovement.move(fish, center, yaw, 0.1, OPEN_WATER);
                if (before > 9) assertTrue(SpecialFishingSpotMovement.distanceSquared(fish, center) < before);
            }
            assertTrue(SpecialFishingSpotMovement.distanceSquared(fish, center) < 9);
        }
    }

    @Test
    void wanderingRemainsInsideSchoolThroughRepeatedTurnsAndLargeSteps() {
        Vector3d center = new Vector3d();
        for (double step : new double[] {0.01, 0.1, 0.6, 2}) {
            Vector3d fish = new Vector3d(3, 0, 0);
            for (int i = 0; i < 2000; i++) {
                SpecialFishingSpotMovement.move(fish, center, i * 0.37f, step, OPEN_WATER);
                assertTrue(SpecialFishingSpotMovement.distanceSquared(fish, center) <= 3.25 * 3.25);
            }
        }
    }

    @Test
    void turnsBeforeShoreAndContinuesSwimmingInsteadOfOscillating() {
        Vector3d fish = new Vector3d(1.8, 0, 0);
        SpecialFishingSpotMovement.SurfaceMove shore = (position, dx, dz) -> {
            if (position.x + dx >= 2) return false;
            position.add(dx, 0, dz);
            return true;
        };
        float yaw = (float) (Math.PI / 2);
        double travelled = 0;
        for (int i = 0; i < 200; i++) {
            Vector3d previous = new Vector3d(fish);
            yaw = SpecialFishingSpotMovement.move(fish, new Vector3d(), yaw, 0.05, shore);
            assertTrue(fish.x < 2);
            travelled += previous.distance(fish);
        }
        assertTrue(travelled > 9, "Fish should swim, not remain stuck at shore");
    }

    @Test
    void blockedFishHoldStillAndNeverJumpToAnotherWaterLevel() {
        Vector3d fish = new Vector3d(0, 10, 0);
        Vector3d center = new Vector3d(fish);
        for (int i = 0; i < 100; i++) {
            assertEquals(0.7f, SpecialFishingSpotMovement.move(fish, center, 0.7f, 0.1,
                (position, dx, dz) -> { position.add(dx, 1, dz); return true; }));
            assertEquals(center, fish);
        }
    }
}
