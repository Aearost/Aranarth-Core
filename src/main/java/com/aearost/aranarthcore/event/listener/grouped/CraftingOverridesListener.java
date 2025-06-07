package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.crafting.*;
import com.aearost.aranarthcore.items.*;
import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumIngot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

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
		for (ItemStack is : e.getInventory().getMatrix()) {
			
			if (is == null) {
				continue;
			}
			if (Objects.isNull(is.getItemMeta())) {
				return;
			}

			if (is.isSimilar(new ChorusDiamond().getItem())) {
				new CraftingOverridesChorusDiamond().onCraft(e, is, player);
			} else if (is.isSimilar(new SugarcaneBlock().getItem())) {
				new CraftingOverridesSugarcaneBlock().onCraft(e, is, player);
			} else if (is.isSimilar(new HoneyGlazedHam().getItem())) {
				new CraftingOverridesHoneyGlazedHam().onCraft(e, is, player);
			} else if (is.isSimilar(new GodAppleFragment().getItem())) {
				new CraftingOverridesGodAppleFragment().onCraft(e, is, player);
			} else if (e.getRecipe().getResult().isSimilar(new Homepad().getItem())) {
				new CraftingOverridesHomepad().onCraft(e, is, player);
			} else if (e.getRecipe().getResult().isSimilar(new AranarthiumIngot().getItem())
						|| is.isSimilar(new AranarthiumIngot().getItem())) {
				new CraftingOverridesAranarthiumIngot().onCraft(e, is, player);
			}
		}
	}
}
