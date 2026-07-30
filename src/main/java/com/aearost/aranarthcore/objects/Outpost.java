package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a dominion outpost that is a land claim which is disconnected from the main dominion.
 * Each dominion may have up to 4 outposts, unlocked at dominion levels 2–5 respectively.
 */
public class Outpost {

    private final UUID id;
    private String name;
    private final UUID dominionId;
    private int outpostIndex;
    private Location home;
    private String homeWorldName;
    private List<Chunk> chunks;
    private int boughtChunks;
    private final long createdTimestamp;
    private Material icon;
    private Set<Biome> cachedBiomes = new HashSet<>();

    public Outpost(UUID id, String name, UUID dominionId, int outpostIndex,
                   String worldName, double homeX, double homeY, double homeZ,
                   float homeYaw, float homePitch, List<Chunk> chunks, int boughtChunks, long createdTimestamp) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name;
        this.dominionId = dominionId;
        this.outpostIndex = outpostIndex;
        String actualWorldName = worldName != null && worldName.startsWith("smp:") ? worldName.substring(4) : worldName;
        this.home = new Location(Bukkit.getWorld(actualWorldName), homeX, homeY, homeZ, homeYaw, homePitch);
        this.homeWorldName = worldName;
        this.chunks = chunks;
        this.boughtChunks = boughtChunks;
        this.createdTimestamp = createdTimestamp;
        this.icon = Material.OAK_LOG;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getDominionId() {
        return dominionId;
    }

    public int getOutpostIndex() {
        return outpostIndex;
    }

    public void setOutpostIndex(int outpostIndex) {
        this.outpostIndex = outpostIndex;
    }

    public Location getHome() {
        return home;
    }

    public String getHomeWorldName() {
        return homeWorldName;
    }

    public void setHome(Location home) {
        this.home = home;
        if (home != null && home.getWorld() != null) {
            this.homeWorldName = AranarthUtils.toStoredDominionWorldName(home.getWorld().getName());
        }
    }

    public List<Chunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<Chunk> chunks) {
        this.chunks = chunks;
    }

    public int getBoughtChunks() {
        return boughtChunks;
    }

    public void setBoughtChunks(int boughtChunks) {
        this.boughtChunks = boughtChunks;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public Material getIcon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public Set<Biome> getCachedBiomes() {
        return cachedBiomes;
    }

    public void setCachedBiomes(Set<Biome> cachedBiomes) {
        this.cachedBiomes = cachedBiomes;
    }
}
