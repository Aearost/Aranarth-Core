package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.List;
import java.util.Objects;

public class GuiQuiverClick implements Listener {

	public GuiQuiverClick(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from adding non-arrow items to the arrows inventory.
	 * @param e The event.
	 */
	@EventHandler
	public void onGuiClick(final InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Quiver") && e.getView().getType() == InventoryType.CHEST) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}
			
			// If adding a new item to the arrows inventory
			if (e.getClickedInventory().getType() == InventoryType.CHEST) {
				ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
				// Ensures a non-empty slot is clicked
				if (Objects.isNull(clickedItem)) {
					// If placing potion back into player slots
					if (Objects.nonNull(e.getCursor())) {
						return;
					}
					e.setCancelled(true);
				}
				
				if (!isItemArrow(clickedItem)) {
					e.setCancelled(true);
				}
			}
		} else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Arrow Selection") && e.getView().getType() == InventoryType.CHEST) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}

			// Prevent inventory updates
			e.setCancelled(true);

			// Handles logic to switch slots if inventory contains that kind of arrow
			if (e.getClickedInventory().getType() == InventoryType.CHEST) {
				if (e.getWhoClicked() instanceof Player player) {
					ItemStack[] playerInventory = player.getInventory().getContents();
					for (int i = 0; i < playerInventory.length; i++) {
						if (playerInventory[i] != null) {
							// Gets the first arrow in the player's inventory
							if (isItemArrow(playerInventory[i])) {
								ItemStack selectedArrow = e.getCurrentItem();
								AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
								List<ItemStack> playerArrows = aranarthPlayer.getArrows();

								for (int j = 0; j < playerArrows.size(); j++) {
									if (selectedArrow != null && playerArrows.get(j) != null) {

										// Finds first stack in quiver matching what was clicked
										if (playerArrows.get(j).isSimilar(selectedArrow)) {
											ItemStack stackFromQuiver = playerArrows.get(j).clone();
											// Updates quiver stack with what's in inventory
											playerArrows.set(j, playerInventory[i]);
											// Updates inventory stack with what's in the quiver
											player.getInventory().setItem(i, stackFromQuiver);

											if (stackFromQuiver.hasItemMeta()) {
												if (stackFromQuiver.getItemMeta() instanceof PotionMeta meta) {
													// If mcMMO arrow
													if (meta.hasCustomEffects()) {
														String arrowName = meta.getCustomEffects().getFirst().getType().getKey().getKey();
														String newName = arrowName.substring(0, 1).toUpperCase();
														newName = newName + arrowName.substring(1);
														player.sendMessage(ChatUtils.chatMessage("&7You will now use &e" + newName + " &7arrows"));
													} else {
														StringBuilder newNameSB = new StringBuilder();
														String[] splitArrowName = ChatUtils.getFormattedItemName(meta.getBasePotionType().name()).split(" ");
														for (int k = 0; k < splitArrowName.length; k++) {
															if (splitArrowName[k].equals("Long") || splitArrowName[k].equals("Strong")) {
																continue;
															}
															newNameSB.append(splitArrowName[k]);
															if (k == splitArrowName.length - 1) {
																break;
															} else {
																newNameSB.append(" ");
															}
														}
														player.sendMessage(ChatUtils.chatMessage("&7You will now use &e" + newNameSB + " &7arrows"));
													}
												}
											} else {
												if (stackFromQuiver.getType() == Material.ARROW) {
													player.sendMessage(ChatUtils.chatMessage("&7You will now use regular &eArrows"));
												} else if (stackFromQuiver.getType() == Material.SPECTRAL_ARROW) {
													player.sendMessage(ChatUtils.chatMessage("&7You will now use &eSpectral Arrows"));
												} else {
													player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong!"));
												}
											}
											break;
										}
									}
								}
								player.closeInventory();
								return;
							}
						}
					}
					player.sendMessage(ChatUtils.chatMessage("&cYou must have an arrow in your inventory to do this!"));
					player.closeInventory();
				}

			}
		}
	}
	
	private boolean isItemArrow(ItemStack item) {
		return (item.getType() == Material.ARROW
				|| item.getType() == Material.TIPPED_ARROW
				|| item.getType() == Material.SPECTRAL_ARROW);
	}
}
