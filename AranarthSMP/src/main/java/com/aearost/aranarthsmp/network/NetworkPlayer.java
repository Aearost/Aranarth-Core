package com.aearost.aranarthsmp.network;

import java.util.UUID;

/** Represents a player on any server in the Aranarth network. */
public class NetworkPlayer {

    private final UUID uuid;
    private String nickname;
    private final String server;
    private final int rank;
    private final int councilRank;
    private final int saintRank;
    private final int architectRank;
    private final boolean vanished;

    public NetworkPlayer(UUID uuid, String nickname, String server,
                         int rank, int councilRank, int saintRank, int architectRank, boolean vanished) {
        this.uuid = uuid;
        this.nickname = nickname;
        this.server = server;
        this.rank = rank;
        this.councilRank = councilRank;
        this.saintRank = saintRank;
        this.architectRank = architectRank;
        this.vanished = vanished;
    }

    public UUID getUuid() { return uuid; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getServer() { return server; }
    public int getRank() { return rank; }
    public int getCouncilRank() { return councilRank; }
    public int getSaintRank() { return saintRank; }
    public int getArchitectRank() { return architectRank; }
    public boolean isVanished() { return vanished; }
}
