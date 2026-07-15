package com.aearost.aranarthcore.network;

import java.util.UUID;

/**
 * Represents a player on a remote server in the Aranarth network.
 */
public class NetworkPlayer {

    private final UUID uuid;
    private final String username;
    private String nickname;
    private final String server;
    private final int rank;
    private final int councilRank;
    private final int saintRank;
    private final int architectRank;
    private final boolean vanished;
    private final String textureValue;
    private final String textureSignature;
    private boolean afk;

    public NetworkPlayer(UUID uuid, String username, String nickname, String server,
                         int rank, int councilRank, int saintRank, int architectRank, boolean vanished,
                         String textureValue, String textureSignature) {
        this.uuid = uuid;
        this.username = username != null ? username : "";
        this.nickname = nickname;
        this.server = server;
        this.rank = rank;
        this.councilRank = councilRank;
        this.saintRank = saintRank;
        this.architectRank = architectRank;
        this.vanished = vanished;
        this.textureValue = textureValue != null ? textureValue : "";
        this.textureSignature = textureSignature != null ? textureSignature : "";
    }

    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getServer() { return server; }
    public int getRank() { return rank; }
    public int getCouncilRank() { return councilRank; }
    public int getSaintRank() { return saintRank; }
    public int getArchitectRank() { return architectRank; }
    public boolean isVanished() { return vanished; }
    public String getTextureValue() { return textureValue; }
    public String getTextureSignature() { return textureSignature; }
    public boolean isAfk() { return afk; }
    public void setAfk(boolean afk) { this.afk = afk; }
}
