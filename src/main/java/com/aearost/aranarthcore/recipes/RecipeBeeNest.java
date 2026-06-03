package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class RecipeBeeNest {

	public RecipeBeeNest(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for crafting a Bee Nest using Honeycomb Blocks.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_BEE_NEST");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.BEE_NEST, 1));

		recipe.shape("PPP", "BBB", "PPP");

		recipe.setIngredient('P', new MaterialChoice(getPlanksList()));
		recipe.setIngredient('B', Material.HONEYCOMB_BLOCK);

		Bukkit.addRecipe(recipe);
	}

	private List<Material> getPlanksList() {
		List<Material> planks = new ArrayList<>();
		planks.add(Material.OAK_PLANKS);
		planks.add(Material.SPRUCE_PLANKS);
		planks.add(Material.BIRCH_PLANKS);
		planks.add(Material.JUNGLE_PLANKS);
		planks.add(Material.ACACIA_PLANKS);
		planks.add(Material.DARK_OAK_PLANKS);
		planks.add(Material.MANGROVE_PLANKS);
		planks.add(Material.CHERRY_PLANKS);
		planks.add(Material.PALE_OAK_PLANKS);
		planks.add(Material.BAMBOO_PLANKS);
		planks.add(Material.CRIMSON_PLANKS);
		planks.add(Material.WARPED_PLANKS);
		return planks;
	}

}
