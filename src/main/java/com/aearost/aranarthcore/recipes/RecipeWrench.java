package com.aearost.aranarthcore.recipes;

import com.aearost.aranarthcore.items.Wrench;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeWrench {

    public RecipeWrench(Plugin plugin) {
        createRecipe(plugin);
    }

    private void createRecipe(Plugin plugin) {
        NamespacedKey nk = new NamespacedKey(plugin, "AC_WRENCH");
        ShapedRecipe recipe = new ShapedRecipe(nk, new Wrench().getItem());

        recipe.shape("I I", "III", " I ");

        recipe.setIngredient('I', Material.IRON_INGOT);

        Bukkit.addRecipe(recipe);
    }
}
