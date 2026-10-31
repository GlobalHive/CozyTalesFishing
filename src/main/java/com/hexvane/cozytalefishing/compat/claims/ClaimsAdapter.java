package com.hexvane.cozytalefishing.compat.claims;

import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * What the mod needs to know from a land-claims mod: is a position claimed, does a player own that claim,
 * and does the claim let a player use containers there. {@link #NONE} answers "not claimed" everywhere,
 * which leaves aquariums open to everyone without a claims mod.
 */
public interface ClaimsAdapter {
    /** A claim or region found at a position; {@code kind} tells the adapter which owner lookup to use. */
    record Claim(@Nonnull String kind, @Nonnull String id) {}

    /** The claim covering the block, or null when the block is in no claim. */
    @Nullable
    Claim claimAt(@Nonnull String world, int x, int y, int z);

    /** True when {@code player} owns (or rents, or bought) {@code claim}. */
    boolean isOwner(@Nonnull Claim claim, @Nonnull UUID player);

    /** True when the claim's own rules (owner, member and group flags, defaults) let the player use containers there. */
    boolean allowsContainers(@Nonnull UUID player, @Nonnull String world, int x, int y, int z);

    /** Short name for logs. */
    @Nonnull
    default String name() {
        return "none";
    }

    ClaimsAdapter NONE = new ClaimsAdapter() {
        @Nullable @Override public Claim claimAt(@Nonnull String world, int x, int y, int z) { return null; }
        @Override public boolean isOwner(@Nonnull Claim claim, @Nonnull UUID player) { return false; }
        @Override public boolean allowsContainers(@Nonnull UUID player, @Nonnull String world, int x, int y, int z) { return false; }
    };
}
