package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiShulkerPreventDrop implements Listener {

	public GuiShulkerPreventDrop(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents the player from dropping the held shulker box when in an inventory
	 * @param e The event.
	 */
	@EventHandler
	public void onShulkerDrop(final InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Shulker") && e.getView().getType() == InventoryType.CHEST) {
			Inventory inventory = e.getInventory();
            if (e.getSlot() == e.getWhoClicked().getInventory().getHeldItemSlot()) {
				e.setCancelled(true);
			}
			// Prevents adding a shulker box to a shulker box
			else if (e.getCurrentItem() != null) {
				ItemMeta meta = e.getCurrentItem().hasItemMeta() ? e.getCurrentItem().getItemMeta() : Bukkit.getItemFactory().getItemMeta(e.getCurrentItem().getType());
				if (meta instanceof BlockStateMeta im) {
					if (im.getBlockState() instanceof ShulkerBox) {
						e.setCancelled(true);
					}
				}
			}
		}
	}
}
