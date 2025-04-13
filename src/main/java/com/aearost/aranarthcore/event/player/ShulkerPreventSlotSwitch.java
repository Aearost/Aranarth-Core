package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ShulkerPreventSlotSwitch implements Listener {

	public ShulkerPreventSlotSwitch(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents the player from putting a shulker box into a shulker box.
	 * @param e The event.
	 */
	@EventHandler
	public void onShulkerSlotSwitch(final InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player player) {
			if (e.getClick() == ClickType.NUMBER_KEY) {
				// If it is out of the hotbar
				if (e.getSlot() > 8) {
					ItemStack itemInHotbarSlot = player.getInventory().getContents()[e.getHotbarButton()];
					if (itemInHotbarSlot != null) {
						if (itemInHotbarSlot.getType().name().endsWith("SHULKER_BOX")) {
							e.setCancelled(true);
						}
					}
				}
			}
		}
	}
}
