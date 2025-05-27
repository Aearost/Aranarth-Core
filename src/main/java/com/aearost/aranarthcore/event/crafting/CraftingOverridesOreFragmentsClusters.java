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
public class CraftingOverridesOreFragmentsClusters {

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        boolean isHasLore = is.getItemMeta().hasLore();
        Material material = is.getType();

        // If regular items are used instead of the fragments or shards
        if (e.getRecipe().getResult().getType() == Material.ECHO_SHARD) {
            boolean containsInvalidItem = false;
            if (material == Material.PRISMARINE_CRYSTALS && !DiamondFragment.getDiamondFragment().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.TURTLE_SCUTE && !EmeraldFragment.getEmeraldFragment().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.GOLD_NUGGET && !GoldFragment.getGoldFragment().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.IRON_NUGGET && !IronFragment.getIronFragment().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.BLAZE_POWDER && !CopperFragment.getCopperFragment().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.FERMENTED_SPIDER_EYE && !RedstoneCluster.getRedstoneCluster().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.BLUE_DYE && !LapisCluster.getLapisCluster().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.PHANTOM_MEMBRANE && !QuartzCluster.getQuartzCluster().isSimilar(is)) {
                containsInvalidItem = true;
            }
            if (containsInvalidItem) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou must use fragments and clusters to craft this!"));
                return;
            }
        }
        // If a fragment or cluster is used instead of a regular material
        else if (e.getRecipe().getResult().getType() != Material.ECHO_SHARD) {
            boolean containsInvalidItem = false;
            if (material == Material.PRISMARINE_CRYSTALS && DiamondFragment.getDiamondFragment().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.TURTLE_SCUTE && EmeraldFragment.getEmeraldFragment().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.GOLD_NUGGET && GoldFragment.getGoldFragment().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.IRON_NUGGET && IronFragment.getIronFragment().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.BLAZE_POWDER && CopperFragment.getCopperFragment().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.FERMENTED_SPIDER_EYE && RedstoneCluster.getRedstoneCluster().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.BLUE_DYE && LapisCluster.getLapisCluster().isSimilar(is)) {
                containsInvalidItem = true;
            } else if (material == Material.PHANTOM_MEMBRANE && QuartzCluster.getQuartzCluster().isSimilar(is)) {
                containsInvalidItem = true;
            }

            if (containsInvalidItem) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use fragments or clusters to craft this!"));
                return;
            }
        }
    }

}
