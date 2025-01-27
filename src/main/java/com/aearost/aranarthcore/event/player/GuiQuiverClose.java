package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GuiQuiverClose implements Listener {

	public GuiQuiverClose(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds the input arrows to the player's quiver inventory.
	 * @param e The event.
	 */
	@EventHandler
	public void onQuiverInventoryClose(final InventoryCloseEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Quiver") && e.getView().getType() == InventoryType.CHEST) {
			Inventory inventory = e.getInventory();
			if (inventory.getContents().length > 0) {
				Player player = (Player) e.getPlayer();
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				
				List<ItemStack> arrows = aranarthPlayer.getArrows();

				// Update inventory mode only
				if (aranarthPlayer.getIsAddingToQuiver()) {
					aranarthPlayer.setIsAddingToQuiver(false);
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
	}
}
