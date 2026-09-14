package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;

public class GuiPotions {

	private final Player player;
	private final Inventory initializedGui;

	/**
	 * Creates a GUI for a player's potions.
	 * @param player The player.
	 * @param potionGuiType The potion GUI type. 0 for listing, 1 for adding, -1 for removing.
	 */
	public GuiPotions(Player player, int potionGuiType) {
		this.player = player;
		if (potionGuiType == 1) {
			this.initializedGui = initializeAddGui(player);
		} else if (potionGuiType == -1 ){
			this.initializedGui = initializeRemoveGui(player);
		} else {
			this.initializedGui = initializeListGui(player);
		}
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}

	public Inventory getInitializedGui() {
		return initializedGui;
	}

	public Inventory initializeAddGui(Player player) {
		return Bukkit.getServer().createInventory(player, 54, Lang.getFor(player, "gui.potions.title_add",
				"count", String.valueOf(AranarthUtils.getPlayerStoredPotionNum(player)),
				"max", String.valueOf(AranarthUtils.getMaxPotionNum(player))));
	}

	private Inventory initializeRemoveGui(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		HashMap<ItemStack, Integer> potions = aranarthPlayer.getPotions();

		int size = potions.size();

		// Size is based on which method is used
		// If the amount is a multiple of 9, use a full row
		if (size % 9 != 0) {
			size = ((int) (double) (size / 9) + 1) * 9;
		}
		Inventory inventory = Bukkit.getServer().createInventory(player, size, Lang.getFor(player, "gui.potions.title_remove",
				"count", String.valueOf(AranarthUtils.getPlayerStoredPotionNum(player)),
				"max", String.valueOf(AranarthUtils.getMaxPotionNum(player))));

		HashMap<String, HashMap<ItemStack, Integer>> formattedPotions = AranarthUtils.getPlayerPotionNames(player);
		for (String formattedName : formattedPotions.keySet()) {
			HashMap<ItemStack, Integer> storedPotion = formattedPotions.get(formattedName);
			// Should only have 1 record in it
			for (ItemStack potion : storedPotion.keySet()) {
				ItemStack potionCopy = potion.clone();
				if (potionCopy.hasItemMeta()) {
					ItemMeta meta = potionCopy.getItemMeta();
					meta.setDisplayName(Lang.getFor(player, "gui.potions.name", "name", formattedName, "count", String.valueOf(storedPotion.get(potion))));
					potionCopy.setItemMeta(meta);
					inventory.addItem(potionCopy);
				}
			}
		}

		return inventory;
	}

	private Inventory initializeListGui(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		HashMap<ItemStack, Integer> potions = aranarthPlayer.getPotions();

		int size = potions.size();

		// Size is based on which method is used
		// If the amount is a multiple of 9, use a full row
		if (size % 9 != 0) {
			size = ((int) (double) (size / 9) + 1) * 9;
		}
		Inventory inventory = Bukkit.getServer().createInventory(player, size, Lang.getFor(player, "gui.potions.title_view",
				"count", String.valueOf(AranarthUtils.getPlayerStoredPotionNum(player)),
				"max", String.valueOf(AranarthUtils.getMaxPotionNum(player))));

		HashMap<String, HashMap<ItemStack, Integer>> formattedPotions = AranarthUtils.getPlayerPotionNames(player);
		for (String formattedName : formattedPotions.keySet()) {
			HashMap<ItemStack, Integer> storedPotion = formattedPotions.get(formattedName);
			// Should only have 1 record in it
			for (ItemStack potion : storedPotion.keySet()) {
				ItemStack potionCopy = potion.clone();
				if (potionCopy.hasItemMeta()) {
					ItemMeta meta = potionCopy.getItemMeta();
					meta.setDisplayName(Lang.getFor(player, "gui.potions.name", "name", formattedName, "count", String.valueOf(storedPotion.get(potion))));
					potionCopy.setItemMeta(meta);
					inventory.addItem(potionCopy);
				}
			}
		}

		return inventory;
	}

}
