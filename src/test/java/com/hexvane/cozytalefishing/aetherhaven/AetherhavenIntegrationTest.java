package com.hexvane.cozytalefishing.aetherhaven;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AetherhavenIntegrationTest {
    @Test
    void standaloneServerSkipsOptionalTreasureIntegration() {
        assertDoesNotThrow(AetherhavenIntegration::setup);
        assertFalse(AetherhavenIntegration.isEnabled());
        assertTrue(AetherhavenIntegration.rollSunkenTreasureBonus().isEmpty());
    }
}
