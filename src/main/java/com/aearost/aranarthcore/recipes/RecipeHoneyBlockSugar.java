package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeHoneyBlockSugar {

	public RecipeHoneyBlockSugar(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for crafting Sugar from a Honey Block.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_HONEY_BLOCK_SUGAR");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.SUGAR, 12));

		recipe.addIngredient(1, Material.HONEY_BLOCK);

		Bukkit.addRecipe(recipe);
	}

}
