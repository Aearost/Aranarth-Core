package com.aearost.aranarthcore.recipes.aranarthium;

import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumFae;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeFaeAranarthium {

    public RecipeFaeAranarthium(Plugin plugin) {
        createRecipe(plugin);
    }

    private void createRecipe(Plugin plugin) {
        NamespacedKey nk = new NamespacedKey(plugin, "AC_FAE_ARANARTHIUM");
        ShapedRecipe recipe = new ShapedRecipe(nk, new AranarthiumFae().getItem());

        recipe.shape("PSP", "REB", "QMQ");

        recipe.setIngredient('P', Material.PINK_PETALS);
        recipe.setIngredient('S', Material.SPORE_BLOSSOM);
        recipe.setIngredient('R', Material.RED_MUSHROOM_BLOCK);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('M', Material.MUSHROOM_STEM);
        recipe.setIngredient('B', Material.BROWN_MUSHROOM_BLOCK);
        recipe.setIngredient('Q', Material.QUARTZ_BLOCK);

        Bukkit.addRecipe(recipe);
    }
}
