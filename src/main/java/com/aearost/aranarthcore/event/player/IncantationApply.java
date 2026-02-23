package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.incantation.Incantation;
import com.aearost.aranarthcore.items.incantation.IncantationBeheading;
import com.aearost.aranarthcore.items.incantation.IncantationLifesteal;
import com.aearost.aranarthcore.items.incantation.IncantationPlentiful;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
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

import java.lang.reflect.InvocationTargetException;
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
								ItemMeta itemMeta = item.getItemMeta();

								String incantationTypeOnItem = itemMeta.getPersistentDataContainer().get(INCANTATION_TYPE, PersistentDataType.STRING);
								String incantationTypeBeingApplied = droppedItemMeta.getPersistentDataContainer().get(INCANTATION_TYPE, PersistentDataType.STRING);

								// Do not allow 2 different incantations to be applied to the same item
								if (itemMeta.getPersistentDataContainer().has(INCANTATION_TYPE)) {
									if (!incantationTypeOnItem.equals(incantationTypeBeingApplied)) {
										player.sendMessage(ChatUtils.chatMessage("&cOnly one incantation can be applied to an item!"));
										return;
									}
								}

								if (incantationTypeBeingApplied.equals("incantation_beheading")) {
									if (isMeleeWeapon(item) && !isExceedingLevel(item)) {
										Incantation incantation = new IncantationBeheading();
										int level = 1;
										// Increase the existing level
										if (itemMeta.getPersistentDataContainer().has(INCANTATION_LEVEL)) {
											level = itemMeta.getPersistentDataContainer().get(INCANTATION_LEVEL, PersistentDataType.INTEGER);
											level++;
										}
										// Applying as a new incantation
										else {
											itemMeta.getPersistentDataContainer().set(INCANTATION_TYPE, PersistentDataType.STRING, "incantation_beheading");
										}
										itemMeta.getPersistentDataContainer().set(INCANTATION_LEVEL, PersistentDataType.INTEGER, level);

										String fullIncantationName = ChatUtils.translateToColor(
												incantation.getColor() + incantation.getIncantationName() + " " + AranarthUtils.getIncantationLevelInNumerals(level));
										// Dynamically apply the incantation description on the item
										List<String> lore = itemMeta.getLore();
										if (lore == null) {
											lore = new ArrayList<>();
											lore.add(fullIncantationName);
										} else {
											for (int i = 0; i < lore.size(); i++) {
												if (ChatUtils.stripColorFormatting(lore.get(i)).startsWith(incantation.getIncantationName())) {
													lore.set(i, fullIncantationName);
													break;
												}
											}
										}
										itemMeta.setLore(lore);
										item.setItemMeta(itemMeta);
										droppedItem.remove();
										floorItem.setItemStack(item);
										player.sendMessage(ChatUtils.chatMessage("&5You have applied the " + incantation.getItem().getItemMeta().getDisplayName()));
										player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
									}
								}
								else if (incantationTypeBeingApplied.equals("incantation_lifesteal")) {
									if (isMeleeWeapon(item) && !isExceedingLevel(item)) {
										Incantation incantation = new IncantationLifesteal();
										int level = 1;
										// Increase the existing level
										if (itemMeta.getPersistentDataContainer().has(INCANTATION_LEVEL)) {
											level = itemMeta.getPersistentDataContainer().get(INCANTATION_LEVEL, PersistentDataType.INTEGER);
											level++;
										}
										// Applying as a new incantation
										else {
											itemMeta.getPersistentDataContainer().set(INCANTATION_TYPE, PersistentDataType.STRING, "incantation_lifesteal");
										}
										itemMeta.getPersistentDataContainer().set(INCANTATION_LEVEL, PersistentDataType.INTEGER, level);

										String fullIncantationName = ChatUtils.translateToColor(
												incantation.getColor() + incantation.getIncantationName() + " " + AranarthUtils.getIncantationLevelInNumerals(level));
										// Dynamically apply the incantation description on the item
										List<String> lore = itemMeta.getLore();
										if (lore == null) {
											lore = new ArrayList<>();
											lore.add(fullIncantationName);
										} else {
											for (int i = 0; i < lore.size(); i++) {
												if (ChatUtils.stripColorFormatting(lore.get(i)).startsWith(incantation.getIncantationName())) {
													lore.set(i, fullIncantationName);
													break;
												}
											}
										}

										itemMeta.setLore(lore);
										item.setItemMeta(itemMeta);
										droppedItem.remove();
										floorItem.setItemStack(item);
										player.sendMessage(ChatUtils.chatMessage("&5You have applied the " + incantation.getItem().getItemMeta().getDisplayName()));
										player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
									}
								} else if (incantationTypeBeingApplied.equals("incantation_plentiful")) {
									if (isTool(item) && !isExceedingLevel(item)) {
										Incantation incantation = new IncantationPlentiful();
										int level = 1;
										itemMeta.getPersistentDataContainer().set(INCANTATION_TYPE, PersistentDataType.STRING, "incantation_plentiful");
										itemMeta.getPersistentDataContainer().set(INCANTATION_LEVEL, PersistentDataType.INTEGER, level);
										String fullIncantationName = ChatUtils.translateToColor(
												incantation.getColor() + incantation.getIncantationName() + " " + AranarthUtils.getIncantationLevelInNumerals(level));

										// Dynamically apply the incantation description on the item
										List<String> lore = itemMeta.getLore();
										if (lore == null) {
											lore = new ArrayList<>();
											lore.add(fullIncantationName);
										}

										itemMeta.setLore(lore);
										item.setItemMeta(itemMeta);
										droppedItem.remove();
										floorItem.setItemStack(item);
										player.sendMessage(ChatUtils.chatMessage("&5You have applied the " + incantation.getItem().getItemMeta().getDisplayName()));
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
	 * Determines if the item is exceeding the level of the incantation applied to it.
	 * @param item The item.
	 * @return Whether the item is exceeding the level of the incantation applied to it.
	 */
	private boolean isExceedingLevel(ItemStack item) {
		// Can only apply if the current level on the item does not exceed the maximum value of the incantation
		ItemMeta meta = item.getItemMeta();
		if (meta.getPersistentDataContainer().has(INCANTATION_LEVEL)) {
			String type = meta.getPersistentDataContainer().get(INCANTATION_TYPE, PersistentDataType.STRING);
			String[] parts = type.split("_");
			parts[0] = parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1);
			parts[1] = parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1);
			type = parts[0] + parts[1];

			// Dynamically instantiates the object
			Object instance = null;
			try {
				Class<?> unknownClass = Class.forName("com.aearost.aranarthcore.items.incantation." + type);
				instance = unknownClass.getDeclaredConstructor().newInstance();
			} catch (ClassNotFoundException | InvocationTargetException | InstantiationException
					 | IllegalAccessException | NoSuchMethodException e) {
				Bukkit.getLogger().info("Formatting error with the incantation type: " + type);
				return true;
			}

			if (instance instanceof Incantation incantation) {
				int levelOnItem = meta.getPersistentDataContainer().get(INCANTATION_LEVEL, PersistentDataType.INTEGER);
				if (levelOnItem < incantation.getLevelLimit()) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Determines if the input item is eligible for a weapon.
	 * @param item The item.
	 * @return Whether the input item can have the Beheading incantation applied to it.
	 */
	private boolean isMeleeWeapon(ItemStack item) {
		// Can only be applied to melee weapons
		if (!item.getType().name().endsWith("_SWORD") && !item.getType().name().endsWith("_AXE")
			&& !item.getType().name().endsWith("_SPEAR") && item.getType() != Material.MACE && item.getType() != Material.TRIDENT) {
			return false;
		}
		return true;
	}

	/**
	 * Determines if the input item can have the Beheading incantation applied to it.
	 * @param item The item.
	 * @return Whether the input item can have the Beheading incantation applied to it.
	 */
	private boolean isTool(ItemStack item) {
		if (!item.getType().name().endsWith("_PICKAXE") && !item.getType().name().endsWith("_AXE")
				&& !item.getType().name().endsWith("_SHOVEL") && !item.getType().name().endsWith("_HOE")) {
			return false;
		}
		return true;
	}
}
