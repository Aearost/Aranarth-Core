package com.aearost.aranarthcore.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
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
		if (ChatUtils.stripColor(e.getView().getTitle()).equals("Potions") && e.getView().getType() == InventoryType.CHEST) {
			Inventory inventory = e.getInventory();
			if (inventory.getContents().length > 0) {
				Player player = (Player) e.getPlayer();
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				
				List<ItemStack> potions = aranarthPlayer.getPotions();
				List<ItemStack> inventoryPotions = new LinkedList<ItemStack>(Arrays.asList(inventory.getContents()));
				
				if (Objects.nonNull(inventoryPotions)) {
					if (Objects.isNull(potions)) {
						potions = new ArrayList<ItemStack>();
					}
					
					int potionAmountAdded = 0;
					for (ItemStack inventoryPotion : inventoryPotions) {
						if (Objects.nonNull(inventoryPotion)) {
							// Rare chance that inventory glitches and stores air
							if (inventoryPotion.getType() == Material.AIR) {
								continue;
							}
							potions.add(inventoryPotion);
							potionAmountAdded++;
						}
					}
					
					aranarthPlayer.setPotions(potions);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					
					if (potionAmountAdded > 0) {
						player.sendMessage(ChatUtils.chatMessage("&7You have added &e" + potionAmountAdded + " &7potions!"));
					}
				}
			}
		}
		
	}
}
