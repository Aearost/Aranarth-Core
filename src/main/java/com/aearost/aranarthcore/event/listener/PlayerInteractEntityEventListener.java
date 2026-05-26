package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.mob.*;
import com.aearost.aranarthcore.event.player.QuestNpcInteract;
import com.aearost.aranarthcore.utils.QuestUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
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
                // Quest NPC check must come first to prevent trade GUI from opening
                if (QuestUtils.isQuestNpc(e.getRightClicked())) {
                    new QuestNpcInteract().execute(e);
                    return;
                }
                new VillagerTradeOverrides().execute(e);
                new VillagerInventoryViewClick().execute(e);
                new VillagerCamelPickup().execute(e);
            } else if (e.getRightClicked() instanceof AbstractHorse || e.getRightClicked() instanceof Wolf
                        || e.getRightClicked() instanceof IronGolem) {
                new SentinelMark().execute(e);
                new MountStats().execute(e);
            } else if (e.getRightClicked() instanceof Camel || e.getRightClicked() instanceof Sniffer) {
                new MountStats().execute(e);
            }

            if (e.getRightClicked() instanceof Tameable) {
                new PetTransferOwnership().execute(e);
            } else if (e.getRightClicked() instanceof Sniffer) {
                new PetTransferOwnership().execute(e);
            }
        }
    }
}
