package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumIngot;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the overrides when crafting involving an Aranarthium Ingot.
 */
public class CraftingOverridesAranarthiumIngot {

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        boolean isHasLore = is.getItemMeta().hasLore();
        Material material = is.getType();

        // If regular items are used instead of the fragments or shards
        if (e.getRecipe().getResult().getType() == Material.ECHO_SHARD) {
            AranarthItem cluster = null;
            if (material == Material.PRISMARINE_CRYSTALS) {
                cluster = new DiamondCluster();
            } else if (material == Material.TURTLE_SCUTE) {
                cluster = new EmeraldCluster();
            } else if (material == Material.GOLD_NUGGET) {
                cluster = new GoldCluster();
            } else if (material == Material.IRON_NUGGET) {
                cluster = new IronCluster();
            } else if (material == Material.BLAZE_POWDER) {
                cluster = new CopperCluster();
            } else if (material == Material.FERMENTED_SPIDER_EYE) {
                cluster = new RedstoneCluster();
            } else if (material == Material.BLUE_DYE) {
                cluster = new LapisCluster();
            } else if (material == Material.PHANTOM_MEMBRANE) {
                cluster = new QuartzCluster();
            }

            String itemName = "";
            if (cluster != null && !cluster.getItem().isSimilar(is)) {
                player.sendMessage(is.getItemMeta().getDisplayName());
                itemName = cluster.getName();
            } else {
                Bukkit.getLogger().info("Something went wrong with crafting this...");
                e.setCancelled(true);
                return;
            }

            if (!itemName.isEmpty()) {
                e.setCancelled(true);
                if (material == Material.IRON_INGOT || material == Material.TURTLE_SCUTE) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou must use an " + itemName + " &cto craft this!"));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cYou must use a " + itemName + " &cto craft this!"));
                }
                return;
            }
        }
        // If a fragment or cluster is used instead of a regular material
        // Also if an Aranarthium Ingot is used instead of a normal Echo Shard
        else if (e.getRecipe().getResult().getType() != Material.ECHO_SHARD) {
            AranarthItem cluster = null;
            if (material == Material.ECHO_SHARD) {
                // Add logic for each piece of gear crafted using Aranarthium here
                // Only cancel if it is none of those pieces
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use " + new AranarthiumIngot().getName() + " &cto craft this!"));

                return;
            }
            else if (material == Material.PRISMARINE_CRYSTALS) {
                cluster = new DiamondCluster();
            } else if (material == Material.TURTLE_SCUTE) {
                cluster = new EmeraldCluster();
            } else if (material == Material.GOLD_NUGGET) {
                cluster = new GoldCluster();
            } else if (material == Material.IRON_NUGGET) {
                cluster = new IronCluster();
            } else if (material == Material.BLAZE_POWDER) {
                cluster = new CopperCluster();
            } else if (material == Material.FERMENTED_SPIDER_EYE) {
                cluster = new RedstoneCluster();
            } else if (material == Material.BLUE_DYE) {
                cluster = new LapisCluster();
            } else if (material == Material.PHANTOM_MEMBRANE) {
                cluster = new QuartzCluster();
            }

            String itemName = "";
            if (cluster != null && !cluster.getItem().isSimilar(is)) {
                player.sendMessage(is.getItemMeta().getDisplayName());
                itemName = cluster.getName();
            } else {
                Bukkit.getLogger().info("Something went wrong with crafting this...");
                e.setCancelled(true);
                return;
            }

            if (!itemName.isEmpty()) {
                e.setCancelled(true);
                if (material == Material.IRON_INGOT || material == Material.TURTLE_SCUTE) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou cannot use an " + itemName + " &cto craft this!"));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a " + itemName + " &cto craft this!"));
                }
                return;
            }
        }
    }

}
