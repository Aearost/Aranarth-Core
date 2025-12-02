package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiPotions;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Dynamically updates the number of potions that will be in the player's pouch as they add potions.
 */
public class GuiPotionAdd {
	public void execute(InventoryClickEvent e) {
		if (e.getClickedInventory() == null) {
			return;
		}

		Player player = (Player) e.getWhoClicked();
		int filledPotionInventorySlotNum = 0;
		Inventory top = e.getView().getTopInventory();
		for (ItemStack item : top) {
			if (item != null) {
				filledPotionInventorySlotNum++;
			}
		}
		boolean hasSpaceInPotionGui = filledPotionInventorySlotNum < 54;

		// Clicking in the potions inventory
		if (e.getClickedInventory().getType() == InventoryType.CHEST) {
			// Adding potion by placing in potions inventory
			if (e.getAction() == InventoryAction.PLACE_ALL) {
				// If the inventory is full, do nothing different
				if (hasSpaceInPotionGui) {
					String potionStats = "(" + (AranarthUtils.getPlayerStoredPotionNum(player) + 1 + filledPotionInventorySlotNum) + "/" + AranarthUtils.getMaxPotionNum(player) + ")";
					// If there are too many potions in the pouch
					if (AranarthUtils.getPlayerStoredPotionNum(player) + 1 > AranarthUtils.getMaxPotionNum(player)) {
						potionStats = "&c" + potionStats;
					}

					ItemStack cursorCopy = e.getCursor().clone();
					player.setItemOnCursor(null);
					addPotion(player, top, cursorCopy, e.getSlot(), true);
					e.getView().setTitle("Add Potions " + potionStats);
				}
			}
			// Removing potion by shift-clicking it into player inventory
			else if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
				if (hasSpaceInPotionGui) {
					String potionStats = "(" + (AranarthUtils.getPlayerStoredPotionNum(player) - 1 + filledPotionInventorySlotNum) + "/" + AranarthUtils.getMaxPotionNum(player) + ")";
					// If there are too many potions in the pouch
					if (AranarthUtils.getPlayerStoredPotionNum(player) + 1 > AranarthUtils.getMaxPotionNum(player)) {
						potionStats = "&c" + potionStats;
					}

					removePotion(player, top, e.getCurrentItem(), e.getSlot(), false);
					e.getView().setTitle("Add Potions " + potionStats);
				}
			}
			// Can be both adding AND removing
			else if (e.getAction() == InventoryAction.HOTBAR_SWAP) {
				int potionGuiSlot = e.getSlot();
				int hotbarSlot = e.getHotbarButton();

				// Adding potion
				if (e.getCurrentItem() == null) {
					if (hasSpaceInPotionGui) {
						String potionStats = "(" + (AranarthUtils.getPlayerStoredPotionNum(player) + 1 + filledPotionInventorySlotNum) + "/" + AranarthUtils.getMaxPotionNum(player) + ")";
						// If there are too many potions in the pouch
						if (AranarthUtils.getPlayerStoredPotionNum(player) + 1 > AranarthUtils.getMaxPotionNum(player)) {
							potionStats = "&c" + potionStats;
						}

						ItemStack item = e.getView().getBottomInventory().getItem(e.getHotbarButton()).clone();
						addPotion(player, top, item, potionGuiSlot, true);
						e.getView().getBottomInventory().setItem(hotbarSlot, null);
						e.getView().setTitle("Add Potions " + potionStats);
					}
				}
				// Removing potion
				else {
					String potionStats = "(" + (AranarthUtils.getPlayerStoredPotionNum(player) - 1 + filledPotionInventorySlotNum) + "/" + AranarthUtils.getMaxPotionNum(player) + ")";
					// If there are too many potions in the pouch
					if (AranarthUtils.getPlayerStoredPotionNum(player) + 1 > AranarthUtils.getMaxPotionNum(player)) {
						potionStats = "&c" + potionStats;
					}

					ItemStack item = top.getItem(e.getSlot()).clone();
					removePotion(player, top, item, e.getSlot(), true);
					top.setItem(e.getSlot(), null);
					player.setItemOnCursor(null); // To negate it being populated in removePotion()
					player.getInventory().setItem(e.getHotbarButton(), item);
					e.getView().setTitle("Add Potions " + potionStats);
				}
			}
			// Removing potions by taking from potions inventory
			else if (e.getAction() == InventoryAction.PICKUP_ALL) {
				String potionStats = "(" + (AranarthUtils.getPlayerStoredPotionNum(player) - 1 + filledPotionInventorySlotNum) + "/" + AranarthUtils.getMaxPotionNum(player) + ")";
				// If there are too many potions in the pouch
				if (AranarthUtils.getPlayerStoredPotionNum(player) + 1 > AranarthUtils.getMaxPotionNum(player)) {
					potionStats = "&c" + potionStats;
				}

				removePotion(player, top, e.getCurrentItem(), e.getSlot(), true);
				e.getView().setTitle("Add Potions " + potionStats);
			}
		} else {
			// Adding potion by shift-clicking it into the potion inventory
			if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
				if (hasSpaceInPotionGui) {
					String potionStats = "(" + (AranarthUtils.getPlayerStoredPotionNum(player) + 1 + filledPotionInventorySlotNum) + "/" + AranarthUtils.getMaxPotionNum(player) + ")";
					// If there are too many potions in the pouch
					if (AranarthUtils.getPlayerStoredPotionNum(player) + 1 > AranarthUtils.getMaxPotionNum(player)) {
						potionStats = "&c" + potionStats;
					}

					addPotion(player, top, e.getCurrentItem(), e.getSlot(), false);
					e.getView().setTitle("Add Potions " + potionStats);
				} else {

				}
			}
		}
	}

	/**
	 * Adds the potion and updates the view title.
	 * @param player The player adding the potion.
	 * @param top The potion portion of the inventory.
	 * @param potion The potion being added.
	 * @param slot The slot that the potion will be going into.
	 * @param fromManualClick Whether the click was done manually.
	 */
	private void addPotion(Player player, Inventory top, ItemStack potion, int slot, boolean fromManualClick) {
		if (isPotion(potion)) {
			ItemStack potionCopy = potion.clone();

			Inventory newInventory = new GuiPotions(player, 1).getInitializedGui();
			for (int i = 0; i < top.getContents().length; i++) {
				if (top.getContents()[i] != null) {
					newInventory.setItem(i, top.getContents()[i].clone());
				}
			}

			if (fromManualClick) {
				newInventory.setItem(slot, potionCopy);
			} else {
				newInventory.addItem(potionCopy);
				player.getInventory().setItem(slot, null);
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			aranarthPlayer.setIsAddingPotions(true);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			player.openInventory(newInventory);

			aranarthPlayer.setIsAddingPotions(false);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		}
	}

	/**
	 * Removes the potion and updates the view title.
	 * @param player The player removing the potion.
	 * @param top The potion portion of the inventory.
	 * @param potion The potion being removed.
	 * @param slot The slot that the potion will be taken from.
	 * @param fromManualClick Whether the click was done manually.
	 */
	private void removePotion(Player player, Inventory top, ItemStack potion, int slot, boolean fromManualClick) {
		if (isPotion(potion)) {
			ItemStack potionCopy = potion.clone();

			Inventory newInventory = new GuiPotions(player, 1).getInitializedGui();
			for (int i = 0; i < top.getContents().length; i++) {
				if (top.getContents()[i] != null) {
					newInventory.setItem(i, top.getContents()[i].clone());
				}
			}

			if (fromManualClick) {
				newInventory.setItem(slot, null);
			} else {
				newInventory.setItem(slot, null);
				player.getInventory().addItem(potionCopy);
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			aranarthPlayer.setIsAddingPotions(true);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			player.openInventory(newInventory);

			// Must be placed after the inventory is opened
			if (fromManualClick) {
				player.setItemOnCursor(potionCopy);
			}

			aranarthPlayer.setIsAddingPotions(false);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		}
	}

	/**
	 * Provides confirmation whether the item is a potion or not.
	 * @param item The item.
	 * @return Confirmation whether the item is a potion or not.
	 */
	private boolean isPotion(ItemStack item) {
		return (item.getType() == Material.POTION
				|| item.getType() == Material.SPLASH_POTION
				|| item.getType() == Material.LINGERING_POTION);
	}
}
