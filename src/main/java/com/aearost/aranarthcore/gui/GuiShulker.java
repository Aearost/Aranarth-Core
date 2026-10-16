package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
		Inventory gui = GuiHolder.create(player, 27, Lang.getFor(player, "gui.shulker.title"));
		// Remembers exactly which shulker was opened so the contents can only be written back into it
		GuiHolder.of(gui).setData(new OpenedShulker(player.getInventory().getHeldItemSlot(), player.getInventory().getItemInMainHand().clone()));
		GuiHolder.of(gui).allowDrag();
		for (int i = 0; i < shulkerInventory.getSize(); i++) {
			gui.setItem(i, shulkerInventory.getItem(i));
		}
		return gui;
	}

	/**
	 * The hotbar slot and a copy of the shulker box as it was when the GUI was opened.
	 */
	public record OpenedShulker(int slot, ItemStack original) {
	}

}
