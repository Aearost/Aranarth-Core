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
import com.aearost.aranarthcore.gui.GuiTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class GuiTeleportClick implements Listener {

	public GuiTeleportClick(AranarthCore plugin) {
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
				}
				return;
			}
			// If they click Exit
			else if (isClickedHomepadGui && slot == 31 && heldItem == Material.AIR) {
				e.setCancelled(true);
				player.closeInventory();
				return;
			}
			// If they click Next
			else if (isClickedHomepadGui && slot == 35 && heldItem == Material.AIR) {
				e.setCancelled(true);
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
					GuiTeleport gui = new GuiTeleport(player, currentPage);
					gui.openGui();
				}
				return;
			} else {
					if (isClickedHomepadGui) {
						// If clicking a slot in the last row
						if (slot >= 27) {
							e.setCancelled(true);
							return;
						}
						
						List<Home> homes = AranarthUtils.getHomes();
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
									player.sendMessage(ChatUtils.chatMessageError("This homepad already uses that icon!"));
								} else {
									AranarthUtils.updateHome(home.getHomeName(), home.getLocation(), heldItem);
									player.sendMessage(ChatUtils.chatMessage("&7You have updated the icon of " + home.getHomeName() + "&7 to &e" + getFormattedItemName(heldItem.name())));
								}
							} else {
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
							}
							player.closeInventory();
						} else {
							e.setCancelled(true);
						}
					}
					
				}
			}
		}
	
	private String getFormattedItemName(String nameToFormat) {
		String[] words = nameToFormat.toLowerCase().split("_");
		String fullItemName = "";
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			String formattedWord = "";
			// If it shouldn't be capitalized
			if (word.equals("the") || word.equals("of") || word.equals("and") || word.equals("a") || word.equals("on")) {
				formattedWord = word;
			} else {
				formattedWord = Character.toUpperCase(word.charAt(0)) + word.substring(1);
			}
			
			if (i == words.length - 1) {
				fullItemName += formattedWord;
				break;
			} else {
				fullItemName += formattedWord + " ";
			}
		}
		return fullItemName;
	}

}
