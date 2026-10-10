package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

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
		Inventory gui = Bukkit.getServer().createInventory(player, 9, Lang.getFor(player, "gui.tables.title"));

		gui.setItem(0, createTableItem(player, new ItemStack(Material.CRAFTING_TABLE), "/craft, /workbench, /wb, /ct"));
		gui.setItem(1, createTableItem(player, new ItemStack(Material.FLETCHING_TABLE), "/fletching"));
		gui.setItem(2, createTableItem(player, new ItemStack(Material.SMITHING_TABLE), "/smithing"));
		gui.setItem(3, createTableItem(player, new ItemStack(Material.CARTOGRAPHY_TABLE), "/cartography"));
		gui.setItem(4, createTableItem(player, new ItemStack(Material.LOOM), "/loom"));
		gui.setItem(5, createTableItem(player, new ItemStack(Material.ANVIL), "/anvil"));
		gui.setItem(6, createTableItem(player, new ItemStack(Material.GRINDSTONE), "/grindstone"));
		gui.setItem(7, createTableItem(player, new ItemStack(Material.STONECUTTER), "/stonecutter"));
		ItemStack sawmill = new ItemStack(Material.OAK_LOG);
		ItemMeta sawmillMeta = sawmill.getItemMeta();
		sawmillMeta.setDisplayName("Sawmill");
		sawmill.setItemMeta(sawmillMeta);
		gui.setItem(8, createTableItem(player, sawmill, "/sawmill"));

		return gui;
	}

	/**
	 * Adds the lore displaying the command(s) used to open the table directly.
	 */
	private ItemStack createTableItem(Player player, ItemStack item, String command) {
		ItemMeta meta = item.getItemMeta();
		meta.setLore(List.of(Lang.getFor(player, "gui.tables.command", "command", command)));
		item.setItemMeta(meta);
		return item;
	}

}
