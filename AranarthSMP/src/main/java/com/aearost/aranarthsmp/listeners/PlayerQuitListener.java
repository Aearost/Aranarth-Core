package com.aearost.aranarthsmp.listeners;

import com.aearost.aranarthsmp.AranarthSMP;
import com.aearost.aranarthsmp.network.SMPNetworkManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    public PlayerQuitListener(AranarthSMP plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        SMPNetworkManager net = SMPNetworkManager.getInstance();
        if (net == null) return;

        net.publishPlayerQuit(player.getUniqueId());
        net.updateTab();
    }
}
