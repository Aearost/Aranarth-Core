package com.aearost.aranarthcore.recipes.produce;

import com.aearost.aranarthcore.items.produce.Produce;
import com.aearost.aranarthcore.items.produce.ProduceType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * Crafts one piece of produce on its own into what it holds.
 */
public class RecipeProduce {

    public static final String KEY = "AC_PRODUCE";

    /**
     * What each piece of produce is crafted into, and how many.
     */
    private record Result(ProduceType type, int amount) {}

    private static final Map<ProduceType, Result> RESULTS = Map.ofEntries(
            Map.entry(ProduceType.RICE_STALK, new Result(ProduceType.RICE, 2)),
            Map.entry(ProduceType.TOMATO, new Result(ProduceType.TOMATO_SEEDS, 2)),
            Map.entry(ProduceType.GREEN_BELL_PEPPER, new Result(ProduceType.BELL_PEPPER_SEEDS, 2)),
            Map.entry(ProduceType.YELLOW_BELL_PEPPER, new Result(ProduceType.BELL_PEPPER_SEEDS, 2)),
            Map.entry(ProduceType.ORANGE_BELL_PEPPER, new Result(ProduceType.BELL_PEPPER_SEEDS, 2)),
            Map.entry(ProduceType.RED_BELL_PEPPER, new Result(ProduceType.BELL_PEPPER_SEEDS, 2)),
            Map.entry(ProduceType.CORN, new Result(ProduceType.CORN_SEEDS, 2)),
            Map.entry(ProduceType.CABBAGE, new Result(ProduceType.CABBAGE_SEEDS, 1)),
            Map.entry(ProduceType.LETTUCE, new Result(ProduceType.LETTUCE_SEEDS, 1)),
            Map.entry(ProduceType.CANTALOUPE_SLICE, new Result(ProduceType.CANTALOUPE_SEEDS, 1)),
            Map.entry(ProduceType.KIWI, new Result(ProduceType.KIWI_SEEDS, 2)));

    public RecipeProduce(Plugin plugin) {
        createRecipe(plugin);
    }

    /**
     * Creates the recipe.
     * @param plugin The plugin.
     */
    private void createRecipe(Plugin plugin) {
        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(plugin, KEY), new Produce(ProduceType.RICE).getItem());
        recipe.addIngredient(1, Material.FIREWORK_STAR);
        Bukkit.addRecipe(recipe);
    }

    /**
     * Provides what the crafting grid makes, when it holds exactly one piece of produce that can be crafted and nothing else.
     * @return The result, or null if the grid makes nothing.
     */
    public static ItemStack getResult(ItemStack[] matrix) {
        ItemStack only = null;
        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (only != null) {
                return null;
            }
            only = item;
        }
        Result result = only == null ? null : RESULTS.get(Produce.getType(only));
        if (result == null) {
            return null;
        }
        ItemStack item = new Produce(result.type()).getItem();
        item.setAmount(result.amount());
        return item;
    }
}
