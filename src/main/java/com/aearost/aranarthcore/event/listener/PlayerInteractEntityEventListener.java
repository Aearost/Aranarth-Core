package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.mob.VillagerCamelPickup;
import com.aearost.aranarthcore.event.mob.VillagerInventoryViewClick;
import com.aearost.aranarthcore.event.mob.VillagerTradeOverrides;
import org.bukkit.Bukkit;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class PlayerInteractEntityEventListener implements Listener {

    public PlayerInteractEntityEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Centralizes all logic to be called by a player interacting with an entity.
     * @param e The event.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() != null) {
            if (e.getRightClicked() instanceof Villager) {
                new VillagerTradeOverrides().execute(e);
                new VillagerInventoryViewClick().execute(e);
                new VillagerCamelPickup().execute(e);
            }
        }
    }
}
