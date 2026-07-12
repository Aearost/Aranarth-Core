package com.aearost.aranarthcore.network;

/**
 * Represents a teleport that should be executed when a player arrives on this server
 * after a cross-server transfer.
 *
 * <p>If {@code targetUuid} is set, the player is teleported to that player's current location
 * (resolved at time of arrival). Otherwise the explicit x/y/z/world coordinates are used.</p>
 */
public class PendingTeleport {

    /** "player" → teleport to targetUuid's current position; "location" → use explicit coords; "command" → dispatch a command. */
    private String type;

    // Used when type = "location"
    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    // Used when type = "player"
    private String targetUuid;

    // Used when type = "command"
    private String command;

    private String titleMain;
    private String titleSub;

    public PendingTeleport() {}

    /** Constructor for a fixed-location pending teleport. */
    public PendingTeleport(String world, double x, double y, double z, float yaw, float pitch,
                           String titleMain, String titleSub) {
        this.type = "location";
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.titleMain = titleMain;
        this.titleSub = titleSub;
    }

    /** Constructor for a player-target pending teleport (teleport to wherever that player is). */
    public PendingTeleport(String targetUuid, String titleMain, String titleSub) {
        this.type = "player";
        this.targetUuid = targetUuid;
        this.titleMain = titleMain;
        this.titleSub = titleSub;
    }

    /** Factory method for a command-dispatch pending teleport (dispatches the command on arrival). */
    public static PendingTeleport forCommand(String command, String titleMain, String titleSub) {
        PendingTeleport pt = new PendingTeleport();
        pt.type = "command";
        pt.command = command;
        pt.titleMain = titleMain;
        pt.titleSub = titleSub;
        return pt;
    }

    public String getType() { return type; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public String getTargetUuid() { return targetUuid; }
    public String getCommand() { return command; }
    public String getTitleMain() { return titleMain; }
    public String getTitleSub() { return titleSub; }
}
