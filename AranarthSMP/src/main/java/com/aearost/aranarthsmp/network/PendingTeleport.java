package com.aearost.aranarthsmp.network;

/**
 * Represents a teleport queued for execution when a player arrives on a server.
 * type="location": teleport to explicit world/coords.
 * type="player": teleport to the named player's current position.
 */
public class PendingTeleport {

    private String type;
    private String world;
    private double x, y, z;
    private float yaw, pitch;
    private String targetUuid;
    private String titleMain;
    private String titleSub;

    public PendingTeleport() {}

    public PendingTeleport(String world, double x, double y, double z, float yaw, float pitch,
                           String titleMain, String titleSub) {
        this.type = "location";
        this.world = world;
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
        this.titleMain = titleMain;
        this.titleSub = titleSub;
    }

    public PendingTeleport(String targetUuid, String titleMain, String titleSub) {
        this.type = "player";
        this.targetUuid = targetUuid;
        this.titleMain = titleMain;
        this.titleSub = titleSub;
    }

    public String getType() { return type; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public String getTargetUuid() { return targetUuid; }
    public String getTitleMain() { return titleMain; }
    public String getTitleSub() { return titleSub; }
}
