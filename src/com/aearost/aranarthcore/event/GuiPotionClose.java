package com.aearost.aranarthcore.event;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class GuiPotionClose implements Listener {

	public GuiPotionClose(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds the input potions to the player's potion inventory.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onPotionInventoryClose(final InventoryCloseEvent e) {
		if (ChatUtils.stripColor(e.getView().getTitle()).equals("Potions")) {
			Inventory inventory = e.getInventory();
			if (inventory.getContents().length > 0) {
				Player player = (Player) e.getPlayer();
				
				List<ItemStack> potions = AranarthUtils.getPotions(player.getUniqueId());
				List<ItemStack> inventoryPotions = Arrays.asList(inventory.getContents());
				
				if (Objects.nonNull(inventoryPotions)) {
					if (Objects.isNull(potions)) {
						potions = new ArrayList<ItemStack>();
					}
					
					int potionAmountAdded = 0;
					for (ItemStack inventoryPotion : inventoryPotions) {
						if (Objects.nonNull(inventoryPotion)) {
							potions.add(inventoryPotion);
							potionAmountAdded++;
						}
					}
					AranarthUtils.updatePotions(player.getUniqueId(), potions);
					if (potionAmountAdded > 0) {
						player.sendMessage(ChatUtils.chatMessage("&7You have added &e" + potionAmountAdded + " &7potions!"));
					}
				}
			}
		}
		
	}
}
