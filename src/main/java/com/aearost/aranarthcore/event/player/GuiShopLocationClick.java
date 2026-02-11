package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiShopLocation;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.*;

/**
 * Handles the teleport logic for shop locations.
 */
public class GuiShopLocationClick {
	public void execute(InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Player Shops")) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}

			if (e.getWhoClicked() instanceof Player player) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

				if (e.getClickedInventory().getType() == InventoryType.CHEST) {
					// If they click Previous, bring them back to the previous page
					if (e.getSlot() == 27) {
						e.setCancelled(true);
						int currentPage = aranarthPlayer.getCurrentGuiPageNum();
						if (currentPage > 0) {
							currentPage--;
							aranarthPlayer.setCurrentGuiPageNum(currentPage);
							GuiShopLocation gui = new GuiShopLocation(player, currentPage);
							gui.openGui();
							player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
						} else if (currentPage == 0) {
							int numOfShopLocations = AranarthUtils.getShopLocations().size();
							int maxPages;
							// If the amount is a multiple of 27
							if (numOfShopLocations % 27 == 0) {
								maxPages = numOfShopLocations / 27;
							} else {
								maxPages = (int) (double) (numOfShopLocations / 27) + 1;
							}
							if (maxPages > 1) {
								aranarthPlayer.setCurrentGuiPageNum(maxPages - 1);
								GuiShopLocation gui = new GuiShopLocation(player, maxPages - 1);
								gui.openGui();
								player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
							}
						}
					}
					// If they click Exit
					else if (e.getSlot() == 31) {
						e.setCancelled(true);
						player.closeInventory();
						player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
					}
					// If they click Next
					else if (e.getSlot() == 35) {
						e.setCancelled(true);
						int numOfShopLocations = AranarthUtils.getShopLocations().size();
						int currentPage = aranarthPlayer.getCurrentGuiPageNum();
						int maxPages;

						// If the amount is a multiple of 27
						if (numOfShopLocations % 27 == 0) {
							maxPages = numOfShopLocations / 27;
						} else {
							maxPages = (int) (double) (numOfShopLocations / 27) + 1;
						}
						if (currentPage + 1 < maxPages) {
							currentPage++;
							aranarthPlayer.setCurrentGuiPageNum(currentPage);
							GuiShopLocation gui = new GuiShopLocation(player, currentPage);
							gui.openGui();
							player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
						} else {
							aranarthPlayer.setCurrentGuiPageNum(0);
							GuiShopLocation gui = new GuiShopLocation(player, 0);
							gui.openGui();
							player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
						}
					} else {
						// If clicking a slot in the last row
						if (e.getSlot() >= 27) {
							e.setCancelled(true);
							return;
						}

						HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
						List<UUID> uuidList = new ArrayList<>();
						uuidList.addAll(shopLocations.keySet());
						UUID uuid = null;
						try {
							uuid = uuidList.get((aranarthPlayer.getCurrentGuiPageNum() * 27) + e.getSlot());
						} catch (IndexOutOfBoundsException exception) {
							e.setCancelled(true);
							return;
						}

						if (uuid == null) {
							e.setCancelled(true);
							return;
						}

						AranarthPlayer shopOwnerPlayer = AranarthUtils.getPlayer(uuid);
						AranarthUtils.teleportPlayer(player, player.getLocation(), shopLocations.get(uuid), aranarthPlayer.isInAdminMode(), success -> {
							if (success) {
								player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + shopOwnerPlayer.getNickname() + "'s &7shop!"));
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &e" + shopOwnerPlayer.getNickname() + "'s &cshop!"));
							}
						});
						player.closeInventory();
					}
				}
			}
		}
	}

}
