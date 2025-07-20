package com.aearost.aranarthcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiEnhancedAranarthium {
	private final Player player;
	private final Inventory initializedGui;

	public GuiEnhancedAranarthium(Player player, ItemStack armor, ItemStack ingot, ItemStack result, boolean isArmorFirst) {
		this.player = player;
		this.initializedGui = initializeGui(player, armor, ingot, result, isArmorFirst);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player, ItemStack armor, ItemStack ingot, ItemStack result, boolean isArmorFirst) {
		Inventory gui = Bukkit.getServer().createInventory(player, InventoryType.ANVIL);
		gui.clear();
		if (isArmorFirst) {
			gui.setItem(0, armor);
			gui.setItem(1, ingot);
		} else {
			gui.setItem(0, ingot);
			gui.setItem(1, armor);
		}
		gui.setItem(2, result);
		return gui;
	}

}
