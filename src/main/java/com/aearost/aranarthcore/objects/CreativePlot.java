package com.aearost.aranarthcore.objects;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A claimed plot in the Creative plots world, identified by its grid coordinates.
 */
public class CreativePlot {

    private final int gridX;
    private final int gridZ;
    private UUID owner;
    private String name;
    private final Set<UUID> members;
    private boolean mergedEast;
    private boolean mergedSouth;
    private boolean spawn;
    private long claimedAt;

    public CreativePlot(int gridX, int gridZ, UUID owner, String name, Set<UUID> members,
                        boolean mergedEast, boolean mergedSouth, boolean spawn, long claimedAt) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.owner = owner;
        this.name = name;
        this.members = members == null ? new HashSet<>() : new HashSet<>(members);
        this.mergedEast = mergedEast;
        this.mergedSouth = mergedSouth;
        this.spawn = spawn;
        this.claimedAt = claimedAt;
    }

    /**
     * Provides the plot's X coordinate on the plot grid.
     * @return The plot's X coordinate on the plot grid.
     */
    public int getGridX() {
        return gridX;
    }

    /**
     * Provides the plot's Z coordinate on the plot grid.
     * @return The plot's Z coordinate on the plot grid.
     */
    public int getGridZ() {
        return gridZ;
    }

    /**
     * Provides the unique key of the plot, in the format "x;z".
     * @return The unique key of the plot.
     */
    public String getKey() {
        return gridX + ";" + gridZ;
    }

    /**
     * Provides the owner of the plot. Spawn plots have no owner.
     * @return The owner of the plot, or null if it is a spawn plot.
     */
    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Provides the players that were added to the plot and are able to build in it.
     * @return The players that were added to the plot.
     */
    public Set<UUID> getMembers() {
        return members;
    }

    /**
     * Provides whether the plot is merged with the plot directly east of it.
     * @return Whether the plot is merged with the plot directly east of it.
     */
    public boolean isMergedEast() {
        return mergedEast;
    }

    public void setMergedEast(boolean mergedEast) {
        this.mergedEast = mergedEast;
    }

    /**
     * Provides whether the plot is merged with the plot directly south of it.
     * @return Whether the plot is merged with the plot directly south of it.
     */
    public boolean isMergedSouth() {
        return mergedSouth;
    }

    public void setMergedSouth(boolean mergedSouth) {
        this.mergedSouth = mergedSouth;
    }

    /**
     * Provides whether the plot is part of the Creative spawn.
     * @return Whether the plot is part of the Creative spawn.
     */
    public boolean isSpawn() {
        return spawn;
    }

    public void setSpawn(boolean spawn) {
        this.spawn = spawn;
    }

    public long getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(long claimedAt) {
        this.claimedAt = claimedAt;
    }
}
