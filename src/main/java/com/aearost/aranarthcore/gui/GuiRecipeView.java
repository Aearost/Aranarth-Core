package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.RecipeGuideEntry;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI displaying the ingredients and result(s) of a single custom recipe.
 */
public class GuiRecipeView {

    public static final String TITLE_KEY = "gui.recipes.view_title";
    public static final int SLOT_BACK = 49;

    private static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int[] MULTI_RESULT_SLOTS = {14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35};
    private static final int SLOT_STATION = 23;
    private static final int SLOT_STATION_MULTI = 21;
    private static final int SLOT_RESULT = 25;
    private static final int MAX_ALTERNATIVES_SHOWN = 8;

    private final Player player;
    private final RecipeGuideEntry entry;
    private final Inventory gui;

    public GuiRecipeView(Player player, RecipeGuideEntry entry) {
        this.player = player;
        this.entry = entry;
        this.gui = build();
    }

    private Inventory build() {
        Inventory inv = Bukkit.createInventory(player, 54, Lang.getFor(player, TITLE_KEY));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }

        // Ingredients, leaving empty grid cells blank to resemble a crafting grid
        ItemStack[] grid = entry.getGrid();
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            inv.setItem(GRID_SLOTS[i], grid[i] == null ? null : createIngredientDisplay(grid[i], entry.getAlternatives(i)));
        }

        // Results
        List<ItemStack> results = entry.getResults();
        boolean isMultiResult = results.size() > 1;
        if (isMultiResult) {
            for (int i = 0; i < MULTI_RESULT_SLOTS.length; i++) {
                inv.setItem(MULTI_RESULT_SLOTS[i], i < results.size() ? results.get(i).clone() : null);
            }
        } else {
            inv.setItem(SLOT_RESULT, results.getFirst().clone());
        }

        // Station
        List<String> stationLore = new ArrayList<>();
        for (String noteKey : entry.getNoteKeys()) {
            stationLore.add(Lang.getFor(player, noteKey));
        }
        inv.setItem(isMultiResult ? SLOT_STATION_MULTI : SLOT_STATION, buildItem(entry.getStation().getIcon(),
                Lang.getFor(player, "gui.recipes.station_name", "station", Lang.getFor(player, entry.getStation().getLangKey())),
                stationLore));

        inv.setItem(SLOT_BACK, buildItem(Material.ARROW, Lang.getFor(player, "gui.recipes.back"), List.of(
                Lang.getFor(player, "gui.recipes.return_list"))));

        return inv;
    }

    /**
     * Adds the list of accepted materials to an ingredient that can be one of many materials.
     */
    private ItemStack createIngredientDisplay(ItemStack ingredient, List<Material> alternatives) {
        ItemStack display = ingredient.clone();
        if (alternatives.isEmpty()) {
            return display;
        }
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(ChatUtils.translateToColor(Lang.getFor(player, "gui.recipes.accepts_any")));
        for (int i = 0; i < alternatives.size() && i < MAX_ALTERNATIVES_SHOWN; i++) {
            lore.add(ChatUtils.translateToColor(Lang.getFor(player, "gui.recipes.accepts_entry",
                    "item", ChatUtils.getFormattedItemName(alternatives.get(i).name()))));
        }
        if (alternatives.size() > MAX_ALTERNATIVES_SHOWN) {
            lore.add(ChatUtils.translateToColor(Lang.getFor(player, "gui.recipes.accepts_more",
                    "n", String.valueOf(alternatives.size() - MAX_ALTERNATIVES_SHOWN))));
        }
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(name));
        meta.setLore(lore.stream().map(ChatUtils::translateToColor).toList());
        item.setItemMeta(meta);
        return item;
    }

    public void openGui() {
        player.openInventory(gui);
    }
}
