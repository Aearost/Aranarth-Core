package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PhantomSpawnPreventListener implements Listener {

    public PhantomSpawnPreventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Prevents phantoms from spawning when their target is wearing Fae Aranarthium.
     */
    @EventHandler
    public void onPhantomPreSpawn(PhantomPreSpawnEvent e) {
        if (e.getSpawningEntity() instanceof Player player && AranarthUtils.isWearingArmorType(player, "fae")) {
            e.setCancelled(true);
        }
    }
}
