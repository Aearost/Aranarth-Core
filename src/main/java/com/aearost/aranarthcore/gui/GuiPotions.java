package com.aearost.aranarthcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GuiPotions {

	private final Player player;
	private final Inventory initializedGui;

	public GuiPotions(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player) {
		return Bukkit.getServer().createInventory(player, 54, "Potions");
	}

}
