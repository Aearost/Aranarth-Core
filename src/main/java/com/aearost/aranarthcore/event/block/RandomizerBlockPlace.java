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
			if (aranarthPlayer.getIsRandomizing()) {
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
                        for (ItemStack content : contents) {
							if (content != null) {
								if (content.isSimilar(randomItem.getItem())) {
									randomItemCount++;
									// Avoid duplicates
									break;
								}
							}
                        }
					}
					if (randomItemCount == randomItemList.size()) {
						Random r = new Random();
						int selectedPercentage = r.nextInt(100) + 1;
						int lowerBracket = 0;
						int higherBracket = 0;

						for (RandomItem randomItem : randomItemList) {
//							Bukkit.getLogger().info("random: " + randomItem.getItem().getType().name());
							higherBracket += randomItem.getPercentage();
//							Bukkit.getLogger().info("Selected: " + selectedPercentage);
//							Bukkit.getLogger().info("lowerBracket: " + lowerBracket);
//							Bukkit.getLogger().info("higherBracket: " + higherBracket);

							// The selected item
							if (selectedPercentage < higherBracket) {
								// Find first stack of the selected item from randomItemList in the player's inventory
								for (int i = 0; i < contents.length; i++) {
									if (contents[i] != null) {
										if (contents[i].isSimilar(randomItem.getItem())) {
											int placedSlot = 0;
											if (e.getHand() == EquipmentSlot.HAND) {
												placedSlot = inventory.getHeldItemSlot();
											} else {
												placedSlot = 40; // Hardcoded value of off-hand slot
											}
//											Bukkit.getLogger().info("Placed slot: " + placedSlot);
//											Bukkit.getLogger().info("i: " + i);
//											Bukkit.getLogger().info("A: " + (placedSlot != i));
//											Bukkit.getLogger().info("B: " + (!contents[placedSlot].isSimilar(contents[i])));
											/*if (placedSlot == i || contents[placedSlot].isSimilar(contents[i])) {
												Bukkit.getLogger().info("C");
												continue;
											}*/
											// Temporary backup and update quantity
											ItemStack tempStack = contents[placedSlot].clone();
											tempStack.setAmount(contents[placedSlot].getAmount() - 1);

											// Exclude logic if and only if the placed item is the same as the random one
											// Auto-replenish functionality should catch this
											if (contents[placedSlot].isSimilar(randomItem.getItem()) && tempStack.getAmount() == 0) {
												Bukkit.getLogger().info("Last of stack and same item");
												continue;
											}
											contents[placedSlot] = contents[i];
											contents[i] = tempStack;
											player.getInventory().setContents(contents);
											player.updateInventory();
											if (aranarthPlayer.getIsMissingItemMessageSent()) {
												aranarthPlayer.setIsMissingItemMessageSent(!aranarthPlayer.getIsMissingItemMessageSent());
												AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
											}
											return;
										}
									}
								}
							}
						}
					} else {
						if (!aranarthPlayer.getIsMissingItemMessageSent()) {
							aranarthPlayer.setIsMissingItemMessageSent(!aranarthPlayer.getIsMissingItemMessageSent());
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
							player.sendMessage(ChatUtils.chatMessage("&cYou are missing one or more items from your pattern!"));
                        }
					}
				}
			}
		}
	}
}
