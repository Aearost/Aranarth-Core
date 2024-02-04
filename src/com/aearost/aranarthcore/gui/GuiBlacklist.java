package com.aearost.aranarthcore.gui;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.utils.AranarthUtils;

public class GuiBlacklist {

	private Player player;
	private Inventory initializedGui;

	public GuiBlacklist(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}
	
	public Inventory getInitializedGui() {
		return initializedGui;
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player) {
		
		Inventory gui = Bukkit.getServer().createInventory(player, 9, "Blacklist");
		List<ItemStack> blacklistedItems = AranarthUtils.getBlacklistedItems(player.getUniqueId());
		for (int i = 0; i < blacklistedItems.size(); i++) {
			ItemStack blacklistedItem = blacklistedItems.get(i);
			gui.setItem(i, blacklistedItem);
		}

		return gui;
	}

}
