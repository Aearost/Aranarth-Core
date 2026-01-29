package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Objects;

/**
 * Deals with all clicks of the homepad GUI elements.
 */
public class GuiHomepadClick {

	public void execute(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		int slot = e.getSlot();

		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		boolean isClickedHomepadGui = e.getClickedInventory().getSize() == 36;
		Material heldItem = e.getCursor().getType();

		// If they click Previous, bring them back to the previous page
		if (isClickedHomepadGui && slot == 27 && heldItem == Material.AIR) {
			e.setCancelled(true);
			int currentPage = aranarthPlayer.getCurrentGuiPageNum();
			if (currentPage > 0) {
				currentPage--;
				aranarthPlayer.setCurrentGuiPageNum(currentPage);
				GuiTeleport gui = new GuiTeleport(player, currentPage);
				gui.openGui();
				player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
			} else if (currentPage == 0) {
				int numOfHomes = AranarthUtils.getHomepads().size();
				int maxPages;
				// If the amount is a multiple of 27
				if (numOfHomes % 27 == 0) {
					maxPages = numOfHomes / 27;
				} else {
					maxPages = (int) (double) (numOfHomes / 27) + 1;
				}
				if (maxPages > 1) {
					aranarthPlayer.setCurrentGuiPageNum(maxPages - 1);
					GuiTeleport gui = new GuiTeleport(player, maxPages - 1);
					gui.openGui();
					player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
				}
			}
		}
		// If they click Exit
		else if (isClickedHomepadGui && slot == 31 && heldItem == Material.AIR) {
			e.setCancelled(true);
			player.closeInventory();
			player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
		}
		// If they click Next
		else if (isClickedHomepadGui && slot == 35 && heldItem == Material.AIR) {
			e.setCancelled(true);
			int numOfHomes = AranarthUtils.getHomepads().size();
			int currentPage = aranarthPlayer.getCurrentGuiPageNum();
			int maxPages;

			// If the amount is a multiple of 27
			if (numOfHomes % 27 == 0) {
				maxPages = numOfHomes / 27;
			} else {
				maxPages = (int) (double) (numOfHomes / 27) + 1;
			}
			if (currentPage + 1 < maxPages) {
				currentPage++;
				aranarthPlayer.setCurrentGuiPageNum(currentPage);
				GuiTeleport gui = new GuiTeleport(player, currentPage);
				gui.openGui();
				player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
			} else {
				aranarthPlayer.setCurrentGuiPageNum(0);
				GuiTeleport gui = new GuiTeleport(player, 0);
				gui.openGui();
				player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
			}
		} else {
			if (isClickedHomepadGui) {
				// If clicking a slot in the last row
				if (slot >= 27) {
					e.setCancelled(true);
					return;
				}

				List<Home> homes = AranarthUtils.getHomepads();
				Home home = null;
				try {
					home = homes.get((aranarthPlayer.getCurrentGuiPageNum() * 27) + slot);
				} catch (IndexOutOfBoundsException exception) {
					e.setCancelled(true);
				}

				// Only proceed if the slot they click is actually a homepad
				if (!Objects.isNull(home)) {

					// If the user is trying to update the icon of a home
					if (heldItem != Material.AIR) {
						e.setCancelled(true);
						if (heldItem == home.getIcon()) {
							player.sendMessage(ChatUtils.chatMessage("&cThis homepad already uses that icon!"));
						} else {
							AranarthUtils.updateHomepad(home.getName(), home.getLocation(), heldItem);
							player.sendMessage(ChatUtils.chatMessage(home.getName() + "&7's icon is now &e" + ChatUtils.getFormattedItemName(heldItem.name())));
						}
					} else {
						if (player.isInsideVehicle()) {
							Entity mount = player.getVehicle();
							if (player.getVehicle() instanceof Horse || player.getVehicle() instanceof Camel) {
								player.leaveVehicle();
								mount.teleport(home.getLocation());
								AranarthUtils.teleportPlayer(player, player.getLocation(), home.getLocation());
								Bukkit.getLogger().info(player.getName() + " has teleported to " + home.getName() + " via homepad");
								try {
									Thread.sleep(20);
								} catch (InterruptedException ex) {
									Bukkit.getLogger().info("Something went wrong with the teleportation...");
								}

								player.sendMessage(ChatUtils.chatMessage("&5&oYou have been wooshed to &d" + home.getName() + "&5!"));
								mount.addPassenger(player);
							}
						} else {
							Bukkit.getLogger().info(player.getName() + " has teleported to " + home.getName() + " via homepad");
							try {
								Thread.sleep(20);
							} catch (InterruptedException ex) {
								Bukkit.getLogger().info("Something went wrong with the teleportation...");
							}
							AranarthUtils.teleportPlayer(player, player.getLocation(), home.getLocation());
							player.sendMessage(ChatUtils
									.chatMessage("&5&oYou have been wooshed to &d" + home.getName() + "&5!"));
						}
					}
					player.closeInventory();
				} else {
					e.setCancelled(true);
				}
			}

		}
	}

}
