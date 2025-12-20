package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiFletchingTable;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.MenuType;


/**
 * Handles the teleport logic for the different kinds of tables.
 */
public class GuiTablesClick {
	public void execute(InventoryClickEvent e) {
		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		if (e.getClickedInventory().getType() != InventoryType.PLAYER) {
			if (e.getWhoClicked() instanceof Player player) {
				// Crafting Table
				if (e.getSlot() == 0) {
					e.setCancelled(true);
					player.closeInventory();
					MenuType.CRAFTING.builder()
							.checkReachable(false)
							.build(player)
							.open();
				}
				// Fletching Table
				else if (e.getSlot() == 1) {
					e.setCancelled(true);
					player.closeInventory();
					GuiFletchingTable gui = new GuiFletchingTable(player);
					gui.openGui();
				}
				// Smithing Table
				else if (e.getSlot() == 2) {
					e.setCancelled(true);
					player.closeInventory();
					MenuType.SMITHING.builder()
							.checkReachable(false)
							.build(player)
							.open();
				}
				// Cartography Table
				else if (e.getSlot() == 3) {
					e.setCancelled(true);
					player.closeInventory();
					MenuType.CARTOGRAPHY_TABLE.builder()
							.checkReachable(false)
							.build(player)
							.open();
				}
				// Loom
				else if (e.getSlot() == 4) {
					e.setCancelled(true);
					player.closeInventory();
					MenuType.LOOM.builder()
							.checkReachable(false)
							.build(player)
							.open();
				}
				// Anvil
				else if (e.getSlot() == 5) {
					e.setCancelled(true);
					player.closeInventory();
					MenuType.ANVIL.builder()
							.checkReachable(false)
							.build(player)
							.open();
				}
				// Grindstone
				else if (e.getSlot() == 6) {
					e.setCancelled(true);
					player.closeInventory();
					MenuType.GRINDSTONE.builder()
							.checkReachable(false)
							.build(player)
							.open();
				}
				// Stonecutter
				else if (e.getSlot() == 7) {
					e.setCancelled(true);
					player.closeInventory();
					MenuType.STONECUTTER.builder()
							.checkReachable(false)
							.build(player)
							.open();
				}
			}
		}
	}

}
