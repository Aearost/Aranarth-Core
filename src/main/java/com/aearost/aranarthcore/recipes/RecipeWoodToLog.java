package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeWoodToLog {

	/**
	 * Each wood block paired with the log it turns back into, including the stripped and Nether variants.
	 */
	private static final Material[][] WOOD_TO_LOG = {
		{Material.OAK_WOOD, Material.OAK_LOG},
		{Material.SPRUCE_WOOD, Material.SPRUCE_LOG},
		{Material.BIRCH_WOOD, Material.BIRCH_LOG},
		{Material.JUNGLE_WOOD, Material.JUNGLE_LOG},
		{Material.ACACIA_WOOD, Material.ACACIA_LOG},
		{Material.DARK_OAK_WOOD, Material.DARK_OAK_LOG},
		{Material.MANGROVE_WOOD, Material.MANGROVE_LOG},
		{Material.CHERRY_WOOD, Material.CHERRY_LOG},
		{Material.PALE_OAK_WOOD, Material.PALE_OAK_LOG},
		{Material.POPLAR_WOOD, Material.POPLAR_LOG},
		{Material.CRIMSON_HYPHAE, Material.CRIMSON_STEM},
		{Material.WARPED_HYPHAE, Material.WARPED_STEM},
		{Material.STRIPPED_OAK_WOOD, Material.STRIPPED_OAK_LOG},
		{Material.STRIPPED_SPRUCE_WOOD, Material.STRIPPED_SPRUCE_LOG},
		{Material.STRIPPED_BIRCH_WOOD, Material.STRIPPED_BIRCH_LOG},
		{Material.STRIPPED_JUNGLE_WOOD, Material.STRIPPED_JUNGLE_LOG},
		{Material.STRIPPED_ACACIA_WOOD, Material.STRIPPED_ACACIA_LOG},
		{Material.STRIPPED_DARK_OAK_WOOD, Material.STRIPPED_DARK_OAK_LOG},
		{Material.STRIPPED_MANGROVE_WOOD, Material.STRIPPED_MANGROVE_LOG},
		{Material.STRIPPED_CHERRY_WOOD, Material.STRIPPED_CHERRY_LOG},
		{Material.STRIPPED_PALE_OAK_WOOD, Material.STRIPPED_PALE_OAK_LOG},
		{Material.STRIPPED_POPLAR_WOOD, Material.STRIPPED_POPLAR_LOG},
		{Material.STRIPPED_CRIMSON_HYPHAE, Material.STRIPPED_CRIMSON_STEM},
		{Material.STRIPPED_WARPED_HYPHAE, Material.STRIPPED_WARPED_STEM},
	};

	public RecipeWoodToLog(Plugin plugin) {
		createRecipes(plugin);
	}

	/**
	 * Creates the recipes for turning 3 wood blocks stacked in a column back into 3 logs, since custom trees grow
	 * from wood rather than logs.
	 * @param plugin The plugin.
	 */
	private void createRecipes(Plugin plugin) {
		for (Material[] pair : WOOD_TO_LOG) {
			Material wood = pair[0];
			Material log = pair[1];
			NamespacedKey nk = new NamespacedKey(plugin, "AC_" + log.name() + "_FROM_WOOD");
			ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(log, 3));

			recipe.shape("W", "W", "W");

			recipe.setIngredient('W', wood);

			Bukkit.addRecipe(recipe);
		}
	}

	/**
	 * Provides each wood block paired with the log it turns back into.
	 * @return The wood and log pairs.
	 */
	public static Material[][] getWoodToLog() {
		return WOOD_TO_LOG;
	}

}
