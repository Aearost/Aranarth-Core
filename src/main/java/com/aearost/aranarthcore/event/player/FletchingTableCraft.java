package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.arrowhead.*;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARROW_HEAD;

/**
 * Handles all crafting of items within a Fletching Table.
 */
public class FletchingTableCraft {
	public void execute(InventoryClickEvent e) {
		if (!ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Fletching Table")) {
			return;
		}

		final int slot = e.getSlot();

		// Prevent shift clicking all of the player's inventory
		if (e.isShiftClick() && e.getClickedInventory().getType() != InventoryType.WORKBENCH) {
			e.setCancelled(true);
			return;
		}

		if (e.getClickedInventory().getType() == InventoryType.WORKBENCH) {
			// Block invalid UI slots
			if (slot == 1 || slot == 3 || slot == 4 || slot == 6 || slot == 7 || slot == 9) {
				e.setCancelled(true);
				return;
			}

			if (slot == 0 && e.isShiftClick() && e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {
				e.setCancelled(true);

				ItemStack[] contents = e.getInventory().getContents();
				ItemStack result = e.getCurrentItem().clone();
				Player player = (Player) e.getWhoClicked();

				int craftsPossible = 0;

				// Arrow recipe
				if (isCraftingArrow(contents)) {
					int headCount = contents[1].getAmount();
					int stickCount = contents[4].getAmount();
					int featherCount = contents[7].getAmount();
					craftsPossible = Math.min(headCount, Math.min(stickCount, featherCount));
				}
				// Arrowhead recipe
				else {
					ItemStack a = safe(contents, 2);
					ItemStack b = safe(contents, 5);
					ItemStack c = safe(contents, 8);
					ItemStack ingredient = firstNonEmpty(a, b, c);
					if (ingredient != null) {
						craftsPossible = ingredient.getAmount();
					}
				}

				if (craftsPossible > 0) {
					// Multiply result amount by craftsPossible
					result.setAmount(result.getAmount() * craftsPossible);

					// Add to player inventory (handles overflow automatically)
					player.getInventory().addItem(result);

					// Consume ingredients
					for (int i = 0; i < craftsPossible; i++) {
						consumeIngredients(e.getInventory());
					}

					// Refresh result next tick
					Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> updateResult(e.getInventory()));
				}
				return;
			}

			// Special handling for result slot (slot 0)
			if (slot == 0 && e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {
				final ItemStack result = e.getCurrentItem();
				final ItemStack cursor = e.getCursor();

				// Always cancel vanilla so it doesn't try to place the cursor into slot 0
				e.setCancelled(true);

				if (cursor == null || cursor.getType() == Material.AIR) {
					// Empty cursor → pick up full result stack (or you can limit to 1 if desired)
					e.getWhoClicked().setItemOnCursor(result.clone());
					consumeIngredients(e.getInventory()); // consume for one craft
				} else if (cursor.isSimilar(result) && cursor.getAmount() < cursor.getMaxStackSize()) {
					// Stack onto cursor
					int spaceLeft = cursor.getMaxStackSize() - cursor.getAmount();
					int toAdd = Math.min(spaceLeft, result.getAmount());
					if (toAdd > 0) {
						cursor.setAmount(cursor.getAmount() + toAdd);
						e.getWhoClicked().setItemOnCursor(cursor);
						consumeIngredients(e.getInventory()); // consume for one craft
					}
				}
				// Next tick: recompute result based on new ingredients
				Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> updateResult(e.getInventory()));
				return;
			}

			// All other clicks: let vanilla finish, then recompute next tick
			if (e.getAction().name().startsWith("PICKUP")
					|| e.getAction().name().startsWith("PLACE")
					|| e.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
				Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> updateResult(e.getInventory()));
			} else {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Decreases the ingredients once crafting in the Fletching Table.
	 * @param inventory The inventory of the Fletching Table
	 */
	private void consumeIngredients(Inventory inventory) {
		decrement(inventory, 2, 1);
		decrement(inventory, 5, 1);
		decrement(inventory, 8, 1);
	}

	/**
	 * Decrements the ingredient and updates the ingredient based on what remains.
	 * @param inventory The inventory of the Fletching Table.
	 * @param slot The slot of the ingredient.
	 * @param amount The amount being decremented.
	 */
	private void decrement(Inventory inventory, int slot, int amount) {
		ItemStack item = inventory.getItem(slot);
		if (item == null || item.getType() == Material.AIR) return;

		int newAmount = item.getAmount() - amount;
		if (newAmount > 0) {
			item.setAmount(newAmount);
			inventory.setItem(slot, item);
		} else {
			inventory.setItem(slot, null);
		}
	}

	/**
	 * Updates the result of the recipe in the Fletching Table.
	 * @param inventory The inventory of the Fletching Table.
	 */
	private void updateResult(Inventory inventory) {
		ItemStack[] contents = inventory.getContents().clone();

		ItemStack result = computeArrowResult(contents);
		if (result == null) {
			result = computeArrowheadResult(contents);
		}

		inventory.setItem(0, result); // Can be null, emptying the slot.
	}

	/**
	 * Determines the result of the arrow based on the inventory.
	 * @param inventory The inventory of the Fletching Table.
	 * @return The result of the recipe if the input recipe was valid.
	 */
	private ItemStack computeArrowResult(ItemStack[] inventory) {
		ItemStack arrowhead = safe(inventory, 2);
		ItemStack stick = safe(inventory, 5);
		ItemStack feather = safe(inventory, 8);

		if (arrowhead == null || stick == null || feather == null) return null;
		if (stick.getType() != Material.STICK || feather.getType() != Material.FEATHER) return null;
		if (!arrowhead.hasItemMeta()) return null;
		if (!arrowhead.getItemMeta().getPersistentDataContainer().has(ARROW_HEAD, PersistentDataType.STRING)) return null;

		String special = arrowhead.getItemMeta().getPersistentDataContainer().get(ARROW_HEAD, PersistentDataType.STRING);
		if (special == null) return null;

		return AranarthUtils.getArrowFromType(special);
	}

	/**
	 * Determines the result of the arrowhead based on the inventory.
	 * @param inventory The inventory of the Fletching Table.
	 * @return The result of the recipe if the input recipe was valid.
	 */
	private ItemStack computeArrowheadResult(ItemStack[] inventory) {
		// Only 2,5,8 are valid ingredient slots and exactly one of them must be filled
		ItemStack a = safe(inventory, 2);
		ItemStack b = safe(inventory, 5);
		ItemStack c = safe(inventory, 8);

		int filled = countNonEmpty(a, b, c);
		if (filled != 1) return null;

		ItemStack single = firstNonEmpty(a, b, c);
		if (single == null) return null;

		Material type = single.getType();
		if (type == Material.IRON_INGOT) {
			return new ArrowheadIron().getItem();
		}
		if (type == Material.GOLD_INGOT) {
			return new ArrowheadGold().getItem();
		}
		if (type == Material.AMETHYST_SHARD) {
			return new ArrowheadAmethyst().getItem();
		}
		if (type == Material.OBSIDIAN) {
			ItemStack is = new ArrowheadObsidian().getItem();
			is.setAmount(2);
			return is;
		}
		if (type == Material.DIAMOND) {
			return new ArrowheadDiamond().getItem();
		}

		return null;
	}

	private ItemStack safe(ItemStack[] inv, int i) {
		if (i < 0 || i >= inv.length) return null;
		ItemStack it = inv[i];
		return (it == null || it.getType() == Material.AIR) ? null : it;
	}

	private int countNonEmpty(ItemStack... arr) {
		int n = 0; for (ItemStack s : arr) if (s != null) n++; return n;
	}
	private ItemStack firstNonEmpty(ItemStack... arr) {
		for (ItemStack s : arr) if (s != null) return s; return null;
	}

	private void updateInventoryResult(InventoryClickEvent e) {
		ItemStack[] craftingInventory = e.getInventory().getContents().clone();

		if (isCraftingArrow(craftingInventory)) {
			String specialArrowType = craftingInventory[2].getItemMeta().getPersistentDataContainer().get(ARROW_HEAD, PersistentDataType.STRING);
			ItemStack arrow = AranarthUtils.getArrowFromType(specialArrowType);

			craftingInventory[0] = arrow;
			e.getInventory().setContents(craftingInventory);
		} else {
			ItemStack arrowhead = craftingArrowhead(e.getInventory().getContents());
			if (arrowhead != null) {
				craftingInventory[0] = arrowhead;
				e.getInventory().setContents(craftingInventory);
			}
		}
	}

	/**
	 * Confirms whether the player is crafting an arrow.
	 * @param inventory The inventory of the Fletching Table.
	 * @return Confirmation whether the player is crafting an arrow.
	 */
	private boolean isCraftingArrow(ItemStack[] inventory) {
		if (inventory[4].getType() == Material.STICK && inventory[7].getType() == Material.FEATHER) {
			if (inventory[1].hasItemMeta()) {
				if (inventory[1].getItemMeta().getPersistentDataContainer().has(ARROW_HEAD)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Confirms and provides the arrowhead the player is attempting to craft
	 * @param inventory The inventory of the Fletching Table.
	 * @return The arrowhead if it was crafted successfully, or null if the recipe is invalid.
	 */
	private ItemStack craftingArrowhead(ItemStack[] inventory) {
		// Only track the 3 relevant ingredients
		ItemStack[] ingredients = new ItemStack[3];
		ingredients[0] = inventory[2];
		ingredients[1] = inventory[5];
		ingredients[2] = inventory[8];

		ItemStack arrowhead = null;
		arrowhead = basicArrowHeadCraft(ingredients);

		return arrowhead;
	}

	private ItemStack basicArrowHeadCraft(ItemStack[] ingredients) {
		int emptyCount = 0;
		ItemStack arrowheadToReturn = null;
		for (ItemStack ingredient : ingredients) {
			if (ingredient == null) {
				emptyCount++;
				continue;
			}

			if (ingredient.getType() == Material.IRON_INGOT) {
				arrowheadToReturn = new ArrowheadIron().getItem();
			} else if (ingredient.getType() == Material.GOLD_INGOT) {
				arrowheadToReturn = new ArrowheadGold().getItem();
			} else if (ingredient.getType() == Material.AMETHYST_SHARD) {
				arrowheadToReturn = new ArrowheadAmethyst().getItem();
			} else if (ingredient.getType() == Material.OBSIDIAN) {
				arrowheadToReturn = new ArrowheadObsidian().getItem();
				arrowheadToReturn.setAmount(2);
			} else if (ingredient.getType() == Material.DIAMOND) {
				arrowheadToReturn = new ArrowheadDiamond().getItem();
			}
		}

		// Ensures that only 1 slot was filled
		if (emptyCount != 2) {
			return null;
		} else {
			return arrowheadToReturn;
		}
	}
}
