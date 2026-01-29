package com.aearost.aranarthcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GuiShulker {

	private final Player player;
	private final Inventory initializedGui;

	public GuiShulker(Player player, Inventory shulkerInventory) {
		this.player = player;
		this.initializedGui = initializeGui(player, shulkerInventory);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player, Inventory shulkerInventory) {
		Inventory gui = Bukkit.getServer().createInventory(player, 27, "Held Shulker");
		for (int i = 0; i < shulkerInventory.getSize(); i++) {
			gui.setItem(i, shulkerInventory.getItem(i));
		}
		return gui;
	}

}
