package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.world.ArrowHitBlock;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * Centralizes all logic to be called when a projectile hits an entity or block.
 */
public class ProjectileHitEventListener implements Listener {

    public ProjectileHitEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        // Hit a block
        if (e.getHitBlock() != null) {
            new ArrowHitBlock().execute(e);
        }
    }
}
