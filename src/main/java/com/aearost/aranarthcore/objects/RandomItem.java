package com.aearost.aranarthcore.objects;

import org.bukkit.inventory.ItemStack;

public class RandomItem {

    private ItemStack item;
    private int percentage;

    public RandomItem(int percentage, ItemStack item) {
        this.item = item;
        this.percentage = percentage;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

}
