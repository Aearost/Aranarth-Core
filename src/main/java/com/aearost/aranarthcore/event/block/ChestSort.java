package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.ChestSortOrder;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChestSort implements Listener {

	public ChestSort(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles sorting the content of a container.
	 * @param e The event.
	 */
	@EventHandler
	public void onContainerSort(final PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND && e.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (isContainer(e.getClickedBlock())) {
				if (e.getPlayer().getGameMode() == GameMode.SURVIVAL && e.getPlayer().isSneaking()) {
					BlockState state = e.getClickedBlock().getState();
					Container container = (Container) state;

					Inventory inventory = container.getInventory();
					if (inventory.getHolder() instanceof DoubleChest doubleChest) {
                        inventory = doubleChest.getInventory(); // Get the full 54 slot inventory
					}

					List<ItemStack> itemsStacked = stackItemsInContainer(inventory.getContents());
					ItemStack[] stackedArray = new ItemStack[inventory.getContents().length];
					stackedArray = itemsStacked.toArray(stackedArray);
					ItemStack[] sortedItems = sortItems(stackedArray);
					inventory.clear();
					inventory.setContents(sortedItems);

					e.getPlayer().sendMessage(ChatUtils.chatMessage("&7The chest has been sorted!"));
					e.getPlayer().playSound(e.getPlayer(), Sound.UI_STONECUTTER_TAKE_RESULT, 1F, 1F);
				}
			}
		}
	}
	
	private List<ItemStack> stackItemsInContainer(ItemStack[] chestInventory) {
		List<ItemStack> stackedList = new ArrayList<>();
		// Handles filling up all slots to the maximum amount
		for (int i = 0; i < chestInventory.length; i++) {
			ItemStack iterated = chestInventory[i];
			if (iterated != null) {
				// If it is not a full stack, check for other slots for the same item
				if (iterated.getMaxStackSize() > iterated.getAmount()) {
					for (int j = i + 1; j < chestInventory.length; j++) {
						ItemStack nextIterated = chestInventory[j];
						if (nextIterated != null) {
							if (nextIterated.isSimilar(iterated)) {
								// Handle adjusting the inventory
								int newI = iterated.getAmount() + nextIterated.getAmount();
								int newJ = 0;
								if (newI > iterated.getMaxStackSize()) {
									newJ = newI - iterated.getMaxStackSize();
									newI = iterated.getMaxStackSize();
								}
								chestInventory[i].setAmount(newI);
								chestInventory[j].setAmount(newJ);
							}
						}
					}
				}
				if (chestInventory[i].getType() == Material.AIR) {
					continue;
				}
				stackedList.add(chestInventory[i]);
			}
		}
		return stackedList;
	}

	private ItemStack[] sortItems(ItemStack[] stackedItems) {
		List<ItemStack> sortedItems = new ArrayList<>();
		List<ItemStack> unsortedItems = new ArrayList<>();
		ChestSortOrder[] sortOrder = ChestSortOrder.values();

		// Iterates through sort list
        for (ChestSortOrder item : sortOrder) {
            // Iterates through stacked chest contents
            for (ItemStack is : stackedItems) {
				if (is != null) {
					// If the iterated is the same sort list item
					if (item.name().equals(is.getType().name())) {
						sortedItems.add(is);
					}
					// If not, see if iterated is in list of predefined sort items
					else {
						try {
							ChestSortOrder.valueOf(is.getType().name());
						} catch (IllegalArgumentException e) {
							if (!unsortedItems.contains(is)) {
								unsortedItems.add(is);
							}
						}
					}
				}
            }
        }
		// Adds all unsorted/non-defined items to the end of the list
        sortedItems.addAll(unsortedItems);

		ItemStack[] sortedItemsAsArray = new ItemStack[sortedItems.size()];
		for (int i = 0; i < sortedItems.size(); i++) {
			sortedItemsAsArray[i] = sortedItems.get(i);
		}
		return sortedItemsAsArray;
	}

	private boolean isContainer(Block block) {
		return block.getType() == Material.CHEST
				|| block.getType() == Material.TRAPPED_CHEST
				|| block.getType() == Material.BARREL
				|| block.getType().name().endsWith("SHULKER_BOX");
	}
}
