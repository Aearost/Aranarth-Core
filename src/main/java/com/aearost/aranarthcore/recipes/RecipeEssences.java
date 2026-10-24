package com.aearost.aranarthcore.recipes;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.items.essence.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeEssences {

	/** The prefix of every essence recipe key. */
	public static final String KEY_PREFIX = "ac_essence_";
	/** The suffix of the recipe keys where the corner and edge clusters are swapped. */
	public static final String ALT_SUFFIX = "_alt";

	public RecipeEssences(Plugin plugin) {
		createRecipe(plugin, "beheading", new EssenceBeheading(), new CopperCluster(), new QuartzCluster());
		createRecipe(plugin, "magnetism", new EssenceMagnetism(), new IronCluster(), new RedstoneCluster());
		createRecipe(plugin, "lifesteal", new EssenceLifesteal(), new RedstoneCluster(), new DiamondCluster());
		createRecipe(plugin, "resilience", new EssenceResilience(), new IronCluster(), new DiamondCluster());
		createRecipe(plugin, "preservation", new EssencePreservation(), new LapisCluster(), new EmeraldCluster());
		createRecipe(plugin, "plentiful", new EssencePlentiful(), new GoldCluster(), new EmeraldCluster());
	}

	/**
	 * Creates the recipe for crafting an essence from an imbued blank Essence, in both cluster orientations.
	 * The clusters and imbued XP are validated in EssenceImbueListener.
	 * @param plugin The plugin.
	 * @param name The name of the essence.
	 * @param essence The resulting essence.
	 * @param first The cluster placed in the corners.
	 * @param second The cluster placed on the edges.
	 */
	private void createRecipe(Plugin plugin, String name, Essence essence, AranarthItem first, AranarthItem second) {
		Material firstMaterial = first.getItem().getType();
		Material secondMaterial = second.getItem().getType();

		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, KEY_PREFIX + name), essence.getItem());
		recipe.shape("ABA", "BEB", "ABA");
		recipe.setIngredient('A', firstMaterial);
		recipe.setIngredient('B', secondMaterial);
		recipe.setIngredient('E', Material.PRISMARINE_SHARD);
		Bukkit.addRecipe(recipe);

		ShapedRecipe altRecipe = new ShapedRecipe(new NamespacedKey(plugin, KEY_PREFIX + name + ALT_SUFFIX), essence.getItem());
		altRecipe.shape("BAB", "AEA", "BAB");
		altRecipe.setIngredient('A', firstMaterial);
		altRecipe.setIngredient('B', secondMaterial);
		altRecipe.setIngredient('E', Material.PRISMARINE_SHARD);
		Bukkit.addRecipe(altRecipe);
	}
}
