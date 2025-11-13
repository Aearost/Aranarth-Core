package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Removes an input quantity of a potion from the player's stored potions.
 */
public class GuiPotionRemove {
	public void execute(InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Remove Potions")) {
			if (e.getView().getType() == InventoryType.CHEST) {
				// If the user did not click a slot
				if (e.getClickedInventory() == null) {
					return;
				}

				// If clicking an item in the player's inventory
				if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
					e.setCancelled(true);
				}
				// Clicking in the potions inventory
				else {
					ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
					if (Objects.isNull(clickedItem)) {
						e.setCancelled(true);
						return;
					}

					Player player = (Player) e.getWhoClicked();
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					HashMap<ItemStack, Integer> potions = aranarthPlayer.getPotions();
					int toBeRemoved = aranarthPlayer.getPotionQuantityToRemove();

					List<ItemStack> potionsToRemove = new ArrayList<>();
					int amountRemoved = 0;
					// While there is still quantity in the pouch for that potion
					while (potions.get(clickedItem) > 0) {
						if (toBeRemoved == 0) {
							break;
						}
						potionsToRemove.add(clickedItem);
						amountRemoved++;
						toBeRemoved--;
						potions.put(clickedItem, potions.get(clickedItem) - 1);
					}

					if (potions.get(clickedItem) == 0) {
						potions.remove(clickedItem);
					}

					e.setCancelled(true);
					player.closeInventory();

					aranarthPlayer.setPotions(potions);
					aranarthPlayer.setPotionQuantityToRemove(0);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					player.sendMessage(ChatUtils.chatMessage("&7You have removed &e" + amountRemoved + " &7potions"));

					for (ItemStack potionBeingAdded : potionsToRemove) {
						HashMap<Integer, ItemStack> nonAdded = player.getInventory().addItem(potionBeingAdded);
						if (!nonAdded.isEmpty()) {
							// Will only ever be 1 since this adds 1 potion at a time
							player.getLocation().getWorld().dropItemNaturally(player.getLocation(), nonAdded.get(0));
						}
					}
				}
			}
		}
	}
	
}
