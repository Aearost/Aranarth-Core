package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.incantation.IncantationBeheading;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_LEVEL;
import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_TYPE;

/**
 * Handles the logic behind applying an incantation.
 */
public class IncantationApply {
	public void execute(PlayerDropItemEvent e) {
		Player player = e.getPlayer();
		Item droppedItem = e.getItemDrop();
		if (droppedItem.getItemStack().hasItemMeta()) {
			ItemMeta droppedItemMeta = droppedItem.getItemStack().getItemMeta();
			if (droppedItemMeta.getPersistentDataContainer().has(INCANTATION_TYPE)) {
				new BukkitRunnable() {
					@Override
					public void run() {
						Location incantationLoc = droppedItem.getLocation();
						List<Entity> nearby = droppedItem.getNearbyEntities(0.25, 0, 0.25);
						// Do not apply if there are several options
						if (nearby.size() == 1) {
							Entity entity = nearby.getFirst();
							if (entity instanceof Item floorItem) {
								ItemStack item = floorItem.getItemStack();
								String type = droppedItemMeta.getPersistentDataContainer().get(INCANTATION_TYPE, PersistentDataType.STRING);

								if (type.equals("beheading")) {
									if (isMeleeWeapon(item)) {
										ItemMeta itemMeta = item.getItemMeta();
										int level = 1;
										// Increase the existing level
										if (itemMeta.getPersistentDataContainer().has(INCANTATION_LEVEL)) {
											level = itemMeta.getPersistentDataContainer().get(INCANTATION_LEVEL, PersistentDataType.INTEGER);
											level++;
										}
										// Applying as a new incantation
										else {
											itemMeta.getPersistentDataContainer().set(INCANTATION_TYPE, PersistentDataType.STRING, "beheading");
										}
										itemMeta.getPersistentDataContainer().set(INCANTATION_LEVEL, PersistentDataType.INTEGER, level);

										String fullIncantationName = ChatUtils.translateToColor("&7Beheading " + AranarthUtils.getIncantationLevelInNumerals(level));
										// Dynamically apply the incantation description on the item
										List<String> lore = itemMeta.getLore();
										if (lore == null) {
											lore = new ArrayList<>();
											lore.add(fullIncantationName);
										} else {
											for (int i = 0; i < lore.size(); i++) {
												if (ChatUtils.stripColorFormatting(lore.get(i)).startsWith("Beheading ")) {
													lore.set(i, fullIncantationName);
													break;
												}
											}
										}

										itemMeta.setLore(lore);
										item.setItemMeta(itemMeta);
										droppedItem.remove();
										floorItem.setItemStack(item);
										player.sendMessage(ChatUtils.chatMessage("&5You have applied the &7Beheading &5incantation!"));
										player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
									}
								}
								else if (type.equals("lifesteal")) {
									if (isMeleeWeapon(item)) {
										ItemMeta itemMeta = item.getItemMeta();
										int level = 1;
										// Increase the existing level
										if (itemMeta.getPersistentDataContainer().has(INCANTATION_LEVEL)) {
											level = itemMeta.getPersistentDataContainer().get(INCANTATION_LEVEL, PersistentDataType.INTEGER);
											level++;
										}
										// Applying as a new incantation
										else {
											itemMeta.getPersistentDataContainer().set(INCANTATION_TYPE, PersistentDataType.STRING, "lifesteal");
										}
										itemMeta.getPersistentDataContainer().set(INCANTATION_LEVEL, PersistentDataType.INTEGER, level);

										String fullIncantationName = ChatUtils.translateToColor("&aLifesteal " + AranarthUtils.getIncantationLevelInNumerals(level));
										// Dynamically apply the incantation description on the item
										List<String> lore = itemMeta.getLore();
										if (lore == null) {
											lore = new ArrayList<>();
											lore.add(fullIncantationName);
										} else {
											for (int i = 0; i < lore.size(); i++) {
												if (ChatUtils.stripColorFormatting(lore.get(i)).startsWith("Lifesteal ")) {
													lore.set(i, fullIncantationName);
													break;
												}
											}
										}

										itemMeta.setLore(lore);
										item.setItemMeta(itemMeta);
										droppedItem.remove();
										floorItem.setItemStack(item);
										player.sendMessage(ChatUtils.chatMessage("&5You have applied the &aLifesteal &5incantation!"));
										player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
									}
								}
							}
						}
					}
				}.runTaskLater(AranarthCore.getInstance(), 30L);
			}
		}
	}

	/**
	 * Determines if the input item can have the Beheading incantation applied to it.
	 * @param item The item.
	 * @return Whether the input item can have the Beheading incantation applied to it.
	 */
	private boolean isMeleeWeapon(ItemStack item) {
		// Can only be applied to melee weapons
		if (!item.getType().name().endsWith("_SWORD") && !item.getType().name().endsWith("_AXE")
			&& !item.getType().name().endsWith("_SPEAR") && item.getType() != Material.MACE && item.getType() != Material.TRIDENT) {
			return false;
		}

		// Can only apply if the weapon's level does not exceed the maximum value of the incantation
		ItemMeta meta = item.getItemMeta();
		if (meta.getPersistentDataContainer().has(INCANTATION_LEVEL)) {
			int levelOnItem = meta.getPersistentDataContainer().get(INCANTATION_LEVEL, PersistentDataType.INTEGER);
			if (levelOnItem >= new IncantationBeheading().getLevelLimit()) {
				return false;
			}
		}

		return true;
	}
}
