package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.mob.VillagerCamelPickup;
import com.aearost.aranarthcore.event.mob.VillagerInventoryViewClick;
import com.aearost.aranarthcore.event.mob.VillagerTradeOverrides;
import com.aearost.aranarthcore.event.player.GuardianMark;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
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
            } else if (e.getRightClicked() instanceof AbstractHorse || e.getRightClicked() instanceof Wolf
                        || e.getRightClicked() instanceof IronGolem) {
                new GuardianMark().execute(e);
            }
        }
    }
}
