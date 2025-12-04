package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.items.SugarcaneBlock;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Automatically compresses a player's entire inventory, and attempts to compress the picked up item as well.
 */
public class CompressorItemPickup {

	public void execute(EntityPickupItemEvent e) {
		if (e.getEntity() instanceof Player player) {
			if (!player.hasPermission("aranarth.compress")) {
				return;
			}

			// Only attempts to compress if the item being picked up is compressible
			if (!isCompressible(e.getItem().getItemStack())) {
				return;
			}

			e.setCancelled(true);
			ItemStack pickupClone = e.getItem().getItemStack().clone();

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			boolean isIncludingShulkers = (player.hasPermission("aranarth.shulker"));
			// Identifies all contents of compressible items in the player's inventory
			HashMap<Material, List<ItemStack>> compressibleItems = new HashMap<>();
			for (ItemStack inventoryItem : player.getInventory().getContents()) {
				// Ignore if it is empty
				if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
					continue;
				}

				int result = AranarthUtils.isBlacklistingItem(aranarthPlayer, inventoryItem);
				if (result >= 0) {
					continue;
				}

				// Do recursive check for shulker box contents
				if (inventoryItem.hasItemMeta()) {
					if (inventoryItem.getItemMeta() instanceof BlockStateMeta im) {
						if (im.getBlockState() instanceof ShulkerBox shulker) {
							if (isIncludingShulkers) {
								for (ItemStack shulkerItem : shulker.getInventory().getContents()) {
									// Ignore if it is empty
									if (shulkerItem == null || shulkerItem.getType() == Material.AIR) {
										continue;
									}

									int shulkerResult = AranarthUtils.isBlacklistingItem(aranarthPlayer, shulkerItem);
									if (shulkerResult >= 0) {
										continue;
									}

									if (isCompressible(shulkerItem)) {
										Material type = shulkerItem.getType();
										if (compressibleItems.containsKey(type)) {
											List<ItemStack> stacksOfItems = compressibleItems.get(type);
											stacksOfItems.add(shulkerItem.clone());
											shulkerItem.setAmount(0);
											compressibleItems.put(type, stacksOfItems);
										} else {
											List<ItemStack> stacksOfItems = new ArrayList<>();
											stacksOfItems.add(shulkerItem.clone());
											shulkerItem.setAmount(0);
											compressibleItems.put(type, stacksOfItems);
										}
									}
								}
								im.setBlockState(shulker);
								inventoryItem.setItemMeta(im);
							}
						}
					}
				}
				// Normal item, not a shulker box
				else {
					if (isCompressible(inventoryItem)) {
						Material type = inventoryItem.getType();
						if (compressibleItems.containsKey(type)) {
							List<ItemStack> stacksOfItems = compressibleItems.get(type);
							stacksOfItems.add(inventoryItem.clone());
							inventoryItem.setAmount(0);
							compressibleItems.put(type, stacksOfItems);
						} else {
							List<ItemStack> stacksOfItems = new ArrayList<>();
							stacksOfItems.add(inventoryItem.clone());
							inventoryItem.setAmount(0);
							compressibleItems.put(type, stacksOfItems);
						}
					}
				}
			}

			// Include the actual item being picked up
			if (isCompressible(pickupClone)) {
				int result = AranarthUtils.isBlacklistingItem(aranarthPlayer, pickupClone);
				if (result == 0) {
					e.getItem().setItemStack(null);
					e.getItem().remove();
				} else if (result == 1) {
					// Do not pick up whatsoever
					e.setCancelled(true);
				} else {
					e.getItem().setItemStack(null);
					Material pickedUpType = pickupClone.getType();
					if (compressibleItems.containsKey(pickedUpType)) {
						List<ItemStack> stacksOfItems = compressibleItems.get(pickedUpType);
						stacksOfItems.add(pickupClone);
						compressibleItems.put(pickedUpType, stacksOfItems);
					} else {
						List<ItemStack> stacksOfItems = new ArrayList<>();
						stacksOfItems.add(pickupClone);
						compressibleItems.put(pickedUpType, stacksOfItems);
					}
				}
			}
			// Fallback method to pick up normally if it's a non-compressible item that's picked up
			else {
				player.getInventory().addItem(pickupClone);
				return;
			}

			// Compresses each item that is in the player's inventory that is compressible
			for (Material type : compressibleItems.keySet()) {
				switch (type) {
					case Material.COAL -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.RAW_COPPER -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.COPPER_INGOT -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.RAW_IRON -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.IRON_NUGGET -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.IRON_INGOT -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.RAW_GOLD -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.GOLD_NUGGET -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.GOLD_INGOT -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.REDSTONE -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.LAPIS_LAZULI -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.DIAMOND -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.EMERALD -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.NETHERITE_INGOT -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.AMETHYST_SHARD -> calculateCompressedAmounts(player, compressibleItems.get(type), 4);
					case Material.RESIN_CLUMP -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.GLOWSTONE_DUST -> calculateCompressedAmounts(player, compressibleItems.get(type), 4);
					case Material.WHEAT -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.MELON_SLICE -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.DRIED_KELP -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.SUGAR_CANE -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.HONEYCOMB -> calculateCompressedAmounts(player, compressibleItems.get(type), 4);
					case Material.SLIME_BALL -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.BONE_MEAL -> calculateCompressedAmounts(player, compressibleItems.get(type), 9);
					case Material.SNOWBALL -> calculateCompressedAmounts(player, compressibleItems.get(type), 4);
					case Material.CLAY_BALL -> calculateCompressedAmounts(player, compressibleItems.get(type), 4);
				}
			}
		}
	}

	/**
	 * Handles the full compressing logic of going through a player's inventory and compressing if the picked up item can be.
	 * @param player The player whose inventory is being compressed.
	 * @param allStacksOfUncompressedItem The List of ItemStack of the Item being compressed.
	 * @param amountRequiredToCompress The quantity of the item that must be in the inventory in order to compress.
	 */
	private void calculateCompressedAmounts(Player player, List<ItemStack> allStacksOfUncompressedItem, int amountRequiredToCompress) {
		int totalAmountOfItem = 0;
		for (ItemStack stack : allStacksOfUncompressedItem) {
			totalAmountOfItem += stack.getAmount();
		}

		int totalAmountOfCompressedItem = totalAmountOfItem / amountRequiredToCompress;
		int remainder = totalAmountOfItem % amountRequiredToCompress;

		addToInventory(player, allStacksOfUncompressedItem.getFirst().getType(), totalAmountOfCompressedItem, remainder);
	}

	/**
	 * Adds the compressed items and the remaining non-compressed items to the player's inventory and shulkers.
	 * @param player The player.
	 * @param type The non-compressed Material.
	 * @param totalAmountOfCompressedItem The total amount of the compressed item.
	 * @param remainder The remaining amount of the compressed item.
	 */
	private void addToInventory(Player player, Material type, int totalAmountOfCompressedItem, int remainder) {
		Material compressedType = getCompressedType(type);

		if (totalAmountOfCompressedItem > 0) {
			ItemStack compressedItemToAdd = null;
			if (compressedType == Material.BAMBOO_BLOCK) {
				compressedItemToAdd = new SugarcaneBlock().getItem();
				compressedItemToAdd.setAmount(totalAmountOfCompressedItem);
			} else {
				compressedItemToAdd = new ItemStack(compressedType, totalAmountOfCompressedItem);
			}

			if (player.hasPermission("aranarth.shulker")) {
				for (ItemStack inventoryItem : player.getInventory().getContents()) {
					// Ignore if it is empty
					if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
						continue;
					}

					if (inventoryItem.hasItemMeta()) {
						if (inventoryItem.getItemMeta() instanceof BlockStateMeta im) {
							if (im.getBlockState() instanceof ShulkerBox shulker) {
								for (ItemStack shulkerItem : shulker.getInventory().getContents()) {
									// Ignore if it is empty
									if (shulkerItem == null || shulkerItem.getType() == Material.AIR) {
										continue;
									}

									if (!isCompressible(shulkerItem)) {
										continue;
									}

									// Increases non-full stacks within the shulker first
									if (shulkerItem.getType() == compressedType) {
										while (shulkerItem.getAmount() < shulkerItem.getMaxStackSize()) {
											if (totalAmountOfCompressedItem > 0) {
												shulkerItem.setAmount(shulkerItem.getAmount() + 1);
												totalAmountOfCompressedItem--;
											} else {
												break;
											}
										}
									}
								}
								im.setBlockState(shulker);
								inventoryItem.setItemMeta(im);
							}
						}
					}
				}
			}

			if (totalAmountOfCompressedItem > 0) {
				addResultsToInventory(player, compressedItemToAdd);
			}
		}

		if (remainder > 0) {
			ItemStack remainderItemToAdd = new ItemStack(type, remainder);
			addResultsToInventory(player, remainderItemToAdd);
		}
	}

	/**
	 * Adds the results of the compressed and remaining items to the inventory, prioritizing the inventory before the hotbar.
	 * @param player The player that picked up the item.
	 * @param item The compressed or remaining item being added to the inventory.
	 */
	private void addResultsToInventory(Player player, ItemStack item) {
		int remainingToAdd = item.getAmount();

		// Fills shulker boxes with the compressed blocks
		if (player.hasPermission("aranarth.shulker")) {
			for (int i = 0; i < player.getInventory().getStorageContents().length; i++) {
				ItemStack inventoryItem = player.getInventory().getStorageContents()[i];
				if (inventoryItem != null && inventoryItem.getType() != Material.AIR) {
					if (inventoryItem.getType().name().contains("SHULKER_BOX")) {
						ItemMeta meta = inventoryItem.getItemMeta();
						if (meta instanceof BlockStateMeta im) {
							if (im.getBlockState() instanceof ShulkerBox shulker) {
								for (ItemStack shulkerItem : shulker.getInventory().getContents()) {
									if (shulkerItem != null && shulkerItem.getType() != Material.AIR) {
										if (shulkerItem.isSimilar(item)) {
											while (remainingToAdd > 0) {
												// Do not exceed stack size
												if (shulkerItem.getAmount() == shulkerItem.getMaxStackSize()) {
													break;
												}

												// Add to the amount and end if it is the amount needed to be added
												shulkerItem.setAmount(shulkerItem.getAmount() + 1);
												remainingToAdd--;
												if (remainingToAdd == 0) {
													break;
												}
											}
											if (remainingToAdd == 0) {
												break;
											}
										}
									}
								}
								im.setBlockState(shulker);
								inventoryItem.setItemMeta(im);
								break;
							}
						}
					} else {
						break;
					}
				}
			}
		}

		// Stacks the added items together
		for (int i = 0; i < player.getInventory().getStorageContents().length; i++) {
			ItemStack inventoryItem = player.getInventory().getStorageContents()[i];
			if (inventoryItem != null && inventoryItem.getType() != Material.AIR) {
				while (remainingToAdd > 0) {
					if (inventoryItem.isSimilar(item)) {
						// Do not exceed stack size
						if (inventoryItem.getAmount() == inventoryItem.getMaxStackSize()) {
							break;
						}

						// Add to the amount and end if it is the amount needed to be added
						inventoryItem.setAmount(inventoryItem.getAmount() + 1);
						remainingToAdd--;
						if (remainingToAdd == 0) {
							break;
						}
					} else {
						break;
					}
				}

				if (remainingToAdd == 0) {
					player.getInventory().setItem(i, inventoryItem);
					break;
				}
			}
		}

		// Will then prioritize empty slots in inventory but not hotbar
		for (int i = 9; i < player.getInventory().getStorageContents().length; i++) {
			ItemStack inventoryItem = player.getInventory().getStorageContents()[i];
			if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
				while (remainingToAdd > 0) {
					if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
						// For sugarcane blocks
						if (item.hasItemMeta()) {
							inventoryItem = item.clone();
							inventoryItem.setAmount(1);
						} else {
							inventoryItem = new ItemStack(item.getType(), 1);
						}

						remainingToAdd--;
					} else {
						inventoryItem.setAmount(inventoryItem.getAmount() + 1);
						remainingToAdd--;
					}

					if (inventoryItem.getAmount() == inventoryItem.getMaxStackSize()) {
						player.getInventory().setItem(i, inventoryItem);
						break;
					}
				}

				if (remainingToAdd == 0) {
					player.getInventory().setItem(i, inventoryItem);
					break;
				}
			} else {
				continue;
			}
		}

		item.setAmount(remainingToAdd);

		// Will finally prioritize empty slots in hotbar (remaining slots) and will drop leftovers to the ground
		HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
		// If the player's inventory was full, drop it to the ground
		if (!leftover.isEmpty()) {
			player.getLocation().getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
		}
	}

	/**
	 * Confirms if the input ItemStack is a compressible item.
	 * @param item The item that is being verified.
	 * @return Confirmation if the input item is compressible.
	 */
	private boolean isCompressible(ItemStack item) {
		if (item.hasItemMeta()) {
			return false;
		}

		Material type = item.getType();
		return type == Material.COAL || type ==  Material.RAW_COPPER || type ==  Material.COPPER_INGOT
				|| type == Material.RAW_IRON || type == Material.IRON_NUGGET || type == Material.IRON_INGOT
				|| type == Material.RAW_GOLD || type == Material.GOLD_NUGGET || type == Material.GOLD_INGOT
				|| type == Material.REDSTONE || type == Material.LAPIS_LAZULI || type == Material.DIAMOND
				|| type == Material.EMERALD || type == Material.NETHERITE_INGOT || type == Material.AMETHYST_SHARD
				|| type == Material.RESIN_CLUMP || type == Material.GLOWSTONE_DUST || type == Material.WHEAT
				|| type == Material.MELON_SLICE || type == Material.DRIED_KELP || type == Material.SUGAR_CANE
				|| type == Material.HONEYCOMB || type == Material.SLIME_BALL || type == Material.BONE_MEAL
				|| type == Material.SNOWBALL || type == Material.CLAY_BALL;
	}

	/**
	 * Provides the compressed equivalent of the input type.
	 * @param type The input type.
	 * @return The compressed equivalent of the input type.
	 */
	private Material getCompressedType(Material type) {
        return switch (type) {
            case Material.COAL -> Material.COAL_BLOCK;
            case Material.RAW_COPPER -> Material.RAW_COPPER_BLOCK;
            case Material.COPPER_INGOT -> Material.COPPER_BLOCK;
            case Material.RAW_IRON -> Material.RAW_IRON_BLOCK;
            case Material.IRON_NUGGET -> Material.IRON_INGOT;
            case Material.IRON_INGOT -> Material.IRON_BLOCK;
            case Material.RAW_GOLD -> Material.RAW_GOLD_BLOCK;
            case Material.GOLD_NUGGET -> Material.GOLD_INGOT;
            case Material.GOLD_INGOT -> Material.GOLD_BLOCK;
            case Material.REDSTONE -> Material.REDSTONE_BLOCK;
            case Material.LAPIS_LAZULI -> Material.LAPIS_BLOCK;
            case Material.DIAMOND -> Material.DIAMOND_BLOCK;
            case Material.EMERALD -> Material.EMERALD_BLOCK;
            case Material.NETHERITE_INGOT -> Material.NETHERITE_BLOCK;
            case Material.AMETHYST_SHARD -> Material.AMETHYST_BLOCK;
            case Material.RESIN_CLUMP -> Material.RESIN_BLOCK;
            case Material.GLOWSTONE_DUST -> Material.GLOWSTONE;
            case Material.WHEAT -> Material.HAY_BLOCK;
            case Material.MELON_SLICE -> Material.MELON;
            case Material.DRIED_KELP -> Material.DRIED_KELP_BLOCK;
            case Material.SUGAR_CANE -> Material.BAMBOO_BLOCK;
            case Material.HONEYCOMB -> Material.HONEYCOMB_BLOCK;
            case Material.SLIME_BALL -> Material.SLIME_BLOCK;
            case Material.BONE_MEAL -> Material.BONE_BLOCK;
            case Material.SNOWBALL -> Material.SNOW_BLOCK;
            case Material.CLAY_BALL -> Material.CLAY;
            default -> null;
        };
	}
}
