package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class GuiTrash {

	private final Player player;
	private final Inventory initializedGui;

	public GuiTrash(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}

	private Inventory initializeGui(Player player) {
		List<Home> homes = AranarthUtils.getHomepads();
		int totalHomesOnPage = homes.size();
		
		return Bukkit.getServer().createInventory(player, 9, "Trash");
	}

}
