package com.aearost.aranarthcore.event;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.TeleportGui;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class GuiClick implements Listener {

	public GuiClick(AranarthCore plugin) {
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

		if (ChatUtils.stripColor(e.getView().getTitle()).equals("Teleport")) {
			e.setCancelled(true);

			Player player = (Player) e.getWhoClicked();
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			int slot = e.getSlot();

			// If they click Previous, bring them back to the previous page
			if (slot == 27) {
				int currentPage = aranarthPlayer.getCurrentGuiPageNum();
				if (currentPage > 1) {
					aranarthPlayer.setCurrentGuiPageNum(currentPage - 1);
					TeleportGui gui = new TeleportGui(player, currentPage - 1);
					gui.openGui();
				}
				return;
			}
			// If they click Exit
			else if (slot == 31) {
				player.closeInventory();
				return;
			}
			// If they click Next
			else if (slot == 35) {
				int homeNum = AranarthUtils.getHomes().size();
				int currentPage = aranarthPlayer.getCurrentGuiPageNum();
				int maxPages = 0;
				
				// If the amount is a multiple of 27
				if (homeNum % 27 == 0) {
					maxPages = homeNum / 27;
				} else {
					maxPages = (int) Math.floor(homeNum / 27) + 1;
				}
				
				if (currentPage < maxPages) {
					aranarthPlayer.setCurrentGuiPageNum(currentPage + 1);
					TeleportGui gui = new TeleportGui(player, currentPage + 1);
					gui.openGui();
				}
				return;
			}

			try {
				if (e.getCurrentItem().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
					player.closeInventory();
					
					List<Home> homes = AranarthUtils.getHomes();
					Home home = homes.get((aranarthPlayer.getCurrentGuiPageNum() - 1) * 27 + slot);
					if (!player.getLocation().equals(home.getLocation())) {
						Horse horse = null;
						if (player.isInsideVehicle()) {
							if (player.getVehicle() instanceof Horse) {
								horse = (Horse) player.getVehicle();
								player.leaveVehicle();
								horse.teleport(home.getLocation());
								player.teleport(home.getLocation());
								player.sendMessage(ChatUtils.chatMessage("&5&oYou have been wooshed to &d" + home.getHomeName() + "&5!"));
								player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1.3F, 2.0F);
								horse.addPassenger(player);
							}
						} else {
							player.teleport(home.getLocation());
							player.sendMessage(ChatUtils.chatMessage("&5&oYou have been wooshed to &d" + home.getHomeName() + "&5!"));
							player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1.3F, 2.0F);
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot teleport to where you are!"));
					}
					player.closeInventory();
				}
			} catch (NullPointerException ex) {
				// Ignore if caught
			}
			
		}
	}

}
