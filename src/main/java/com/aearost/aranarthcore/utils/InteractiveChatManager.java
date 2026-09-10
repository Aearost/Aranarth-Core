package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages short-lived inventory snapshots that are embedded as clickable components in chat.
 */
public class InteractiveChatManager {

    public enum SnapshotType { ITEM, INV, EC }

    public static class Snapshot {
        private final UUID id;
        private final UUID ownerId;
        private final String ownerDisplayName;
        private final SnapshotType type;
        private final ItemStack[] items;
        private final long createdAt;

        public Snapshot(UUID id, UUID ownerId, String ownerDisplayName, SnapshotType type, ItemStack[] items) {
            this.id = id;
            this.ownerId = ownerId;
            this.ownerDisplayName = ownerDisplayName;
            this.type = type;
            this.items = items;
            this.createdAt = System.currentTimeMillis();
        }

        public UUID getId() { return id; }
        public UUID getOwnerId() { return ownerId; }
        public String getOwnerDisplayName() { return ownerDisplayName; }
        public SnapshotType getType() { return type; }
        public ItemStack[] getItems() { return items; }
        public long getCreatedAt() { return createdAt; }
    }

    private static final long EXPIRY_MS = 10 * 60 * 1000L;
    private static final ConcurrentHashMap<UUID, Snapshot> snapshots = new ConcurrentHashMap<>();

    /**
     * Stores a snapshot and returns its UUID.
     */
    public static UUID storeSnapshot(UUID ownerId, String ownerDisplayName, SnapshotType type, ItemStack[] items) {
        UUID id = UUID.randomUUID();
        snapshots.put(id, new Snapshot(id, ownerId, ownerDisplayName, type, items));
        return id;
    }

    /**
     * Stores a snapshot using a pre-determined UUID (used when replicating a snapshot from another server).
     */
    public static void storeSnapshotWithId(UUID id, UUID ownerId, String ownerDisplayName, SnapshotType type, ItemStack[] items) {
        snapshots.put(id, new Snapshot(id, ownerId, ownerDisplayName, type, items));
    }

    /**
     * Returns the snapshot for the given ID, or null if it has expired or never existed.
     */
    public static Snapshot getSnapshot(UUID id) {
        Snapshot snap = snapshots.get(id);
        if (snap == null) return null;
        if (System.currentTimeMillis() - snap.getCreatedAt() > EXPIRY_MS) {
            snapshots.remove(id);
            return null;
        }
        return snap;
    }

    /**
     * Removes all snapshots that are older than the expiry period.
     */
    public static void purgeExpired() {
        long now = System.currentTimeMillis();
        snapshots.entrySet().removeIf(uuidSnapshotEntry -> now - uuidSnapshotEntry.getValue().getCreatedAt() > EXPIRY_MS);
    }

    /**
     * Returns true if the player has the Saint II/III or council rank required for interactive chat.
     */
    public static boolean hasInteractiveChatPerm(AranarthPlayer ap) {
        return ap.getSaintRank() >= 2 || ap.getCouncilRank() > 0;
    }
}
