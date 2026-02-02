package com.aearost.aranarthcore.items.enchantment;

import org.bukkit.inventory.ItemStack;

public interface AranarthEnchantment {

    ItemStack getItem(int level);
    String getEnchantmentName();

}
