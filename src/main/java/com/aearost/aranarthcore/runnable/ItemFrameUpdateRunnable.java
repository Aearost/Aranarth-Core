package com.aearost.aranarthcore.runnable;

import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Inspired by tiffany352
 * Source: https://github.com/tiffany352/InvisibleItemFrames/blob/main/src/main/java/com/tiffnix/invisibleitemframes/ItemFrameUpdateRunnable.java
 */
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