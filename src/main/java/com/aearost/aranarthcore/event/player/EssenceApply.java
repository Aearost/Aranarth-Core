package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumIngot;
import com.aearost.aranarthcore.items.essence.Essence;
import com.aearost.aranarthcore.items.essence.EssenceBeheading;
import com.aearost.aranarthcore.items.essence.EssenceLifesteal;
import com.aearost.aranarthcore.items.essence.EssenceMagnetism;
import com.aearost.aranarthcore.items.essence.EssencePlentiful;
import com.aearost.aranarthcore.items.essence.EssencePreservation;
import com.aearost.aranarthcore.items.essence.EssenceResilience;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
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

import static com.aearost.aranarthcore.objects.CustomKeys.ESSENCE_LEVEL;
import static com.aearost.aranarthcore.objects.CustomKeys.ESSENCE_TYPE;
import static com.aearost.aranarthcore.objects.CustomKeys.MAGNETISM_TOOL_ID;
import static com.aearost.aranarthcore.objects.CustomKeys.PRESERVATION_USES;

/**
 * Handles the logic behind applying an essence.
 * // Yes, this is issue #420. We had to make it dank.
 */
public class EssenceApply {
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

				// Find an essence among all items in the area
				Item essenceFloorItem = null;
				String essenceType = null;
				ItemMeta essenceMeta = null;
				for (Item floorItem : allItems) {
					if (!floorItem.getItemStack().hasItemMeta()) continue;
					ItemMeta meta = floorItem.getItemStack().getItemMeta();
					if (meta.getPersistentDataContainer().has(ESSENCE_TYPE)) {
						essenceFloorItem = floorItem;
						essenceMeta = meta;
						essenceType = meta.getPersistentDataContainer().get(ESSENCE_TYPE, PersistentDataType.STRING);
						break;
					}
				}

				if (essenceFloorItem == null) return;

				// Items other than the essence
				List<Item> targets = new ArrayList<>(allItems);
				targets.remove(essenceFloorItem);

				if (targets.size() == 1) {
					Item floorItem = targets.get(0);
					ItemStack item = floorItem.getItemStack();
					ItemMeta itemMeta = item.getItemMeta();

					// Do not allow 2 different essences to be applied to the same item
					if (itemMeta.getPersistentDataContainer().has(ESSENCE_TYPE)) {
						String essenceTypeOnItem = itemMeta.getPersistentDataContainer().get(ESSENCE_TYPE, PersistentDataType.STRING);
						if (!essenceTypeOnItem.equals(essenceType)) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("essence.one_only")));
							return;
						}
					}

					if (essenceType.equals("essence_beheading")) {
						if (isMeleeWeapon(item) && !isExceedingLevel(item)) {
							Essence essence = new EssenceBeheading();
							int level = 1;
							// Increase the existing level
							if (itemMeta.getPersistentDataContainer().has(ESSENCE_LEVEL)) {
								level = itemMeta.getPersistentDataContainer().get(ESSENCE_LEVEL, PersistentDataType.INTEGER);
								level++;
							}
							// Applying as a new essence
							else {
								itemMeta.getPersistentDataContainer().set(ESSENCE_TYPE, PersistentDataType.STRING, "essence_beheading");
							}
							itemMeta.getPersistentDataContainer().set(ESSENCE_LEVEL, PersistentDataType.INTEGER, level);

							String fullEssenceName = ChatUtils.translateToColor(
									essence.getColor() + essence.getEssenceName() + " " + AranarthUtils.getEssenceLevelInNumerals(level));
							// Dynamically apply the essence description on the item
							List<String> lore = itemMeta.getLore();
							if (lore == null) {
								lore = new ArrayList<>();
								lore.add(fullEssenceName);
							} else {
								for (int i = 0; i < lore.size(); i++) {
									if (ChatUtils.stripColorFormatting(lore.get(i)).startsWith(essence.getEssenceName())) {
										lore.set(i, fullEssenceName);
										break;
									}
								}
							}
							itemMeta.setLore(lore);
							item.setItemMeta(itemMeta);
							essenceFloorItem.remove();
							floorItem.setItemStack(item);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("essence.applied", "name", essence.getItem().getItemMeta().getDisplayName())));
							player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
						}
					} else if (essenceType.equals("essence_resilience")) {
						if (isDamageable(item) && !itemMeta.getPersistentDataContainer().has(ESSENCE_TYPE)) {
							Essence essence = new EssenceResilience();
							String fullEssenceName = ChatUtils.translateToColor(essence.getColor() + essence.getEssenceName());

							List<String> lore = itemMeta.getLore();
							if (lore == null) {
								lore = new ArrayList<>();
							}
							lore.add(fullEssenceName);
							itemMeta.getPersistentDataContainer().set(ESSENCE_TYPE, PersistentDataType.STRING, "essence_resilience");
							itemMeta.getPersistentDataContainer().set(ESSENCE_LEVEL, PersistentDataType.INTEGER, 1);
							itemMeta.setLore(lore);
							item.setItemMeta(itemMeta);
							essenceFloorItem.remove();
							floorItem.setItemStack(item);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("essence.applied", "name", essence.getItem().getItemMeta().getDisplayName())));
							player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
						}
					}
				} else if (targets.size() == 2) {
					Item first = targets.get(0);
					Item second = targets.get(1);

					Item toolItem = null;
					Item aranarthiumItem = null;
					if ((isTool(first.getItemStack()) || isMeleeWeapon(first.getItemStack()) || first.getItemStack().getType() == Material.SHEARS) && second.getItemStack().isSimilar(new AranarthiumIngot().getItem())) {
						toolItem = first;
						aranarthiumItem = second;
					} else if (first.getItemStack().isSimilar(new AranarthiumIngot().getItem()) && (isTool(second.getItemStack()) || isMeleeWeapon(second.getItemStack()) || second.getItemStack().getType() == Material.SHEARS)) {
						aranarthiumItem = first;
						toolItem = second;
					}

					if (toolItem == null || aranarthiumItem == null) {
						return;
					}

					// Do not allow 2 different essences to be applied to the same item
					if (toolItem.getItemStack().getItemMeta().getPersistentDataContainer().has(ESSENCE_TYPE)) {
						String essenceTypeOnItem = toolItem.getItemStack().getItemMeta().getPersistentDataContainer().get(ESSENCE_TYPE, PersistentDataType.STRING);
						if (!essenceTypeOnItem.equals(essenceType)) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("essence.one_only")));
							return;
						}
					}

					if (essenceType.equals("essence_plentiful")) {
						if (isExceedingLevel(toolItem.getItemStack())) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("essence.already_max_level")));
							return;
						}
						Essence essence = new EssencePlentiful();
						String fullEssenceName = ChatUtils.translateToColor(essence.getColor() + essence.getEssenceName());

						ItemStack tool = toolItem.getItemStack();
						ItemMeta toolMeta = tool.getItemMeta();
						List<String> lore = toolMeta.getLore();
						if (lore == null) {
							lore = new ArrayList<>();
							lore.add(fullEssenceName);
						}
						toolMeta.getPersistentDataContainer().set(ESSENCE_TYPE, PersistentDataType.STRING, "essence_plentiful");
						toolMeta.getPersistentDataContainer().set(ESSENCE_LEVEL, PersistentDataType.INTEGER, 1);
						toolMeta.setLore(lore);
						tool.setItemMeta(toolMeta);
						toolItem.setItemStack(tool);
						aranarthiumItem.remove();
						essenceFloorItem.remove();
						player.sendMessage(ChatUtils.chatMessage(Lang.get("essence.applied", "name", essence.getItem().getItemMeta().getDisplayName())));
						player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
					} else if (essenceType.equals("essence_lifesteal")) {
						if (isMeleeWeapon(toolItem.getItemStack()) && !isExceedingLevel(toolItem.getItemStack())) {
							Essence essence = new EssenceLifesteal();
							ItemStack tool = toolItem.getItemStack();
							ItemMeta toolMeta = tool.getItemMeta();
							int level = 1;
							// Increase the existing level
							if (toolMeta.getPersistentDataContainer().has(ESSENCE_LEVEL)) {
								level = toolMeta.getPersistentDataContainer().get(ESSENCE_LEVEL, PersistentDataType.INTEGER);
								level++;
							}
							// Applying as a new essence
							else {
								toolMeta.getPersistentDataContainer().set(ESSENCE_TYPE, PersistentDataType.STRING, "essence_lifesteal");
							}
							toolMeta.getPersistentDataContainer().set(ESSENCE_LEVEL, PersistentDataType.INTEGER, level);

							String fullEssenceName = ChatUtils.translateToColor(
									essence.getColor() + essence.getEssenceName() + " " + AranarthUtils.getEssenceLevelInNumerals(level));
							List<String> lore = toolMeta.getLore();
							if (lore == null) {
								lore = new ArrayList<>();
								lore.add(fullEssenceName);
							} else {
								for (int i = 0; i < lore.size(); i++) {
									if (ChatUtils.stripColorFormatting(lore.get(i)).startsWith(essence.getEssenceName())) {
										lore.set(i, fullEssenceName);
										break;
									}
								}
							}
							toolMeta.setLore(lore);
							tool.setItemMeta(toolMeta);
							toolItem.setItemStack(tool);
							aranarthiumItem.remove();
							essenceFloorItem.remove();
							player.sendMessage(ChatUtils.chatMessage(Lang.get("essence.applied", "name", essence.getItem().getItemMeta().getDisplayName())));
							player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
						}
					} else if (essenceType.equals("essence_magnetism")) {
						if (isExceedingLevel(toolItem.getItemStack())) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("essence.already_max_level")));
							return;
						}
						if (isTool(toolItem.getItemStack())) {
							Essence essence = new EssenceMagnetism();
							ItemStack tool = toolItem.getItemStack();
							ItemMeta toolMeta = tool.getItemMeta();
							String fullEssenceName = ChatUtils.translateToColor(essence.getColor() + essence.getEssenceName());

							List<String> lore = toolMeta.getLore();
							if (lore == null) {
								lore = new ArrayList<>();
								lore.add(fullEssenceName);
							} else {
								lore.add(fullEssenceName);
							}
							toolMeta.getPersistentDataContainer().set(ESSENCE_TYPE, PersistentDataType.STRING, "essence_magnetism");
							toolMeta.getPersistentDataContainer().set(ESSENCE_LEVEL, PersistentDataType.INTEGER, 1);
							toolMeta.getPersistentDataContainer().set(MAGNETISM_TOOL_ID, PersistentDataType.STRING, UUID.randomUUID().toString());
							toolMeta.setLore(lore);
							tool.setItemMeta(toolMeta);
							toolItem.setItemStack(tool);
							aranarthiumItem.remove();
							essenceFloorItem.remove();
							player.sendMessage(ChatUtils.chatMessage(Lang.get("essence.applied", "name", essence.getItem().getItemMeta().getDisplayName())));
							player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
						}
					} else if (essenceType.equals("essence_preservation")) {
						if (isExceedingLevel(toolItem.getItemStack())) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("essence.already_max_level")));
							return;
						}
						if (isPickaxe(toolItem.getItemStack())) {
							if (hasFortune(toolItem.getItemStack())) {
								player.sendMessage(ChatUtils.chatMessage(Lang.get("essence.preservation_no_fortune")));
								return;
							}
							Essence essence = new EssencePreservation();
							ItemStack tool = toolItem.getItemStack();
							ItemMeta toolMeta = tool.getItemMeta();
							String fullEssenceName = ChatUtils.translateToColor(essence.getColor() + essence.getEssenceName());

							List<String> lore = toolMeta.getLore();
							if (lore == null) {
								lore = new ArrayList<>();
								lore.add(fullEssenceName);
							} else {
								lore.add(fullEssenceName);
							}
							toolMeta.getPersistentDataContainer().set(ESSENCE_TYPE, PersistentDataType.STRING, "essence_preservation");
							toolMeta.getPersistentDataContainer().set(ESSENCE_LEVEL, PersistentDataType.INTEGER, 1);
							toolMeta.getPersistentDataContainer().set(PRESERVATION_USES, PersistentDataType.INTEGER, 3);
							toolMeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
							toolMeta.setLore(lore);
							tool.setItemMeta(toolMeta);
							toolItem.setItemStack(tool);
							aranarthiumItem.remove();
							essenceFloorItem.remove();
							player.sendMessage(ChatUtils.chatMessage(Lang.get("essence.applied", "name", essence.getItem().getItemMeta().getDisplayName())));
							player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.5F);
						}
					}
				}
			}
		}.runTaskLater(AranarthCore.getInstance(), 30L);
	}

	/**
	 * Determines if the item is exceeding the level of the essence applied to it.
	 * @param item The item.
	 * @return Whether the item is exceeding the level of the essence applied to it.
	 */
	private boolean isExceedingLevel(ItemStack item) {
		// Can only apply if the current level on the item does not exceed the maximum value of the essence
		ItemMeta meta = item.getItemMeta();
		if (meta.getPersistentDataContainer().has(ESSENCE_LEVEL)) {
			String type = meta.getPersistentDataContainer().get(ESSENCE_TYPE, PersistentDataType.STRING);
			String[] parts = type.split("_");
			parts[0] = parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1);
			parts[1] = parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1);
			type = parts[0] + parts[1];

			// Dynamically instantiates the object
			Object instance = null;
			try {
				Class<?> unknownClass = Class.forName("com.aearost.aranarthcore.items.essence." + type);
				instance = unknownClass.getDeclaredConstructor().newInstance();
			} catch (ClassNotFoundException | InvocationTargetException | InstantiationException
					 | IllegalAccessException | NoSuchMethodException e) {
				Bukkit.getLogger().info("[AC] Formatting error with the essence type: " + type);
				return true;
			}

			if (instance instanceof Essence essence) {
				int levelOnItem = meta.getPersistentDataContainer().get(ESSENCE_LEVEL, PersistentDataType.INTEGER);
				if (levelOnItem < essence.getLevelLimit()) {
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
	 * @return Whether the input item can have the Beheading essence applied to it.
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
	 * Determines if the input item has durability (i.e., tools, weapons, armor).
	 * @param item The item.
	 * @return Whether the item can take durability damage.
	 */
	private boolean isDamageable(ItemStack item) {
		return item.getType().getMaxDurability() > 0;
	}

	/**
	 * Determines if the input item can have the Beheading essence applied to it.
	 * @param item The item.
	 * @return Whether the input item can have the Beheading essence applied to it.
	 */
	private boolean isTool(ItemStack item) {
		if (!item.getType().name().endsWith("_PICKAXE") && !item.getType().name().endsWith("_AXE")
				&& !item.getType().name().endsWith("_SHOVEL") && !item.getType().name().endsWith("_HOE")) {
			return false;
		}
		return true;
	}

	/**
	 * Determines if the item is a pickaxe.
	 * @param item The item.
	 * @return Whether the item is a pickaxe.
	 */
	private boolean isPickaxe(ItemStack item) {
		return item.getType().name().endsWith("_PICKAXE");
	}

	/**
	 * Determines if the item has the Fortune enchantment.
	 * @param item The item.
	 * @return Whether the item has Fortune.
	 */
	private boolean hasFortune(ItemStack item) {
		return item.containsEnchantment(Enchantment.FORTUNE);
	}
}
