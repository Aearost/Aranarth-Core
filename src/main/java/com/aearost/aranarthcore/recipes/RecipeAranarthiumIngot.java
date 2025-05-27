package com.aearost.aranarthcore.recipes;

import com.aearost.aranarthcore.items.aranarthium.AranarthiumIngot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeAranarthiumIngot {

	public RecipeAranarthiumIngot(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting an Aranarthium Ingot.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_ARANARTHIUM_INGOT");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, AranarthiumIngot.getAranarthiumIngot());
		
		recipe.addIngredient(1, Material.NETHERITE_INGOT);
		recipe.addIngredient(1, Material.PRISMARINE_CRYSTALS);
		recipe.addIngredient(1, Material.TURTLE_SCUTE);
		recipe.addIngredient(1, Material.GOLD_NUGGET);
		recipe.addIngredient(1, Material.IRON_NUGGET);
		recipe.addIngredient(1, Material.BLAZE_POWDER);
		recipe.addIngredient(1, Material.FERMENTED_SPIDER_EYE);
		recipe.addIngredient(1, Material.BLUE_DYE);
		recipe.addIngredient(1, Material.PHANTOM_MEMBRANE);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
