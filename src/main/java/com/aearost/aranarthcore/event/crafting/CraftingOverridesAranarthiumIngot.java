package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.items.aranarthium.*;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the overrides when crafting involving a Chorus Diamond.
 */
public class CraftingOverridesAranarthiumIngot {

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        boolean isHasLore = is.getItemMeta().hasLore();
        Material material = is.getType();

        // If regular items are used instead of the fragments or shards
        if (e.getRecipe().getResult().getType() == Material.ECHO_SHARD) {
            String itemName = "";
            if (material == Material.PRISMARINE_CRYSTALS && !DiamondFragment.getDiamondFragment().isSimilar(is)) {
                player.sendMessage(is.getItemMeta().getDisplayName());
                itemName = DiamondFragment.getName();
            } else if (material == Material.TURTLE_SCUTE && !EmeraldFragment.getEmeraldFragment().isSimilar(is)) {
                itemName = EmeraldFragment.getName();
            } else if (material == Material.GOLD_NUGGET && !GoldFragment.getGoldFragment().isSimilar(is)) {
                itemName = GoldFragment.getName();
            } else if (material == Material.IRON_NUGGET && !IronFragment.getIronFragment().isSimilar(is)) {
                itemName = IronFragment.getName();
            } else if (material == Material.BLAZE_POWDER && !CopperFragment.getCopperFragment().isSimilar(is)) {
                itemName = CopperFragment.getName();
            } else if (material == Material.FERMENTED_SPIDER_EYE && !RedstoneCluster.getRedstoneCluster().isSimilar(is)) {
                itemName = RedstoneCluster.getName();
            } else if (material == Material.BLUE_DYE && !LapisCluster.getLapisCluster().isSimilar(is)) {
                itemName = LapisCluster.getName();
            } else if (material == Material.PHANTOM_MEMBRANE && !QuartzCluster.getQuartzCluster().isSimilar(is)) {
                itemName = QuartzCluster.getName();
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
            String itemName = "";
            if (material == Material.ECHO_SHARD) {
                // Add logic for each piece of gear crafted using Aranarthium here
                // Only cancel if it is none of those pieces
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use " + AranarthiumIngot.getName() + " &cto craft this!"));

                return;
            } else if (material == Material.PRISMARINE_CRYSTALS && DiamondFragment.getDiamondFragment().isSimilar(is)) {
                itemName = DiamondFragment.getName();
            } else if (material == Material.TURTLE_SCUTE && EmeraldFragment.getEmeraldFragment().isSimilar(is)) {
                itemName = EmeraldFragment.getName();
            } else if (material == Material.GOLD_NUGGET && GoldFragment.getGoldFragment().isSimilar(is)) {
                itemName = GoldFragment.getName();
            } else if (material == Material.IRON_NUGGET && IronFragment.getIronFragment().isSimilar(is)) {
                itemName = IronFragment.getName();
            } else if (material == Material.BLAZE_POWDER && CopperFragment.getCopperFragment().isSimilar(is)) {
                itemName = CopperFragment.getName();
            } else if (material == Material.FERMENTED_SPIDER_EYE && RedstoneCluster.getRedstoneCluster().isSimilar(is)) {
                itemName = RedstoneCluster.getName();
            } else if (material == Material.BLUE_DYE && LapisCluster.getLapisCluster().isSimilar(is)) {
                itemName = RedstoneCluster.getName();
            } else if (material == Material.PHANTOM_MEMBRANE && QuartzCluster.getQuartzCluster().isSimilar(is)) {
                itemName = QuartzCluster.getName();
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
