package com.aearost.aranarthcore.recipes.produce;

import com.aearost.aranarthcore.items.produce.FruitSapling;
import com.aearost.aranarthcore.trees.CustomTrees.Variant;
import com.aearost.aranarthcore.utils.CustomItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Set;

/**
 * Crafts a fruit with the sapling of its tree into a fruit sapling, which grows into that fruit tree.
 */
public class RecipeFruitSapling {

    public static final String KEY_PREFIX = "AC_FRUIT_SAPLING_";

    public RecipeFruitSapling(Plugin plugin) {
        createRecipes(plugin);
    }

    /**
     * Creates one recipe for each sapling that a fruit tree grows from.
     * @param plugin The plugin.
     */
    private void createRecipes(Plugin plugin) {
        Set<Material> saplings = EnumSet.noneOf(Material.class);
        for (Variant variant : FruitSapling.getVariants()) {
            saplings.add(variant.getSapling());
        }
        for (Material sapling : saplings) {
            Variant example = null;
            for (Variant variant : FruitSapling.getVariants()) {
                if (variant.getSapling() == sapling) {
                    example = variant;
                    break;
                }
            }
            ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(plugin, KEY_PREFIX + sapling.name()),
                    new FruitSapling(example).getItem());
            recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.FIREWORK_STAR, Material.APPLE));
            recipe.addIngredient(sapling);
            Bukkit.addRecipe(recipe);
        }
    }

    /**
     * Provides the fruit sapling that the crafting grid makes.
     * @return The result, or null if the grid makes nothing.
     */
    public static ItemStack getResult(ItemStack[] matrix) {
        ItemStack fruit = null;
        ItemStack sapling = null;
        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (item.getType() == Material.FIREWORK_STAR || item.getType() == Material.APPLE) {
                if (fruit != null) {
                    return null;
                }
                fruit = item;
            } else {
                if (sapling != null) {
                    return null;
                }
                sapling = item;
            }
        }
        if (fruit == null || sapling == null || CustomItemUtils.isCustomItem(sapling)) {
            return null;
        }
        Variant variant = FruitSapling.forFruit(fruit);
        if (variant == null || variant.getSapling() != sapling.getType()) {
            return null;
        }
        return new FruitSapling(variant).getItem();
    }
}
