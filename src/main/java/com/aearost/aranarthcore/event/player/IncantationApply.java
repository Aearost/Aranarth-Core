package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumIngot;
import com.aearost.aranarthcore.items.incantation.Incantation;
import com.aearost.aranarthcore.items.incantation.IncantationBeheading;
import com.aearost.aranarthcore.items.incantation.IncantationLifesteal;
import com.aearost.aranarthcore.items.incantation.IncantationMagnetism;
import com.aearost.aranarthcore.items.incantation.IncantationPlentiful;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
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
import java.util.UUID;

import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_LEVEL;
import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_TYPE;
import static com.aearost.aranarthcore.objects.CustomKeys.MAGNETISM_TOOL_ID;

/**
 * Handles the logic behind applying an incantation.
 */
public class IncantationApply {
	public void execute(PlayerDropItemEvent e) {
		Player player = e.getPlayer();
		Item droppedItem = e.getItemDrop();
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!droppedItem.isValid()) return;
				// Collect all floor items in the area (the dropped item plus any already nearby)
				List<Item> allItems = new ArrayList<>();
				allItems.add(droppedItem);
				for (Entity entity : droppedItem.getNearbyEntities(0.25, 0, 0.25)) {
					if (entity instanceof Item nearby) {
						allItems.add(nearby);
					}
				}

				// Find an incantation among all items in the area
				Item incantationFloorItem = null;
				String incantationType = null;
				ItemMeta incantationMeta = null;
				for (Item floorItem : allItems) {
					if (!floorItem.getItemStack().hasItemMeta()) continue;
					ItemMeta meta = floorItem.getItemStack().getItemMeta();
					if (meta.getPersistentDataContainer().has(INCANTATION_TYPE)) {
						incantationFloorItem = floorItem;
						incantationMeta = meta;
						incantationType = meta.getPersistentDataContainer().get(INCANTATION_TYPE, PersistentDataType.STRING);
						break;
					}
				}

				if (incantationFloorItem == null) return;

				// Items other than the incantation
				List<Item> targets = new ArrayList<>(allItems);
				targets.remove(incantationFloorItem);

				if (targets.size() == 1) {
					Item floorItem = targets.get(0);
					ItemStack item = floorItem.getItemStack();
					ItemMeta itemMeta = item.getItemMeta();

					// Do not allow 2 different incantations to be applied to the same item
					if (itemMeta.getPersistentDataContainer().has(INCANTATION_TYPE)) {
						String incantationTypeOnItem = itemMeta.getPersistentDataContainer().get(INCANTATION_TYPE, PersistentDataType.STRING);
						if (!incantationTypeOnItem.equals(incantationType)) {
							player.sendMessage(ChatUtils.chatMessage("&cOnly one incantation can be applied to an item!"));
							return;
						}
					}

					if (incantationType.equals("incantation_beheading")) {
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
							incantationFloorItem.remove();
							floorItem.setItemStack(item);
							player.sendMessage(ChatUtils.chatMessage("&5You have applied the " + incantation.getItem().getItemMeta().getDisplayName()));
							player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
						}
					} else if (incantationType.equals("incantation_magnetism")) {
						if (isTool(item)) {
							Incantation incantation = new IncantationMagnetism();
							String fullIncantationName = ChatUtils.translateToColor(incantation.getColor() + incantation.getIncantationName());

							List<String> lore = itemMeta.getLore();
							if (lore == null) {
								lore = new ArrayList<>();
								lore.add(fullIncantationName);
							} else {
								lore.add(fullIncantationName);
							}
							itemMeta.getPersistentDataContainer().set(INCANTATION_TYPE, PersistentDataType.STRING, "incantation_magnetism");
							itemMeta.getPersistentDataContainer().set(INCANTATION_LEVEL, PersistentDataType.INTEGER, 1);
							itemMeta.getPersistentDataContainer().set(MAGNETISM_TOOL_ID, PersistentDataType.STRING, UUID.randomUUID().toString());
							itemMeta.setLore(lore);
							item.setItemMeta(itemMeta);
							incantationFloorItem.remove();
							floorItem.setItemStack(item);
							player.sendMessage(ChatUtils.chatMessage("&5You have applied the " + incantation.getItem().getItemMeta().getDisplayName()));
							player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
						}
					}
				} else if (targets.size() == 2) {
					Item first = targets.get(0);
					Item second = targets.get(1);

					Item toolItem = null;
					Item aranarthiumItem = null;
					if ((isTool(first.getItemStack()) || isMeleeWeapon(first.getItemStack())) && second.getItemStack().isSimilar(new AranarthiumIngot().getItem())) {
						toolItem = first;
						aranarthiumItem = second;
					} else if (first.getItemStack().isSimilar(new AranarthiumIngot().getItem()) && (isTool(second.getItemStack()) || isMeleeWeapon(second.getItemStack()))) {
						aranarthiumItem = first;
						toolItem = second;
					}

					if (toolItem == null || aranarthiumItem == null) {
						return;
					}

					// Do not allow 2 different incantations to be applied to the same item
					if (toolItem.getItemStack().getItemMeta().getPersistentDataContainer().has(INCANTATION_TYPE)) {
						String incantationTypeOnItem = toolItem.getItemStack().getItemMeta().getPersistentDataContainer().get(INCANTATION_TYPE, PersistentDataType.STRING);
						if (!incantationTypeOnItem.equals(incantationType)) {
							player.sendMessage(ChatUtils.chatMessage("&cOnly one incantation can be applied to an item!"));
							return;
						}
					}

					if (incantationType.equals("incantation_plentiful")) {
						Incantation incantation = new IncantationPlentiful();
						String fullIncantationName = ChatUtils.translateToColor(incantation.getColor() + incantation.getIncantationName());

						// Dynamically apply the incantation description on the item
						List<String> lore = incantationMeta.getLore();
						if (lore == null) {
							lore = new ArrayList<>();
							lore.add(fullIncantationName);
						}

						ItemStack tool = toolItem.getItemStack();
						ItemMeta toolMeta = tool.getItemMeta();
						toolMeta.getPersistentDataContainer().set(INCANTATION_TYPE, PersistentDataType.STRING, "incantation_plentiful");
						toolMeta.getPersistentDataContainer().set(INCANTATION_LEVEL, PersistentDataType.INTEGER, 1);
						toolMeta.setLore(lore);
						tool.setItemMeta(toolMeta);
						toolItem.setItemStack(tool);
						aranarthiumItem.remove();
						incantationFloorItem.remove();
						player.sendMessage(ChatUtils.chatMessage("&5You have applied the " + incantation.getItem().getItemMeta().getDisplayName()));
						player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
					} else if (incantationType.equals("incantation_lifesteal")) {
						if (isMeleeWeapon(toolItem.getItemStack()) && !isExceedingLevel(toolItem.getItemStack())) {
							Incantation incantation = new IncantationLifesteal();
							ItemStack tool = toolItem.getItemStack();
							ItemMeta toolMeta = tool.getItemMeta();
							int level = 1;
							// Increase the existing level
							if (toolMeta.getPersistentDataContainer().has(INCANTATION_LEVEL)) {
								level = toolMeta.getPersistentDataContainer().get(INCANTATION_LEVEL, PersistentDataType.INTEGER);
								level++;
							}
							// Applying as a new incantation
							else {
								toolMeta.getPersistentDataContainer().set(INCANTATION_TYPE, PersistentDataType.STRING, "incantation_lifesteal");
							}
							toolMeta.getPersistentDataContainer().set(INCANTATION_LEVEL, PersistentDataType.INTEGER, level);

							String fullIncantationName = ChatUtils.translateToColor(
									incantation.getColor() + incantation.getIncantationName() + " " + AranarthUtils.getIncantationLevelInNumerals(level));
							List<String> lore = toolMeta.getLore();
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
							toolMeta.setLore(lore);
							tool.setItemMeta(toolMeta);
							toolItem.setItemStack(tool);
							aranarthiumItem.remove();
							incantationFloorItem.remove();
							player.sendMessage(ChatUtils.chatMessage("&5You have applied the " + incantation.getItem().getItemMeta().getDisplayName()));
							player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
						}
					}
				}
			}
		}.runTaskLater(AranarthCore.getInstance(), 30L);
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
				Bukkit.getLogger().info("[AC] Formatting error with the incantation type: " + type);
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
