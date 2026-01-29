package com.aearost.aranarthcore.recipes.aranarthium;

import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumSoulbound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeSoulboundAranarthium {

	public RecipeSoulboundAranarthium(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Soulbound Aranarthium.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_SOULBOUND_ARANARTHIUM");
		ShapedRecipe recipe = new ShapedRecipe(nk, new AranarthiumSoulbound().getItem());

		recipe.shape("YNY", "CEC", "ATA");

		recipe.setIngredient('Y', Material.ENDER_EYE);
		recipe.setIngredient('N', Material.NETHER_STAR);
		recipe.setIngredient('E', Material.ECHO_SHARD);
		recipe.setIngredient('C', Material.CHORUS_FRUIT);
		recipe.setIngredient('A', Material.AMETHYST_SHARD);
		recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);

		Bukkit.addRecipe(recipe);
	}
		
	
}
