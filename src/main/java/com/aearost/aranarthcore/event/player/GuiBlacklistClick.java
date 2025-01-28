package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GuiBlacklistClick implements Listener {

	public GuiBlacklistClick(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with all clicks of the blacklist GUI elements.
	 * @param e The event.
	 */
	@EventHandler
	public void onGuiClick(final InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Blacklist") && e.getView().getType() == InventoryType.CHEST) {
			e.setCancelled(true);
			Player player = (Player) e.getWhoClicked();
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			List<ItemStack> blacklistedItems = aranarthPlayer.getBlacklist();
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}
			if (Objects.isNull(blacklistedItems)) {
				blacklistedItems = new ArrayList<>();
			}
			
			// If adding a new item to the blacklist
			if (e.getClickedInventory().getType() == InventoryType.CHEST) {
				ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
				if (Objects.isNull(clickedItem)) {
					return;
				}
				if (blacklistedItems.size() == 27) {
					player.sendMessage(ChatUtils.chatMessage("&cYou have already blacklisted 27 items!"));
				} else {
					for (ItemStack itemStack : blacklistedItems) {
						if (clickedItem.getType() == itemStack.getType()) {
							player.sendMessage(ChatUtils.chatMessage("&cThis item is already blacklisted!"));
							e.getWhoClicked().closeInventory();
							return;
						}
					}
					blacklistedItems.add(new ItemStack(clickedItem.getType(), 1));
					player.getInventory().setItem(e.getSlot(), clickedItem);
					aranarthPlayer.setBlacklist(blacklistedItems);
					player.sendMessage(ChatUtils.chatMessage("&7You have added &e" + ChatUtils.getFormattedItemName(clickedItem.getType().name() + " &7to the blacklisted items")));
					player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.5F, 1.75F);
				}
			}
			// If removing a blacklisted item
			else {
				// Clicking on a null slot
				if (e.getSlot() >= blacklistedItems.size()) {
					return;
				}
				ItemStack blacklistedItem = blacklistedItems.get(e.getSlot());
				if (Objects.nonNull(blacklistedItem)) {
					player.sendMessage(ChatUtils.chatMessage("&e" + ChatUtils.getFormattedItemName(blacklistedItem.getType().name()) + " &7is no longer blacklisted"));
					player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.5F, 0.8F);
					blacklistedItems.remove(e.getSlot());
					aranarthPlayer.setBlacklist(blacklistedItems);
				}
			}
			e.getWhoClicked().closeInventory();
		}
	}
	
}
