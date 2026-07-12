package com.aearost.aranarthsmp;

import com.aearost.aranarthsmp.commands.*;
import com.aearost.aranarthsmp.listeners.PlayerChatListener;
import com.aearost.aranarthsmp.listeners.PlayerJoinListener;
import com.aearost.aranarthsmp.listeners.PlayerQuitListener;
import com.aearost.aranarthsmp.network.SMPNetworkManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Thin companion plugin that runs on the dedicated SMP server.
 * Handles cross-server chat, tab list, teleports, and the /survival command
 * via a shared Redis instance and Velocity plugin messaging (BungeeCord channel).
 */
public class AranarthSMP extends JavaPlugin {

    private static AranarthSMP plugin;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();

        // Connect to Redis
        String host     = getConfig().getString("network.redis.host", "127.0.0.1");
        int    port     = getConfig().getInt("network.redis.port", 6379);
        String password = getConfig().getString("network.redis.password", "");
        String smpName  = getConfig().getString("network.servers.smp", "smp");

        SMPNetworkManager.initialize(host, port, password, smpName);

        if (!SMPNetworkManager.isActive()) {
            getLogger().severe("Could not connect to Redis — AranarthSMP will not function correctly.");
            return;
        }

        // Sync roster with any players already online on other servers
        SMPNetworkManager.getInstance().syncRosterFromRedis();

        // Register BungeeCord channel for player transfers
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Listeners
        new PlayerJoinListener(this);
        new PlayerQuitListener(this);
        new PlayerChatListener(this);

        // Commands
        getCommand("survival").setExecutor(new CommandSurvival());
        getCommand("smp").setExecutor((sender, cmd, alias, args) -> {
            sender.sendMessage("§7You are already on the §eSMP§7!");
            return true;
        });
        getCommand("teleport").setExecutor(new CommandTeleport());
        getCommand("tpaccept").setExecutor(new CommandTpAccept());
        getCommand("tpdeny").setExecutor(new CommandTpDeny());
        getCommand("tphere").setExecutor(new CommandTpHere());

        // Refresh tab list every 5 seconds (same cadence as AranarthCore)
        new BukkitRunnable() {
            @Override
            public void run() {
                SMPNetworkManager net = SMPNetworkManager.getInstance();
                if (net != null) net.updateTab();
            }
        }.runTaskTimer(this, 0L, 100L);

        getLogger().info("AranarthSMP enabled");
    }

    @Override
    public void onDisable() {
        SMPNetworkManager.shutdown();
        getLogger().info("AranarthSMP disabled");
    }

    public static AranarthSMP getInstance() { return plugin; }

    public static boolean isActive() { return SMPNetworkManager.isActive(); }
}
