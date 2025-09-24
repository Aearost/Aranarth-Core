package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.*;
import com.aearost.aranarthcore.event.mob.MountSwim;
import com.aearost.aranarthcore.event.player.*;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractEventListener implements Listener {

    public PlayerInteractEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Centralizes all logic to be called by a player interacting with blocks.
     * @param e The event.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getItem() != null) {
            if (e.getItem().getType().name().endsWith("_BUNDLE")) {
                new QuiverClick().execute(e);
            } else if (e.getItem().getType().name().contains("SHULKER_BOX")) {
                new ShulkerClick().execute(e);
            }
        }

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            new LogWoodStripPrevent().execute(e);
            new BoneMealSapling().execute(e);
            new BoneMealWood().execute(e);
            new SignDye().execute(e);
            new DragonHeadClick().execute(e);
            new MangroveRootShear().execute(e);
            new DoubleDoorOpen().execute(e);
            new PlayerShopInteract().execute(e);
            new EnderChestOpenPrevent().execute(e);
            new PlayerShopChestOpen().execute(e);
            new FletchingTableClick().execute(e);
            new ContainerInteract().execute(e);
            new ContainerOpenPrevent().execute(e);
        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            new ChestSort().execute(e);
        } else if (e.getAction() == Action.LEFT_CLICK_AIR) {
            new MountSwim().execute(e); // Unclear why this is the case as it is right-clicking the mount
        } else if (e.getAction() == Action.RIGHT_CLICK_AIR) {
            new ExpBottle().execute(e);
        }
    }
}
