package com.aearost.aranarthcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class GuiFletchingTable {

	private final Player player;
	private final Inventory initializedGui;

	public GuiFletchingTable(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player) {
        return Bukkit.getServer().createInventory(player, InventoryType.WORKBENCH, "Fletching Table");
	}

}
