package com.aearost.aranarthcore.items.incantation;

import org.bukkit.inventory.ItemStack;

public interface Incantation {

    ItemStack getItem();
    String getIncantationName();
    int getLevelLimit();

}
