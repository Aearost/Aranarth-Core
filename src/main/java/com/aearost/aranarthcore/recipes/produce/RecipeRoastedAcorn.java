package com.aearost.aranarthcore.recipes.produce;

import com.aearost.aranarthcore.items.produce.Produce;
import com.aearost.aranarthcore.items.produce.ProduceType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.plugin.Plugin;

/**
 * Roasts an Acorn into a Roasted Acorn in a furnace, smoker or campfire, as quickly as vanilla food.
 */
public class RecipeRoastedAcorn {

    private static final float EXPERIENCE = 0.35F;

    public RecipeRoastedAcorn(Plugin plugin) {
        createRecipes(plugin);
    }

    /**
     * Creates the recipes.
     * @param plugin The plugin.
     */
    private void createRecipes(Plugin plugin) {
        ItemStack result = new Produce(ProduceType.ROASTED_ACORN).getItem();
        // Acorns never carry seasonal lore, so every acorn is the exact same item
        RecipeChoice acorn = RecipeChoice.exactChoice(new Produce(ProduceType.ACORN).getItem());
        Bukkit.addRecipe(new FurnaceRecipe(new NamespacedKey(plugin, "AC_ROASTED_ACORN_FURNACE"), result, acorn, EXPERIENCE, 200));
        Bukkit.addRecipe(new SmokingRecipe(new NamespacedKey(plugin, "AC_ROASTED_ACORN_SMOKER"), result, acorn, EXPERIENCE, 100));
        Bukkit.addRecipe(new CampfireRecipe(new NamespacedKey(plugin, "AC_ROASTED_ACORN_CAMPFIRE"), result, acorn, EXPERIENCE, 600));
    }
}
