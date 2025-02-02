package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.RandomItem;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Random;

public class RandomizerBlockPlace implements Listener {

	public RandomizerBlockPlace(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Randomizes the player's block if it's in their list to randomize.
	 * @param e The event.
	 */
	@EventHandler
	public void onRandomizeBlockPlace(final BlockPlaceEvent e) {
		Player player = e.getPlayer();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		List<RandomItem> randomItemList = aranarthPlayer.getRandomItems();
		if (randomItemList != null && !randomItemList.isEmpty()) {
			PlayerInventory inventory = player.getInventory();
			ItemStack[] contents = inventory.getContents();

			int addedPercentageSum = 0;
			int randomItemCount = 0;

			boolean hasPlacedRandomItem = false;

			// Verifies the player placed one of the random items
			for (RandomItem randomItem : randomItemList) {
				if (e.getItemInHand().isSimilar(randomItem.getItem())) {
					hasPlacedRandomItem = true;
					break;
				}
			}
			if (hasPlacedRandomItem) {
				// Verifies the player has the full list of items
				for (RandomItem randomItem : randomItemList) {
					if (inventory.contains(randomItem.getItem())) {
						randomItemCount++;
					} else {
						// Could get around this logic by adding a boolean to aranarthPlayer
						// Toggle it back to false when the player gets all blocks in the list
						// This way, the player isn't spammed with messages
						String formattedItemName = ChatUtils.getFormattedItemName(randomItem.getItem().getType().name());
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot randomize without &7" + formattedItemName + "!"));
						return;
					}
				}
				if (randomItemCount == randomItemList.size()) {
					Random r = new Random();
					int selectedPercentage = r.nextInt(100) + 1;
					int lowerBracket = 0;
					int higherBracket = 0;

					for (RandomItem randomItem : randomItemList) {
						higherBracket += randomItem.getPercentage();
						// The selected item
						if (selectedPercentage < higherBracket) {
							// Find first stack of the selected item from randomItemList in the player's inventory
							for (int i = 0; i < contents.length; i++) {
								if (contents[i].isSimilar(randomItem.getItem())) {
									int placedSlot = 0;
									if (e.getHand() == EquipmentSlot.HAND) {
										placedSlot = inventory.getHeldItemSlot();
									} else {
										placedSlot = 40; // Hardcoded value of off-hand slot
									}
									ItemStack tempStack = contents[placedSlot].clone();
									contents[placedSlot] = contents[i];
									contents[i] = tempStack;
									player.updateInventory();
								}
							}
						}
					}

				}
			}
		}
	}
}
