package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.items.aranarthium.ingots.*;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.objects.CustomKeys.ARANARTHIUM_INGOT;
import static com.aearost.aranarthcore.objects.CustomKeys.CLUSTER;

/**
 * Handles the overrides when crafting involving an Aranarthium Ingot.
 */
public class CraftingOverridesAranarthium {

    public void onCrafterCraft(CrafterCraftEvent e, ItemStack ingredient) {
        ItemStack result = e.getResult();
        ItemMeta resultMeta = result.getItemMeta();

        if (ingredient.getType() == Material.NETHERITE_INGOT) {
            return;
        }

        if (ingredient.hasItemMeta()) {
            ItemMeta ingredientMeta = ingredient.getItemMeta();

            if (ingredientMeta.getPersistentDataContainer().has(CLUSTER)) {
                if (resultMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT)) {
                    String ingotType = resultMeta.getPersistentDataContainer().get(ARANARTHIUM_INGOT, PersistentDataType.STRING);
                    if (ingotType.equals("aranarthium")) {
                        return;
                    }
                }
            }

            if (ingredientMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT)) {
                if (resultMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT)) {
                    String ingotType = ingredientMeta.getPersistentDataContainer().get(ARANARTHIUM_INGOT, PersistentDataType.STRING);
                    if (ingotType.equals("aranarthium")) {
                        return;
                    }
                }
            }

            if (ingredientMeta instanceof MusicInstrumentMeta) {
                return;
            }
        } else {
            if (resultMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT)) {
                String ingotType = resultMeta.getPersistentDataContainer().get(ARANARTHIUM_INGOT, PersistentDataType.STRING);
                // Allow normal (non-echo-shard) ingredients in variant ingot recipes.
                // Echo shards must be a proper AranarthiumIngot (custom PDC), not vanilla.
                if (!ingotType.equals("aranarthium") && ingredient.getType() != Material.ECHO_SHARD) {
                    return;
                }
            }
        }

        e.setCancelled(true);
    }

    public void onCraft(CraftItemEvent e, ItemStack ingredient, HumanEntity player) {
        ItemStack result = e.getRecipe().getResult();
        ItemMeta resultMeta = result.getItemMeta();

        if (ingredient.getType() == Material.NETHERITE_INGOT) {
            return;
        }

        // Ignores override when crafting aranarthium ingots or enhanced Aranarthium
        if (ingredient.hasItemMeta()) {
            ItemMeta ingredientMeta = ingredient.getItemMeta();

            // If a cluster is used and result is an Aranarthium ingot, this is good
            // Do not use clusters in enhanced Aranarthium ingot recipes
            if (ingredientMeta.getPersistentDataContainer().has(CLUSTER)) {
                if (resultMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT)) {
                    String ingotType = resultMeta.getPersistentDataContainer().get(ARANARTHIUM_INGOT, PersistentDataType.STRING);
                    if (ingotType.equals("aranarthium")) {
                        return;
                    }
                }
            }

            // If Aranarthium Ingot is used to craft an enhanced ingot, this is good
            if (ingredientMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT)) {
                if (resultMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT)) {
                    String ingotType = ingredientMeta.getPersistentDataContainer().get(ARANARTHIUM_INGOT, PersistentDataType.STRING);
                    if (ingotType.equals("aranarthium")) {
                        return;
                    }
                }
            }

            // Skip meta check for Goat horns
            if (ingredientMeta instanceof MusicInstrumentMeta) {
                return;
            }
        }
        // Handles normal ingredients being used to craft enhanced Aranarthium.
        // Echo shards are excluded here because they must specifically be an AranarthiumIngot (custom PDC),
        // not a plain vanilla echo shard.
        else {
            String ingotType = resultMeta.getPersistentDataContainer().get(ARANARTHIUM_INGOT, PersistentDataType.STRING);
            if (!ingotType.equals("aranarthium") && ingredient.getType() != Material.ECHO_SHARD) {
                return;
            }
        }

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
            String ingotPdcValue = ingredient.hasItemMeta()
                ? ingredient.getItemMeta().getPersistentDataContainer().get(ARANARTHIUM_INGOT, PersistentDataType.STRING)
                : null;
            if ("aquatic".equals(ingotPdcValue)) {
                ingredientItem = new AranarthiumAquatic();
            } else if ("ardent".equals(ingotPdcValue)) {
                ingredientItem = new AranarthiumArdent();
            } else if ("dwarven".equals(ingotPdcValue)) {
                ingredientItem = new AranarthiumDwarven();
            } else if ("elven".equals(ingotPdcValue)) {
                ingredientItem = new AranarthiumElven();
            } else if ("scorched".equals(ingotPdcValue)) {
                ingredientItem = new AranarthiumScorched();
            } else if ("soulbound".equals(ingotPdcValue)) {
                ingredientItem = new AranarthiumSoulbound();
            } else {
                ingredientItem = new AranarthiumIngot();
            }
        }

        String itemName = "";
        if (ingredientItem != null) {
            itemName = ingredientItem.getName();
        }

        if (resultMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT)) {
            e.setCancelled(true);
            String ingotType = resultMeta.getPersistentDataContainer().get(ARANARTHIUM_INGOT, PersistentDataType.STRING);
            if (ingotType.equals("aranarthium")) {
                if (material == Material.IRON_INGOT || material == Material.TURTLE_SCUTE) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou must use an " + itemName + " &cto craft this!"));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cYou must use a " + itemName + " &cto craft this!"));
                }
            } else {
                if (material == Material.ECHO_SHARD && ingredientItem instanceof AranarthiumIngot) {
                    // Plain echo shard used where an Aranarthium Ingot (custom) is required
                    player.sendMessage(ChatUtils.chatMessage("&cYou must use an " + itemName + " &cto craft this!"));
                } else if (material == Material.IRON_INGOT || material == Material.TURTLE_SCUTE || material == Material.ECHO_SHARD) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou cannot use an " + itemName + " &cto craft this!"));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a " + itemName + " &cto craft this!"));
                }
            }
        } else {
            e.setCancelled(true);
            if (material == Material.IRON_INGOT || material == Material.TURTLE_SCUTE || material == Material.ECHO_SHARD) {
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use an " + itemName + " &cto craft this!"));
            } else {
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a " + itemName + " &cto craft this!"));
            }
        }
    }
}
