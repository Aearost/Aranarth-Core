package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;

import java.util.Objects;

public class CraftingOverrides implements Listener {

	public CraftingOverrides(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles all overrides of default vanilla recipes to cater to custom recipes.
	 * @param e The event.
	 */
	@EventHandler
	public void preCraftItem(final PrepareItemCraftEvent e) {
		int nullCounter = 0;
		int sugarcaneBlockCounter = 0;
		for (ItemStack is : e.getInventory().getMatrix()) {
			
			if (is == null) {
				nullCounter++;
				continue;
			}
			
			if (is.getType() == Material.BAMBOO_BLOCK && Objects.nonNull(is.getItemMeta()) && is.getItemMeta().hasLore()) {
				sugarcaneBlockCounter++;
			}
		}

		if (nullCounter == 8 && sugarcaneBlockCounter == 1) {
			e.getInventory().setResult(new ItemStack(Material.SUGAR_CANE, 9));
		}
	}

	/**
	 * Handles cancelling improper crafting recipes.
	 * @param e The event.
	 */
	@EventHandler
	public void onCraftItem(final CraftItemEvent e) {
		HumanEntity player = e.getWhoClicked();
		for (ItemStack is : e.getInventory().getMatrix()) {
			
			if (is == null) {
				continue;
			}
			if (Objects.isNull(is.getItemMeta())) {
				return;
			}
			boolean isHasLore = is.getItemMeta().hasLore();
			
			// Chorus Diamond
			if (is.getType() == Material.DIAMOND) {
				// If a Chorus Diamond is used in place of a regular Diamond
				if (isHasLore) {
					if (e.getRecipe().getResult().getType() != Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessageError("You cannot use a Chorus Diamond to craft this!"));
						return;
					}
				}
				// If a regular Diamond is used in place of a Chorus Diamond
				else {
					if (e.getRecipe().getResult().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessageError("You must use a Chorus Diamond to craft this!"));
						return;
					}
				}
			}
			// Sugarcane Block
			else if (is.getType() == Material.BAMBOO_BLOCK) {
				if (isHasLore) {
					// If a Sugarcane Block is used in place of a regular Sugarcane or Bamboo
					if (e.getRecipe().getResult().getType() != Material.SUGAR_CANE &&
							e.getRecipe().getResult().getType() != Material.BAMBOO) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessageError("You cannot use a Sugarcane Block to craft this!"));
						return;
					}
				}
				// If a Sugarcane or Bamboo is used in place of a Sugarcane Block
				else {
					if (e.getRecipe().getResult().getType() == Material.SUGAR_CANE) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessageError("You must use a Sugarcane Block to craft this!"));
						return;
					}
				}
			}
			// Honey Glazed Ham
			else if (is.getType() == Material.COOKED_PORKCHOP) {
				
				// If a Honey Glazed Ham is used in place of a regular Cooked Porkchop
				if (isHasLore) {
					if (e.getRecipe().getResult().getType() == Material.COOKED_PORKCHOP) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessageError("You cannot use Honey Glazed Ham to craft this!"));
						return;
					}
				}
				// If a Cooked Porkchop is used in place of a Honey Glazed Ham
				else {
					if (e.getRecipe().getResult().getType() != Material.COOKED_PORKCHOP) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessageError("You must use a regular Cooked Porkchop to craft this!"));
						return;
					}
				}
			}
			// Homepad prevent craft in non-survival worlds
			else if (e.getRecipe().getResult().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
				if (!Objects.requireNonNull(player.getLocation().getWorld()).getName().startsWith("world")) {
					e.setCancelled(true);
					player.sendMessage(ChatUtils.chatMessageError("You cannot craft a homepad in this world!"));
					return;
				}
			}
		}
	}
}
