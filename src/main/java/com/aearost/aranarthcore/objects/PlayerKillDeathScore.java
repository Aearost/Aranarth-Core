package com.aearost.aranarthcore.objects;

import java.util.UUID;

public class PlayerKillDeathScore {

    private UUID uuid;
    private String worldPrefix;
    private int kills;
    private int deaths;

    public PlayerKillDeathScore(UUID uuid, String worldPrefix, int kills, int deaths) {
        this.uuid = uuid;
        this.worldPrefix = worldPrefix;
        this.kills = kills;
        this.deaths = deaths;
    }

    /**
     * Provides the UUID of the player.
     * @return The UUID of the player.
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Updates the UUID of the player.
     * @param uuid The UUID of the player.
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Provides the prefix of the world the kills and deaths are in.
     * @return The prefix of the world the kills and deaths are in.
     */
    public String getWorldPrefix() {
        return worldPrefix;
    }

    /**
     * Updates the prefix of the world the kills and deaths are in.
     * @param worldPrefix The prefix of the world the kills and deaths are in.
     */
    public void setWorldPrefix(String worldPrefix) {
        this.worldPrefix = worldPrefix;
    }

    /**
     * Provides the number of kills the player has in the given world.
     * @return The number of kills the player has in the given world.
     */
    public int getKills() {
        return kills;
    }

    /**
     * Updates the number of kills the player has in the given world.
     * @param kills The number of kills the player has in the given world.
     */
    public void setKills(int kills) {
        this.kills = kills;
    }

    /**
     * Provides the number of deaths the player has in the given world.
     * @return The number of deaths the player has in the given world.
     */
    public int getDeaths() {
        return deaths;
    }

    /**
     * Updates the number of deaths the player has in the given world.
     * @param deaths The number of deaths the player has in the given world.
     */
    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
}
