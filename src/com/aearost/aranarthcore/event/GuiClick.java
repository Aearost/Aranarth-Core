package com.aearost.aranarthcore.event;

import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Entity;
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
				if (currentPage > 0) {
					currentPage--;
					aranarthPlayer.setCurrentGuiPageNum(currentPage);
					TeleportGui gui = new TeleportGui(player, currentPage);
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
				int numOfHomes = AranarthUtils.getHomes().size();
				int currentPage = aranarthPlayer.getCurrentGuiPageNum();
				int maxPages;
				
				// If the amount is a multiple of 27
				if (numOfHomes % 27 == 0) {
					maxPages = numOfHomes / 27;
				} else {
					maxPages = (int) Math.floor(numOfHomes / 27) + 1;
				}
				if (currentPage + 1 < maxPages) {
					currentPage++;
					aranarthPlayer.setCurrentGuiPageNum(currentPage);
					TeleportGui gui = new TeleportGui(player, currentPage);
					gui.openGui();
				}
				return;
			}

			try {
				if (Objects.nonNull(e.getCurrentItem())
						&& e.getCurrentItem().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
					List<Home> homes = AranarthUtils.getHomes();
					Home home = homes.get((aranarthPlayer.getCurrentGuiPageNum() * 27) + slot);
					// Only proceed if the slot they click is actually a homepad
					if (!Objects.isNull(home)) {
						if (player.isInsideVehicle()) {
							Entity mount = player.getVehicle();
							if (player.getVehicle() instanceof Horse || player.getVehicle() instanceof Camel) {
								player.leaveVehicle();
								mount.teleport(home.getLocation());
								player.teleport(home.getLocation());
								player.sendMessage(ChatUtils
										.chatMessage("&5&oYou have been wooshed to &d" + home.getHomeName() + "&5!"));
								player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1.3F, 2.0F);
								mount.addPassenger(player);
							}
						} else {
							player.teleport(home.getLocation());
							player.sendMessage(ChatUtils
									.chatMessage("&5&oYou have been wooshed to &d" + home.getHomeName() + "&5!"));
							player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1.3F, 2.0F);
						}
						player.closeInventory();
					}
				}
			} catch (NullPointerException ex) {
				// Ignore if caught
				Bukkit.getLogger().severe("NullPointerException caught when teleporting!");
				ex.printStackTrace();
			}
		}
	}

}
