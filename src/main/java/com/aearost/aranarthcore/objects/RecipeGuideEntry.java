package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.enums.RecipeStation;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * A single custom recipe displayed in the recipes guide.
 */
public class RecipeGuideEntry {

    private final RecipeStation station;
    private final ItemStack icon;
    private final ItemStack[] grid;
    private final List<ItemStack> results;
    private final List<List<Material>> alternatives;
    private final List<String> noteKeys;
    private final String searchText;

    /**
     * @param station    The station the recipe is crafted in.
     * @param icon       The item shown in the recipe list.
     * @param grid       The 3x3 ingredient grid, null entries being empty slots.
     * @param results    The result(s) of the recipe.
     * @param alternatives The accepted materials of each grid slot, empty if only one is accepted.
     * @param noteKeys   Lang keys of any additional notes shown on the station item.
     * @param searchText The lowercase text matched against when searching.
     */
    public RecipeGuideEntry(RecipeStation station, ItemStack icon, ItemStack[] grid, List<ItemStack> results,
                            List<List<Material>> alternatives, List<String> noteKeys, String searchText) {
        this.station = station;
        this.icon = icon;
        this.grid = grid;
        this.results = results;
        this.alternatives = alternatives;
        this.noteKeys = noteKeys;
        this.searchText = searchText;
    }

    public RecipeStation getStation() {
        return station;
    }

    public ItemStack getIcon() {
        return icon.clone();
    }

    public ItemStack[] getGrid() {
        return grid;
    }

    public List<ItemStack> getResults() {
        return results;
    }

    public List<Material> getAlternatives(int gridIndex) {
        return alternatives.get(gridIndex);
    }

    public List<String> getNoteKeys() {
        return noteKeys;
    }

    public boolean matches(String filter) {
        return searchText.contains(filter.toLowerCase());
    }
}
