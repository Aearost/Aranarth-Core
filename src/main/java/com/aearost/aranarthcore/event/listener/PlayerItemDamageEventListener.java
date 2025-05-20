package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.DurabilityDecreaseWarning;
import com.aearost.aranarthcore.event.world.ArenaDurabilityPrevent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

/**
 * Centralizes all logic to be called by an item taking damage.
 */
public class PlayerItemDamageEventListener implements Listener {

    public PlayerItemDamageEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent e) {
        if (e.getPlayer().getWorld().getName().equalsIgnoreCase("arena")) {
            new ArenaDurabilityPrevent().execute(e);
        } else {
            new DurabilityDecreaseWarning().execute(e);
        }
    }
}
