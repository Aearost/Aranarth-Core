package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.*;
import com.aearost.aranarthcore.event.player.*;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.DominionLevelUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.MusicInstrumentMeta;

public class PlayerInteractEventListener implements Listener {

    private final AranarthCore plugin;

    public PlayerInteractEventListener(AranarthCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Centralizes all logic to be called by a player interacting with blocks.
     * @param e The event.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {

        new FaeMushroomEat().execute(e);

        if (e.getItem() != null) {
            if (e.getItem().getType().name().equals("LEATHER")) {
                new QuiverClick().execute(e);
            } else if (e.getItem().getType().name().contains("SHULKER_BOX")) {
                new ShulkerClick().execute(e);
            } else if (e.getItem().hasItemMeta()) {
                if (e.getItem().getItemMeta() instanceof MusicInstrumentMeta) {
                    new GoatHornUse().execute(e);
                    return;
                }
            }
        }

        // Hoe used on till-able block - check 1 tick later
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK
                && e.getItem() != null
                && e.getItem().getType().name().endsWith("_HOE")
                && e.getClickedBlock() != null) {
            Block clickedBlock = e.getClickedBlock();
            Material type = clickedBlock.getType();
            if (type == Material.DIRT || type == Material.GRASS_BLOCK
                    || type == Material.DIRT_PATH || type == Material.COARSE_DIRT) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (clickedBlock.getType() == Material.FARMLAND) {
                        Dominion dominion = DominionUtils.getDominionOfChunkAnywhere(clickedBlock.getChunk());
                        if (dominion != null) {
                            dominion.setCachedFarmlandCount(dominion.getCachedFarmlandCount() + 1);
                            DominionLevelUtils.reevaluateDominion(dominion);
                        }
                    }
                }, 1L);
            }
            // 3x3 till of dirt
            if (type == Material.DIRT || type == Material.GRASS_BLOCK) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> new HoeTillArea().execute(e), 1L);
            }
        }

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            new PlayerAutoReplenishSlot().execute(e, plugin);
            new SweetBerryHarvest().execute(e);
            new LogWoodStripPrevent().execute(e);
            new BoneMealSapling().execute(e);
            new BoneMealWood().execute(e);
            new SignDye().execute(e);
            new DragonHeadClick().execute(e);
            new MangroveRootShear().execute(e);
            new DoubleDoorOpen().execute(e);
            new EnderChestOpenPrevent().execute(e);
            new FletchingTableClick().execute(e);
            new ContainerInteract().execute(e);
            new ExpStore().execute(e);
            new CrateOpen().execute(e);
        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            new ChestSort().execute(e);
        } else if (e.getAction() == Action.RIGHT_CLICK_AIR) {
            new ExpStore().execute(e);
        }

        // Ensures the event is only called once
        if (e.getHand() == EquipmentSlot.HAND) {
            if (e.getClickedBlock() != null) {
                new ShopInteract().execute(e);
                new ShopBulkTransaction().execute(e);
            }
        }
    }
}
