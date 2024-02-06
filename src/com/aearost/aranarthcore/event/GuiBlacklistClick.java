package com.aearost.aranarthcore.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class GuiBlacklistClick implements Listener {

	public GuiBlacklistClick(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with all clicks of the GUI elements.
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onGuiClick(final InventoryClickEvent e) {
		if (ChatUtils.stripColor(e.getView().getTitle()).equals("Blacklist")) {
			e.setCancelled(true);
			Player player = (Player) e.getWhoClicked();
			List<ItemStack> blacklistedItems = AranarthUtils.getBlacklistedItems(player.getUniqueId());
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}
			if (Objects.isNull(blacklistedItems)) {
				blacklistedItems = new ArrayList<ItemStack>();
			}
			
			// If adding a new item to the blacklist
			if (e.getClickedInventory().getSize() == 41) {
				ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
				if (blacklistedItems.size() == 9 && clickedItem != null) {
					player.sendMessage(ChatUtils.chatMessageError("You have already blacklisted 9 items!"));
				} else {
					for (ItemStack itemStack : blacklistedItems) {
						if (clickedItem.getType() == itemStack.getType()) {
							player.sendMessage(ChatUtils.chatMessageError("This item is already blacklisted!"));
							e.getWhoClicked().closeInventory();
							return;
						}
					}
					blacklistedItems.add(new ItemStack(clickedItem.getType(), 1));
					player.getInventory().setItem(e.getSlot(), clickedItem);
					AranarthUtils.updateBlacklistedItems(player.getUniqueId(), blacklistedItems);
					player.sendMessage(ChatUtils.chatMessage("&7You have added &e" + ChatUtils.getFormattedItemName(clickedItem.getType().name() + " &7to the blacklisted items")));
				}
			}
			// If removing a blacklisted item
			else {
				ItemStack blacklistedItem = blacklistedItems.get(e.getSlot());
				if (Objects.nonNull(blacklistedItem)) {
					player.sendMessage(ChatUtils.chatMessage("&e" + ChatUtils.getFormattedItemName(blacklistedItem.getType().name()) + " &7is no longer blacklisted"));
					blacklistedItems.remove(e.getSlot());
					AranarthUtils.updateBlacklistedItems(player.getUniqueId(), blacklistedItems);
				}
			}
			e.getWhoClicked().closeInventory();
		}
	}
	
}
