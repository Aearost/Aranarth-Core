package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiHolder;
import com.aearost.aranarthcore.gui.GuiShulker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles updating the shulker inventory when the GUI is closed.
 */
public class GuiShulkerClose {
	public void execute(InventoryCloseEvent e) {
		Inventory inventory = e.getInventory();
		HumanEntity entity = e.getPlayer();
		GuiHolder holder = GuiHolder.of(inventory);
		if (holder == null || !(holder.getData() instanceof GuiShulker.OpenedShulker(int slot, ItemStack original))) {
			return;
		}

		// Only write back into the exact shulker that was opened
        ItemStack heldItem = entity.getInventory().getItem(slot);
		if (heldItem == null || !heldItem.isSimilar(original)) {
			slot = -1;
			ItemStack[] contents = entity.getInventory().getContents();
			for (int i = 0; i < contents.length; i++) {
				if (contents[i] != null && contents[i].isSimilar(original)) {
					slot = i;
					break;
				}
			}
			if (slot == -1) {
				Bukkit.getLogger().warning("[AC] " + entity.getName() + " closed a held shulker that is no longer in their inventory, the changes were discarded");
				return;
			}
			heldItem = entity.getInventory().getItem(slot);
		}

		ItemMeta meta = heldItem.hasItemMeta() ? heldItem.getItemMeta() : Bukkit.getItemFactory().getItemMeta(heldItem.getType());
		if (meta instanceof BlockStateMeta im) {
			if (im.getBlockState() instanceof ShulkerBox shulker) {
				shulker.getInventory().setContents(inventory.getContents());
				im.setBlockState(shulker);
				heldItem.setItemMeta(im);
				entity.getInventory().setItem(slot, heldItem);
				if (entity instanceof Player player) {
					Location locationBelow = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();
					player.playSound(locationBelow, Sound.BLOCK_SHULKER_BOX_CLOSE, 1F, 1F);
				}
			}
		}
	}
}
