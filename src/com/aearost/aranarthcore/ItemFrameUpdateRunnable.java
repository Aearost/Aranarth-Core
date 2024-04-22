package com.aearost.aranarthcore;

import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ItemFrameUpdateRunnable extends BukkitRunnable {
    public ItemFrame itemFrame;

    public ItemFrameUpdateRunnable(ItemFrame itemFrame) {
        this.itemFrame = itemFrame;
    }

    @Override
    public void run() {
        final ItemStack item = this.itemFrame.getItem();
        final boolean hasItem = item.getType() != Material.AIR;
        itemFrame.setVisible(!hasItem);
    }
}