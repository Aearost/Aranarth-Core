package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class GuiPotions {

	private final Player player;
	private final Inventory initializedGui;

	public GuiPotions(Player player, boolean isAdding) {
		this.player = player;
		if (isAdding) {
			this.initializedGui = initializeAddGui(player);
		} else {
			this.initializedGui = initializeRemoveGui(player);
		}
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}

	private Inventory initializeAddGui(Player player) {
		return Bukkit.getServer().createInventory(player, 54, "Potions");
	}

	private Inventory initializeRemoveGui(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		HashMap<ItemStack, Integer> potions = aranarthPlayer.getPotions();

//		HashMap<ItemStack, Integer> potionsAndAmounts = AranarthUtils.getPotionsAndAmounts(player);
		int size = potions.size();

		// Size is based on which method is used
		// If the amount is a multiple of 9, use a full row
		if (size % 9 != 0) {
			size = ((int) (double) (size / 9) + 1) * 9;
		}

		Inventory inventory = Bukkit.getServer().createInventory(player, size, "Remove Potions");
		for (ItemStack storedPotion : potions.keySet()) {
			inventory.addItem(storedPotion);
		}
		return inventory;
	}

}
