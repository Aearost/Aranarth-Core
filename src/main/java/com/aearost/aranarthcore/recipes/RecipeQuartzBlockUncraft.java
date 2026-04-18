package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeQuartzBlockUncraft {

	public RecipeQuartzBlockUncraft(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Nether Quartz.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_QUARTZ_FROM_QUARTZ_BLOCK");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.QUARTZ, 4));

		recipe.addIngredient(1, Material.QUARTZ_BLOCK);

		Bukkit.addRecipe(recipe);
	}

}
