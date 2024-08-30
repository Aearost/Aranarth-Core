package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.ChestItemComparator;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
			if (e.getClickedBlock().getType() == Material.CHEST
					|| e.getClickedBlock().getType() == Material.TRAPPED_CHEST
					|| e.getClickedBlock().getType() == Material.BARREL) {
				if (e.getPlayer().getGameMode() == GameMode.SURVIVAL && e.getPlayer().isSneaking()) {
					e.getPlayer().sendMessage(ChatUtils.chatMessage("&cChest sorting functionality is currently disabled!"));

					/*
					BlockState state = e.getClickedBlock().getState();
					Container container = (Container) state;
					
					ItemStack[] itemsStacked = stackItemsInContainer(container.getInventory().getContents());
					List<ItemStack> sortedList = new ArrayList<>();
					for (ItemStack stackedItem : itemsStacked) {
						if (Objects.nonNull(stackedItem)) {
							if (stackedItem.getType() != Material.AIR) {
								sortedList.add(stackedItem);
							}
						}
					}
					
					ChestItemComparator comparator = new ChestItemComparator();
					sortedList.sort(comparator);
					
					ItemStack[] sortedArray = new ItemStack[sortedList.size()];
					for (int i = 0; i < sortedList.size(); i++) {
						sortedArray[i] = sortedList.get(i);
					}
					container.getInventory().setContents(sortedArray);
					*/
				}
			}
		}
	}
	
	private ItemStack[] stackItemsInContainer(ItemStack[] chestInventory) {
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
								int newAmount = iterated.getAmount() + nextIterated.getAmount();
								if (newAmount > iterated.getMaxStackSize()) {
									newAmount = iterated.getMaxStackSize();
								}
								chestInventory[j].setAmount(nextIterated.getAmount() - (newAmount - iterated.getAmount()));
								chestInventory[i].setAmount(newAmount);
							}
						}
					}
				}
			}
		}
		return chestInventory;
	}
}
