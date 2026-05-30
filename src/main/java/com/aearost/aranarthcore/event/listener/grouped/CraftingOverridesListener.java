package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.crafting.*;
import com.aearost.aranarthcore.items.InvisibleItemFrame;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Crafter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.objects.CustomKeys.*;

public class CraftingOverridesListener implements Listener {

	public CraftingOverridesListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles all overrides of default vanilla recipes to cater to custom recipes.
	 * @param e The event.
	 */
	@EventHandler
	public void preCraftItem(final PrepareItemCraftEvent e) {
		if (e.getInventory().contains(Material.BAMBOO_BLOCK)) {
			new CraftingOverridesSugarcaneBlock().preCraft(e);
		}
	}

	/**
	 * Handles cancelling improper crafting recipes.
	 * @param e The event.
	 */
	@EventHandler
	public void onCraftItem(final CraftItemEvent e) {
		HumanEntity player = e.getWhoClicked();
		ItemStack result = e.getRecipe().getResult();
		for (ItemStack ingredient : e.getInventory().getMatrix()) {

			if (ingredient == null) {
				continue;
			}

			if (hasKey(ARANARTHIUM_INGOT, e, ingredient) || hasKey(CLUSTER, e, ingredient) || hasKey(ARMOR_TYPE, e, ingredient)) {
				new CraftingOverridesAranarthium().onCraft(e, ingredient, player);
			}

			if (hasKey(CHORUS_DIAMOND, e, ingredient) || hasKey(HOMEPAD, e, ingredient)) {
				new CraftingOverridesChorusDiamond().onCraft(e, ingredient, player);
				new CraftingOverridesHomepad().onCraft(e, ingredient, player);
			}

			if (hasKey(GOD_APPLE_FRAGMENT, e, ingredient) || result.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
				new CraftingOverridesGodAppleFragment().onCraft(e, ingredient, player);
			}

			if (hasKey(HONEY_GLAZED_HAM, e, ingredient)) {
				new CraftingOverridesHoneyGlazedHam().onCraft(e, ingredient, player);
			}

			if (hasKey(SUGARCANE_BLOCK, e, ingredient)) {
				new CraftingOverridesSugarcaneBlock().onCraft(e, ingredient, player);
			}

			if (hasKey(ARROW, e, ingredient) || hasKey(ARROW_HEAD, e, ingredient) || result.getType() == Material.ARROW) {
				new CraftingOverridesArrows().onCraft(e, ingredient, player);
			}

			if (result.isSimilar(new InvisibleItemFrame().getItem())) {
				if (!player.hasPermission("aranarth.invisible_item_frame")) {
					e.setCancelled(true);
					player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to craft this!"));
					break;
				}
			}
		}
	}

	/**
	 * Enforces all crafting overrides when an item is crafted by a Crafter block.
	 * @param e The event.
	 */
	@EventHandler
	public void onCrafterCraft(final CrafterCraftEvent e) {
		ItemStack result = e.getResult();
		Crafter crafter = (Crafter) e.getBlock().getState();
		for (ItemStack ingredient : crafter.getInventory().getContents()) {
			if (ingredient == null) {
				continue;
			}

			// Prevent vanilla recipes from matching custom items (custom items should not be craftable in a crafter)
			if (ingredient.hasItemMeta() && !ingredient.getItemMeta().getPersistentDataContainer().isEmpty()) {
				e.setCancelled(true);
				return;
			}

			if (hasKey(ARANARTHIUM_INGOT, result, ingredient) || hasKey(CLUSTER, result, ingredient) || hasKey(ARMOR_TYPE, result, ingredient)) {
				new CraftingOverridesAranarthium().onCrafterCraft(e, ingredient);
			}

			if (hasKey(CHORUS_DIAMOND, result, ingredient) || hasKey(HOMEPAD, result, ingredient)) {
				new CraftingOverridesChorusDiamond().onCrafterCraft(e, ingredient);
				new CraftingOverridesHomepad().onCrafterCraft(e, ingredient);
			}

			if (hasKey(GOD_APPLE_FRAGMENT, result, ingredient) || result.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
				new CraftingOverridesGodAppleFragment().onCrafterCraft(e, ingredient);
			}

			if (hasKey(HONEY_GLAZED_HAM, result, ingredient)) {
				new CraftingOverridesHoneyGlazedHam().onCrafterCraft(e, ingredient);
			}

			if (hasKey(SUGARCANE_BLOCK, result, ingredient)) {
				new CraftingOverridesSugarcaneBlock().onCrafterCraft(e, ingredient);
			}

			if (hasKey(ARROW, result, ingredient) || hasKey(ARROW_HEAD, result, ingredient) || result.getType() == Material.ARROW) {
				new CraftingOverridesArrows().onCrafterCraft(e, ingredient);
			}

			if (result.isSimilar(new InvisibleItemFrame().getItem())) {
				e.setCancelled(true);
				break;
			}
		}
	}

	/**
	 * Determines if the provided NamespacedKey is applied to the input ingredient or result of the recipe.
	 * @param key The NamespacedKey to search for.
	 * @param e The crafting event which contains the result.
	 * @param ingredient The ingredient to be searched for.
	 * @return Confirmation whether the input ingredient of the result of the recipe contains the NamespacedKey.
	 */
	private boolean hasKey(NamespacedKey key, CraftItemEvent e, ItemStack ingredient) {
		ItemMeta resultMeta;
		ItemMeta ingredientMeta;
		if (e.getRecipe().getResult().hasItemMeta()) {
			resultMeta = e.getRecipe().getResult().getItemMeta();
			if (resultMeta.getPersistentDataContainer().has(key)) {
				return true;
			}
		}

		if (ingredient.hasItemMeta()) {
			ingredientMeta = ingredient.getItemMeta();
            if (ingredientMeta.getPersistentDataContainer().has(key)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Determines if the provided NamespacedKey is applied to the input ingredient or result item.
	 * @param key The NamespacedKey to search for.
	 * @param result The result ItemStack.
	 * @param ingredient The ingredient to be searched for.
	 * @return Confirmation whether the ingredient or result contains the NamespacedKey.
	 */
	private boolean hasKey(NamespacedKey key, ItemStack result, ItemStack ingredient) {
		if (result.hasItemMeta()) {
			if (result.getItemMeta().getPersistentDataContainer().has(key)) {
				return true;
			}
		}
		if (ingredient.hasItemMeta()) {
			if (ingredient.getItemMeta().getPersistentDataContainer().has(key)) {
				return true;
			}
		}
		return false;
	}
}
