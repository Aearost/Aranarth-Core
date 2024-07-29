package com.aearost.aranarthcore.recipes;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeWoolUncraft {

	public RecipeWoolUncraft(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting a String.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_STRING_FROM_WOOL");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.STRING, 4));
		
		MaterialChoice wool = new MaterialChoice(getWoolList());
		
		recipe.addIngredient(wool);
		
		Bukkit.addRecipe(recipe);
	}
	
	private List<Material> getWoolList() {
		List<Material> woolList = new ArrayList<>();
		woolList.add(Material.BLACK_WOOL);
		woolList.add(Material.BLUE_WOOL);
		woolList.add(Material.BROWN_WOOL);
		woolList.add(Material.CYAN_WOOL);
		woolList.add(Material.GRAY_WOOL);
		woolList.add(Material.GREEN_WOOL);
		woolList.add(Material.LIGHT_BLUE_WOOL);
		woolList.add(Material.LIGHT_GRAY_WOOL);
		woolList.add(Material.LIME_WOOL);
		woolList.add(Material.MAGENTA_WOOL);
		woolList.add(Material.ORANGE_WOOL);
		woolList.add(Material.PINK_WOOL);
		woolList.add(Material.PURPLE_WOOL);
		woolList.add(Material.RED_WOOL);
		woolList.add(Material.WHITE_WOOL);
		woolList.add(Material.YELLOW_WOOL);
		return woolList;
	}
	
}
