package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Objects;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARROW;

/**
 * Prevents players from adding non-arrow items to the arrows inventory.
 */
public class GuiQuiverClick {
	public void execute(InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Quiver")) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}

			// Prevent all hotbar swapping in a Quiver GUI
			if (e.getAction() == InventoryAction.HOTBAR_SWAP) {
				e.setCancelled(true);
				return;
			}

			// If adding a new item to the arrows inventory
			if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
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
										if (AranarthUtils.verifyIsSameArrow(playerArrows.get(j), selectedArrow) != null) {
											ItemStack stackFromQuiver = playerArrows.get(j).clone();
											// Updates quiver stack with what's in inventory
											playerArrows.set(j, playerInventory[i]);
											// Updates inventory stack with what's in the quiver
											player.getInventory().setItem(i, stackFromQuiver);

											if (stackFromQuiver.hasItemMeta()) {
												// Tipped arrows
												if (stackFromQuiver.getItemMeta() instanceof PotionMeta meta) {
													// If mcMMO arrow
													if (meta.hasCustomEffects()) {
														String arrowName = meta.getCustomEffects().getFirst().getType().getKey().getKey();
														String newName = arrowName.substring(0, 1).toUpperCase();
														newName = newName + arrowName.substring(1);

														Color color = meta.getCustomEffects().getFirst().getType().getColor();
														String rgbAsHex = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
														player.sendMessage(ChatUtils.chatMessage("&7You will now use " + rgbAsHex + newName + " &7Arrows"));
														player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 1);
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
														// Puts the text in the color of the potion
														Color color = meta.getBasePotionType().getPotionEffects().getFirst().getType().getColor();
														String rgbAsHex = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
														player.sendMessage(ChatUtils.chatMessage("&7You will now use " + rgbAsHex + newNameSB + " &7Arrows"));
														player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 1);
													}
												}
												// Custom Arrows
												else {
													if (stackFromQuiver.getItemMeta().getPersistentDataContainer().has(ARROW)) {
														String type = stackFromQuiver.getItemMeta().getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
														ItemStack arrowItem = AranarthUtils.getArrowFromType(type);
														String arrowName = arrowItem.getItemMeta().getDisplayName() + "s";
														player.sendMessage(ChatUtils.chatMessage("&7You will now use " + arrowName));
														player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 1);
													}
												}
											}
											// Regular or Spectral Arrows
											else {
												if (selectedArrow.getType() == Material.ARROW) {
													player.sendMessage(ChatUtils.chatMessage("&7You will now use regular &eArrows"));
													player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 1);
												} else if (selectedArrow.getType() == Material.SPECTRAL_ARROW) {
													player.sendMessage(ChatUtils.chatMessage("&7You will now use &eSpectral Arrows"));
													player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 1);
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
