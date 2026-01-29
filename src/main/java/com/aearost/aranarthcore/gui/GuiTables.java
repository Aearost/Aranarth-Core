package com.aearost.aranarthcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiTables {

	private final Player player;
	private final Inventory initializedGui;

	public GuiTables(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 9, "Tables");

		ItemStack crafting = new ItemStack(Material.CRAFTING_TABLE);
		gui.setItem(0, crafting);
		ItemStack fletching = new ItemStack(Material.FLETCHING_TABLE);
		gui.setItem(1, fletching);
		ItemStack smithing = new ItemStack(Material.SMITHING_TABLE);
		gui.setItem(2, smithing);
		ItemStack cartography = new ItemStack(Material.CARTOGRAPHY_TABLE);
		gui.setItem(3, cartography);
		ItemStack loom = new ItemStack(Material.LOOM);
		gui.setItem(4, loom);
		ItemStack anvil = new ItemStack(Material.ANVIL);
		gui.setItem(5, anvil);
		ItemStack grindstone = new ItemStack(Material.GRINDSTONE);
		gui.setItem(6, grindstone);
		ItemStack stonecutter = new ItemStack(Material.STONECUTTER);
		gui.setItem(7, stonecutter);

		return gui;
	}

}
