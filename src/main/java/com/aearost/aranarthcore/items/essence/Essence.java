package com.aearost.aranarthcore.items.essence;

import org.bukkit.inventory.ItemStack;

public interface Essence {

    ItemStack getItem();
    String getEssenceName();
    int getLevelLimit();
    String getColor();

}
