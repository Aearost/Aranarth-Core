package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Adds the input arrows and firework rockets to the player's quiver inventory.
 */
public class GuiQuiverClose {
	public void execute(InventoryCloseEvent e) {
		Inventory inventory = e.getInventory();
		if (inventory.getContents().length > 0) {
			Player player = (Player) e.getPlayer();
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			List<ItemStack> inventoryArrows = new LinkedList<>(Arrays.asList(inventory.getContents()));

			// Only arrows and firework rockets can be stored, anything else goes back to the player
			for (int i = 0; i < inventoryArrows.size(); i++) {
				ItemStack item = inventoryArrows.get(i);
				if (item == null || item.getType() == Material.BLACK_STAINED_GLASS_PANE || isArrow(item)) {
					continue;
				}
				inventoryArrows.set(i, null);
				if (item.getType() != Material.AIR) {
					HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
					for (ItemStack drop : leftover.values()) {
						player.getWorld().dropItemNaturally(player.getLocation(), drop);
					}
				}
			}
			aranarthPlayer.setArrows(inventoryArrows);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		}
	}

	private boolean isArrow(ItemStack item) {
		return item.getType() == Material.ARROW
				|| item.getType() == Material.TIPPED_ARROW
				|| item.getType() == Material.SPECTRAL_ARROW
				|| item.getType() == Material.FIREWORK_ROCKET;
	}
}
