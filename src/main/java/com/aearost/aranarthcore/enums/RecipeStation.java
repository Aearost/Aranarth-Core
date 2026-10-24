package com.aearost.aranarthcore.enums;

import org.bukkit.Material;

/**
 * The station in which a custom recipe is crafted, as shown in the recipes guide.
 */
public enum RecipeStation {

    CRAFTING(Material.CRAFTING_TABLE, "gui.recipes.station_crafting"),
    FLETCHING(Material.FLETCHING_TABLE, "gui.recipes.station_fletching"),
    ANVIL(Material.ANVIL, "gui.recipes.station_anvil"),
    SAWMILL(Material.STONECUTTER, "gui.recipes.station_sawmill");

    private final Material icon;
    private final String langKey;

    RecipeStation(Material icon, String langKey) {
        this.icon = icon;
        this.langKey = langKey;
    }

    public Material getIcon() {
        return icon;
    }

    public String getLangKey() {
        return langKey;
    }
}
