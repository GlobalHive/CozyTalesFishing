package com.hexvane.cozytalefishing.compat.claims;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hexvane.cozytalefishing.compat.claims.fake.FakeClaimsWorld;
import com.hexvane.cozytalefishing.compat.claims.fake.FakeClaimsWorld.RegionFlags;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ClaimsTest {
    private static final UUID OWNER = FakeClaimsWorld.OWNER;
    private static final UUID MEMBER = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();
    private static final String PLUGIN = FakeClaimsWorld.Plugin.class.getName();
    private static final String PREFIX = FakeClaimsWorld.class.getName() + "$";

    /** A claims mod in a map: one claim at x 0..15, container access per player. */
    private record Fake(UUID owner, Set<UUID> containerUsers) implements ClaimsAdapter {
        @Override public Claim claimAt(String world, int x, int y, int z) {
            return x >= 0 && x < 16 ? new Claim("claim", "1") : null;
        }
        @Override public boolean isOwner(Claim claim, UUID player) { return owner.equals(player); }
        @Override public boolean allowsContainers(UUID player, String world, int x, int y, int z) {
            return containerUsers.contains(player);
        }
    }

    private static final Fake CLAIMS = new Fake(OWNER, Set.of(MEMBER));

    private static ClaimsAdapter bindFake(Object plugin) {
        return QuestLinesClaimsAdapter.bind(List.of(plugin), PLUGIN, PREFIX, FakeClaimsWorld.Player.class,
            id -> id.equals(STRANGER) ? null : new FakeClaimsWorld.Player(id));
    }

    @AfterEach
    void reset() {
        Claims.installPluginsForTests(null);
        Claims.installForTests(null);
        FakeClaimsWorld.memberFlags.clear();
    }

    @Test
    void ownerAndContainerMembersMayUseAquariumsInTheClaim() {
        assertTrue(Claims.mayUseAquarium(CLAIMS, OWNER, "w", 3, 64, 3));
        assertTrue(Claims.mayUseAquarium(CLAIMS, MEMBER, "w", 3, 64, 3));
    }

    @Test
    void strangersMayNotUseAquariumsInTheClaim() {
        assertFalse(Claims.mayUseAquarium(CLAIMS, STRANGER, "w", 3, 64, 3));
    }

    @Test
    void everyoneMayUseAquariumsOutsideClaims() {
        assertTrue(Claims.mayUseAquarium(CLAIMS, STRANGER, "w", 100, 64, 3));
        assertTrue(Claims.mayUseAquarium(ClaimsAdapter.NONE, STRANGER, "w", 3, 64, 3));
    }

    @Test
    void absentClaimsModMeansNoClaims() {
        Claims.installPluginsForTests(() -> List.of(new Object()));
        assertSame(ClaimsAdapter.NONE, Claims.adapter());
        assertTrue(Claims.mayUseAquarium(STRANGER, "w", 3, 64, 3));
    }

    @Test
    void bindFailsSoftWhenThePluginIsAbsent() {
        assertNull(QuestLinesClaimsAdapter.bind(List.of(new Object()), PLUGIN, PREFIX, FakeClaimsWorld.Player.class, id -> null));
    }

    @Test
    void bindRetriesWhileTheApiIsNotReady() {
        assertNull(bindFake(new FakeClaimsWorld.Plugin(null)));
    }

    @Test
    void bindGivesUpQuietlyOnAnUnknownApiShape() {
        assertSame(ClaimsAdapter.NONE, bindFake(new FakeClaimsWorld.Plugin(new FakeClaimsWorld.OldApi())));
    }

    @Test
    void reflectionBindingAnswersThroughTheApi() {
        ClaimsAdapter adapter = bindFake(new FakeClaimsWorld.Plugin(new FakeClaimsWorld.Api()));
        assertNotNull(adapter);
        assertEquals("QuestLinesClaims", adapter.name());
        FakeClaimsWorld.memberFlags.put(MEMBER, EnumSet.of(RegionFlags.CONTAINER));

        assertTrue(Claims.mayUseAquarium(adapter, OWNER, "w", 3, 64, 3));
        assertTrue(Claims.mayUseAquarium(adapter, MEMBER, "w", 3, 64, 3));
        assertTrue(Claims.mayUseAquarium(adapter, STRANGER, "w", 100, 64, 3));
    }

    @Test
    void membersWithoutTheContainerFlagAreRefused() {
        ClaimsAdapter adapter = bindFake(new FakeClaimsWorld.Plugin(new FakeClaimsWorld.Api()));
        assertNotNull(adapter);
        FakeClaimsWorld.memberFlags.put(MEMBER, EnumSet.of(RegionFlags.INTERACT, RegionFlags.DOOR));
        assertFalse(Claims.mayUseAquarium(adapter, MEMBER, "w", 3, 64, 3));
    }

    @Test
    void offlinePlayersAreNeverAllowedByFlag() {
        ClaimsAdapter adapter = bindFake(new FakeClaimsWorld.Plugin(new FakeClaimsWorld.Api()));
        assertNotNull(adapter);
        FakeClaimsWorld.memberFlags.put(STRANGER, EnumSet.of(RegionFlags.CONTAINER));
        assertFalse(Claims.mayUseAquarium(adapter, STRANGER, "w", 3, 64, 3));
    }
}
