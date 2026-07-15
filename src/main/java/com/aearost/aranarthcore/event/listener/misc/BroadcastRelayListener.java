package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.BroadcastMessageEvent;

/**
 * Relays server-wide broadcasts (Bukkit.broadcastMessage) to other servers in the network.
 * This ensures plugin-generated announcements (e.g. mcMMO skill thresholds, dominion events,
 * crate openings) are visible to players on all servers, not just the one they originated on.
 */
public class BroadcastRelayListener implements Listener {

    public BroadcastRelayListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBroadcast(BroadcastMessageEvent e) {
        if (!NetworkManager.isActive()) return;
        NetworkManager.getInstance().publishBroadcast(e.getMessage());
    }
}
