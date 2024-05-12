package com.aearost.aranarthcore.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class GuiQuiverClose implements Listener {

	public GuiQuiverClose(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds the input arrows to the player's quiver inventory.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onQuiverInventoryClose(final InventoryCloseEvent e) {
		if (ChatUtils.stripColor(e.getView().getTitle()).equals("Quiver")) {
			Inventory inventory = e.getInventory();
			if (inventory.getContents().length > 0) {
				Player player = (Player) e.getPlayer();
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				
				List<ItemStack> arrows = aranarthPlayer.getArrows();
				List<ItemStack> inventoryArrows = new LinkedList<ItemStack>(Arrays.asList(inventory.getContents()));
				
				if (Objects.nonNull(inventoryArrows)) {
					if (Objects.isNull(arrows)) {
						arrows = new ArrayList<ItemStack>();
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
