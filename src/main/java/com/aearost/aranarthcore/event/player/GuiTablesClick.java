package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiFletchingTable;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

/**
 * Handles the teleport logic for the different kinds of tables.
 */
public class GuiTablesClick {
	public void execute(InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Tables")) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}

			if (e.getWhoClicked() instanceof Player player) {
				// Crafting Table
				if (e.getSlot() == 0) {
					e.setCancelled(true);
					Inventory gui = Bukkit.getServer().createInventory(player, InventoryType.WORKBENCH);
					player.closeInventory();
					player.openInventory(gui);
				}
				// Fletching Table
				else if (e.getSlot() == 1) {
					e.setCancelled(true);
					GuiFletchingTable gui = new GuiFletchingTable(player);
					gui.openGui();
				}
				// Smithing Table
				else if (e.getSlot() == 2) {
					e.setCancelled(true);
					Inventory gui = Bukkit.getServer().createInventory(player, InventoryType.SMITHING);
					player.closeInventory();
					player.openInventory(gui);
				}
				// Cartography Table
				else if (e.getSlot() == 3) {
					e.setCancelled(true);
					Inventory gui = Bukkit.getServer().createInventory(player, InventoryType.CARTOGRAPHY);
					player.closeInventory();
					player.openInventory(gui);
				}
				// Loom
				else if (e.getSlot() == 4) {
					e.setCancelled(true);
					Inventory gui = Bukkit.getServer().createInventory(player, InventoryType.LOOM);
					player.closeInventory();
					player.openInventory(gui);
				}
				// Anvil
				else if (e.getSlot() == 5) {
					e.setCancelled(true);
					Inventory gui = Bukkit.getServer().createInventory(player, InventoryType.ANVIL);
					player.closeInventory();
					player.openInventory(gui);
				}
				// Grindstone
				else if (e.getSlot() == 6) {
					e.setCancelled(true);
					Inventory gui = Bukkit.getServer().createInventory(player, InventoryType.GRINDSTONE);
					player.closeInventory();
					player.openInventory(gui);
				}
				// Stonecutter
				else if (e.getSlot() == 7) {
					e.setCancelled(true);
					Inventory gui = Bukkit.getServer().createInventory(player, InventoryType.STONECUTTER);
					player.closeInventory();
					player.openInventory(gui);
				}
			}
		}
	}

}
