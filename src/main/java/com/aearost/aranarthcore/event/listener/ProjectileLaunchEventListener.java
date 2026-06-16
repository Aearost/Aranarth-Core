package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.PlayerAutoReplenishSlot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

/**
 * Centralizes all logic to be called when a projectile is launched.
 */
public class ProjectileLaunchEventListener implements Listener {

    private final AranarthCore plugin;

    public ProjectileLaunchEventListener(AranarthCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity() instanceof Egg || e.getEntity() instanceof Snowball || e.getEntity() instanceof ThrownExpBottle) {
            new PlayerAutoReplenishSlot().execute(e, plugin);
        }
    }
}
