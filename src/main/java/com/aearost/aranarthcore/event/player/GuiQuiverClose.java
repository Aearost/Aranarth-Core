package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Adds the input arrows to the player's quiver inventory.
 */
public class GuiQuiverClose {
	public void execute(InventoryCloseEvent e) {
		Inventory inventory = e.getInventory();
		if (inventory.getContents().length > 0) {
			Player player = (Player) e.getPlayer();
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			List<ItemStack> arrows = aranarthPlayer.getArrows();
			List<ItemStack> inventoryArrows = new LinkedList<>(Arrays.asList(inventory.getContents()));

			if (Objects.isNull(arrows)) {
				arrows = new ArrayList<>();
			}

			for (ItemStack inventoryArrow : inventoryArrows) {
				if (Objects.nonNull(inventoryArrow)) {
					arrows.add(inventoryArrow);
				}
			}
			aranarthPlayer.setArrows(inventoryArrows);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		}
	}
}
