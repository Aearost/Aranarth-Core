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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

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
			// Removing potion(s) by shift-clicking from potion inventory into player inventory
			else if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
				// Regular shift-click: no item on cursor
				if (e.getCursor().getType() == Material.AIR) {
					boolean hasSpaceInPlayerInventory = false;
					// Do not consider armor slots
					for (int i = 0; i < 36; i++) {
						ItemStack item = player.getInventory().getItem(i);
						if (item == null || item.getType() == Material.AIR) {
							hasSpaceInPlayerInventory = true;
							break;
						}
					}

					if (hasSpaceInPlayerInventory) {
						String potionStats = "(" + (AranarthUtils.getPlayerStoredPotionNum(player) - 1 + filledPotionInventorySlotNum) + "/" + AranarthUtils.getMaxPotionNum(player) + ")";
						// If there are too many potions in the pouch
						if (AranarthUtils.getPlayerStoredPotionNum(player) + 1 > AranarthUtils.getMaxPotionNum(player)) {
							potionStats = "&c" + potionStats;
						}

						removePotion(player, top, e.getCurrentItem(), e.getSlot(), false);
						e.getView().setTitle("Add Potions " + potionStats);
					} else {
						e.setCancelled(true);
					}
				}
				// Shift double-click with cursor item: move all matching potions from chest into player inventory
				else {
					e.setCancelled(true);

					ItemStack clickedItem = e.getCurrentItem();
					if (clickedItem == null || !isValidPotion(clickedItem)) {
						return;
					}

					Inventory newInventory = new GuiPotions(player, 1).getInitializedGui();
					for (int i = 0; i < top.getContents().length; i++) {
						if (top.getContents()[i] != null) {
							newInventory.setItem(i, top.getContents()[i].clone());
						}
					}

					Inventory bottom = e.getView().getBottomInventory();
					int removedCount = 0;

					for (int i = 0; i < top.getSize(); i++) {
						ItemStack item = top.getItem(i);
						if (item != null && item.isSimilar(clickedItem) && isValidPotion(item)) {
							if (bottom.firstEmpty() == -1) {
								break;
							}

							bottom.addItem(item.clone());
							newInventory.setItem(i, null);
							removedCount++;
						}
					}

					if (removedCount > 0) {
						String potionStats = "(" + (AranarthUtils.getPlayerStoredPotionNum(player) - removedCount + filledPotionInventorySlotNum) + "/" + AranarthUtils.getMaxPotionNum(player) + ")";
						if (AranarthUtils.getPlayerStoredPotionNum(player) + 1 > AranarthUtils.getMaxPotionNum(player)) {
							potionStats = "&c" + potionStats;
						}

						ItemStack cursorCopy = e.getCursor().clone();
						player.setItemOnCursor(null);
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
						aranarthPlayer.setAddingPotions(true);
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						player.openInventory(newInventory);
						player.setItemOnCursor(cursorCopy);
						aranarthPlayer.setAddingPotions(false);
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

						e.getView().setTitle("Add Potions " + potionStats);
					}
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
						boolean wasAdded = addPotion(player, top, item, potionGuiSlot, true);
						if (wasAdded) {
							e.getView().getBottomInventory().setItem(hotbarSlot, null);
							e.getView().setTitle("Add Potions " + potionStats);
						} else {
							e.setCancelled(true);
						}
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
			// Adding potion(s) by shift-clicking into the potion inventory
			if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
				if (hasSpaceInPotionGui) {
					// Regular shift-click: no item on cursor
					if (e.getCursor().getType() == Material.AIR) {
						String potionStats = "(" + (AranarthUtils.getPlayerStoredPotionNum(player) + 1 + filledPotionInventorySlotNum) + "/" + AranarthUtils.getMaxPotionNum(player) + ")";
						// If there are too many potions in the pouch
						if (AranarthUtils.getPlayerStoredPotionNum(player) + 1 > AranarthUtils.getMaxPotionNum(player)) {
							potionStats = "&c" + potionStats;
						}

						boolean wasAdded = addPotion(player, top, e.getCurrentItem(), e.getSlot(), false);
						if (wasAdded) {
							e.getView().setTitle("Add Potions " + potionStats);
						} else {
							e.setCancelled(true);
						}
					}
					// Shift double-click with cursor item: move all matching potions from player inventory into chest
					else {
						e.setCancelled(true);

						ItemStack clickedItem = e.getCurrentItem();
						if (clickedItem == null || !isValidPotion(clickedItem)) {
							return;
						}

						Inventory newInventory = new GuiPotions(player, 1).getInitializedGui();
						for (int i = 0; i < top.getContents().length; i++) {
							if (top.getContents()[i] != null) {
								newInventory.setItem(i, top.getContents()[i].clone());
							}
						}

						Inventory bottom = e.getView().getBottomInventory();
						int addedCount = 0;

						for (int i = 0; i < bottom.getSize(); i++) {
							ItemStack item = bottom.getItem(i);
							if (item != null && item.isSimilar(clickedItem) && isValidPotion(item)) {
								boolean hasSpace = false;
								for (ItemStack topItem : newInventory) {
									if (topItem == null) {
										hasSpace = true;
										break;
									}
								}
								if (!hasSpace) {
									break;
								}

								newInventory.addItem(item.clone());
								bottom.setItem(i, null);
								addedCount++;
							}
						}

						if (addedCount > 0) {
							String potionStats = "(" + (AranarthUtils.getPlayerStoredPotionNum(player) + addedCount + filledPotionInventorySlotNum) + "/" + AranarthUtils.getMaxPotionNum(player) + ")";
							if (AranarthUtils.getPlayerStoredPotionNum(player) + 1 > AranarthUtils.getMaxPotionNum(player)) {
								potionStats = "&c" + potionStats;
							}

							ItemStack cursorCopy = e.getCursor().clone();
							player.setItemOnCursor(null);
							AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
							aranarthPlayer.setAddingPotions(true);
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
							player.openInventory(newInventory);
							player.setItemOnCursor(cursorCopy);
							aranarthPlayer.setAddingPotions(false);
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

							e.getView().setTitle("Add Potions " + potionStats);
						}
					}
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
	private boolean addPotion(Player player, Inventory top, ItemStack potion, int slot, boolean fromManualClick) {
		if (isValidPotion(potion)) {
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
			aranarthPlayer.setAddingPotions(true);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			player.openInventory(newInventory);

			aranarthPlayer.setAddingPotions(false);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			return true;
		}
		return false;
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
		if (isValidPotion(potion)) {
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
			aranarthPlayer.setAddingPotions(true);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			player.openInventory(newInventory);

			// Must be placed after the inventory is opened
			if (fromManualClick) {
				player.setItemOnCursor(potionCopy);
			}

			aranarthPlayer.setAddingPotions(false);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		}
	}

	/**
	 * Provides confirmation whether the item is a valid potion or not.
	 * @param item The item.
	 * @return Confirmation whether the item is a valid potion or not.
	 */
	private boolean isValidPotion(ItemStack item) {
		if (item.getType() == Material.POTION
				|| item.getType() == Material.SPLASH_POTION
				|| item.getType() == Material.LINGERING_POTION) {
			PotionMeta meta = (PotionMeta) item.getItemMeta();
			// Prevent potions without effects from being added
			if (meta.getBasePotionType() == PotionType.AWKWARD || meta.getBasePotionType() == PotionType.MUNDANE
					|| meta.getBasePotionType() == PotionType.THICK || meta.getBasePotionType() == PotionType.WATER) {
				// Allows mcMMO potions
				if (!meta.getCustomEffects().isEmpty()) {
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}

}
