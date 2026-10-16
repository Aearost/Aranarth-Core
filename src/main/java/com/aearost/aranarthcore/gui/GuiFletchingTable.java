package com.aearost.aranarthcore.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiFletchingTable {

	private final Player player;
	private final Inventory initializedGui;

	public GuiFletchingTable(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player, null);
	}

	public GuiFletchingTable(Player player, ItemStack[] inventory) {
		this.player = player;
		this.initializedGui = initializeGui(player, inventory);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player, ItemStack[] inventory) {
        Inventory gui = GuiHolder.create(player, InventoryType.WORKBENCH, "Fletching Table");
		// Behaves like the vanilla block, so items can be dragged across its slots
		GuiHolder holder = GuiHolder.of(gui);
		if (holder != null) {
			holder.allowDrag();
		}
		ItemStack blank = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		gui.setItem(1, blank);
		gui.setItem(3, blank);
		gui.setItem(4, blank);
		gui.setItem(6, blank);
		gui.setItem(7, blank);
		gui.setItem(9, blank);
		return gui;
	}

}
