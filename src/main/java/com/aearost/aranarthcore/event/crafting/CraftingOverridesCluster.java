package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumIngot;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARANARTHIUM_INGOT;
import static com.aearost.aranarthcore.items.CustomItemKeys.CLUSTER;

/**
 * Handles the overrides when crafting involving an Aranarthium Ingot.
 */
public class CraftingOverridesCluster {

    public void onCraft(CraftItemEvent e, ItemStack ingredient, HumanEntity player) {
        ItemStack result = e.getRecipe().getResult();
        ItemMeta resultMeta = result.getItemMeta();

        if (ingredient.getType() == Material.NETHERITE_INGOT) {
            return;
        }

        if (ingredient.hasItemMeta()) {
            ItemMeta ingredientMeta = ingredient.getItemMeta();

            // If a cluster is used and result is an Aranarthium ingot
            if (ingredientMeta.getPersistentDataContainer().has(CLUSTER, PersistentDataType.STRING)) {
                if (resultMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT, PersistentDataType.STRING)) {
                    return;
                }
            }

            // If Aranarthium Ingot is used to craft something other than an enhanced ingot
            if (ingredientMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT, PersistentDataType.STRING)) {
                if (resultMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT, PersistentDataType.STRING)) {
                    return;
                }
            }
        }

        e.setCancelled(true);
        Material material = ingredient.getType();
        AranarthItem ingredientItem = null;
        if (material == Material.PRISMARINE_CRYSTALS) {
            ingredientItem = new DiamondCluster();
        } else if (material == Material.TURTLE_SCUTE) {
            ingredientItem = new EmeraldCluster();
        } else if (material == Material.GOLD_NUGGET) {
            ingredientItem = new GoldCluster();
        } else if (material == Material.IRON_NUGGET) {
            ingredientItem = new IronCluster();
        } else if (material == Material.BLAZE_POWDER) {
            ingredientItem = new CopperCluster();
        } else if (material == Material.FERMENTED_SPIDER_EYE) {
            ingredientItem = new RedstoneCluster();
        } else if (material == Material.BLUE_DYE) {
            ingredientItem = new LapisCluster();
        } else if (material == Material.PHANTOM_MEMBRANE) {
            ingredientItem = new QuartzCluster();
        } else if (material == Material.ECHO_SHARD) {
            ingredientItem = new AranarthiumIngot();
        }

        String itemName = "";
        if (ingredientItem != null) {
            itemName = ingredientItem.getName();
        }

        if (resultMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT, PersistentDataType.STRING)) {
            if (material == Material.IRON_INGOT || material == Material.TURTLE_SCUTE) {
                player.sendMessage(ChatUtils.chatMessage("&cYou must use an " + itemName + " &cto craft this!"));
            } else {
                player.sendMessage(ChatUtils.chatMessage("&cYou must use a " + itemName + " &cto craft this!"));
            }
        } else {
            if (material == Material.IRON_INGOT || material == Material.TURTLE_SCUTE || material == Material.ECHO_SHARD) {
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use an " + itemName + " &cto craft this!"));
            } else {
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a " + itemName + " &cto craft this!"));
            }
        }




//
//        // If a fragment or cluster is used instead of a regular material
//        // Also if an Aranarthium Ingot is used instead of a normal Echo Shard
//        else if (e.getRecipe().getResult().getType() != Material.ECHO_SHARD) {
//            AranarthItem cluster = null;
//            if (material == Material.ECHO_SHARD) {
//                // Add logic for each piece of gear crafted using Aranarthium here
//                // Only cancel if it is none of those pieces
//                e.setCancelled(true);
//                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use " + new AranarthiumIngot().getName() + " &cto craft this!"));
//
//                return;
//            }
//            else if (material == Material.PRISMARINE_CRYSTALS) {
//                cluster = new DiamondCluster();
//            } else if (material == Material.TURTLE_SCUTE) {
//                cluster = new EmeraldCluster();
//            } else if (material == Material.GOLD_NUGGET) {
//                cluster = new GoldCluster();
//            } else if (material == Material.IRON_NUGGET) {
//                cluster = new IronCluster();
//            } else if (material == Material.BLAZE_POWDER) {
//                cluster = new CopperCluster();
//            } else if (material == Material.FERMENTED_SPIDER_EYE) {
//                cluster = new RedstoneCluster();
//            } else if (material == Material.BLUE_DYE) {
//                cluster = new LapisCluster();
//            } else if (material == Material.PHANTOM_MEMBRANE) {
//                cluster = new QuartzCluster();
//            }
//
//            String itemName = "";
//            if (cluster != null && !cluster.getItem().isSimilar(inputItem)) {
//                player.sendMessage(inputItem.getItemMeta().getDisplayName());
//                itemName = cluster.getName();
//            } else {
//                Bukkit.getLogger().info("Something went wrong with crafting this...");
//                e.setCancelled(true);
//                return;
//            }
//
//            if (!itemName.isEmpty()) {
//                e.setCancelled(true);
//                if (material == Material.IRON_INGOT || material == Material.TURTLE_SCUTE) {
//                    player.sendMessage(ChatUtils.chatMessage("&cYou cannot use an " + itemName + " &cto craft this!"));
//                } else {
//                    player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a " + itemName + " &cto craft this!"));
//                }
//                return;
//            }
//        }
    }

}
