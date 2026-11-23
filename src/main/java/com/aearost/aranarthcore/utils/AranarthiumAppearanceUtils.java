package com.aearost.aranarthcore.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

import static com.aearost.aranarthcore.objects.CustomKeys.ARMOR_TYPE;

/**
 * Handles how Aranarthium armour looks when worn.
 */
public class AranarthiumAppearanceUtils {

	private static final Set<String> CUSTOM_SETS = Set.of("dwarven", "aquatic", "ardent", "echo", "elven", "fae");

	/**
	 * @param set The Aranarthium set (e.g. "dwarven").
	 * @return Whether the set has its own worn textures in the resource pack.
	 */
	public static boolean hasCustomAppearance(String set) {
		return set != null && CUSTOM_SETS.contains(set);
	}

	/**
	 * Applies the set's worn appearance to the meta of an Aranarthium armour piece or elytra.
	 *
	 * @param meta The item's meta. The caller is responsible for setting it back on the item.
	 * @param set The Aranarthium set (e.g. "dwarven").
	 * @param type The item's material (NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS or ELYTRA).
	 */
	public static void applyAppearance(ItemMeta meta, String set, Material type) {
		if (meta == null || !hasCustomAppearance(set)) {
			return;
		}
		EquippableComponent equippable = meta.getEquippable();
		switch (type) {
			case NETHERITE_HELMET -> {
				equippable.setSlot(EquipmentSlot.HEAD);
				// No equipment asset: the head layer then renders the item's 3D model (items JSON "head" case)
				equippable.setModel(null);
				equippable.setEquipSound(Sound.ITEM_ARMOR_EQUIP_NETHERITE);
			}
			case NETHERITE_CHESTPLATE -> setArmour(equippable, EquipmentSlot.CHEST, set);
			case NETHERITE_LEGGINGS -> setArmour(equippable, EquipmentSlot.LEGS, set);
			case NETHERITE_BOOTS -> setArmour(equippable, EquipmentSlot.FEET, set);
			case ELYTRA -> {
				equippable.setSlot(EquipmentSlot.CHEST);
				equippable.setModel(new NamespacedKey("aranarthcore", set + "_elytra"));
				equippable.setEquipSound(Sound.ITEM_ARMOR_EQUIP_ELYTRA);
				// Matches vanilla elytra: durability is only used up by flying
				equippable.setDamageOnHurt(false);
			}
			default -> {
				return;
			}
		}
		meta.setEquippable(equippable);
		if (meta instanceof ArmorMeta armorMeta) {
			armorMeta.setTrim(null);
		}
	}

	private static void setArmour(EquippableComponent equippable, EquipmentSlot slot, String set) {
		equippable.setSlot(slot);
		equippable.setModel(new NamespacedKey("aranarthcore", set));
		equippable.setEquipSound(Sound.ITEM_ARMOR_EQUIP_NETHERITE);
	}

	/**
	 * Gives an Aranarthium piece made before its set had its own textures the new appearance.
	 *
	 * @param item The item to migrate.
	 * @return Whether the item was changed.
	 */
	public static boolean migrateItem(ItemStack item) {
		if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		String set = meta.getPersistentDataContainer().get(ARMOR_TYPE, PersistentDataType.STRING);
		// Pieces that already have an equippable were made with (or migrated to) the new appearance
		if (!hasCustomAppearance(set) || meta.hasEquippable()) {
			return false;
		}
		applyAppearance(meta, set, item.getType());
		if (!meta.hasEquippable()) {
			return false;
		}
		item.setItemMeta(meta);
		return true;
	}

	/**
	 * Scans every slot of the given inventory (including armour and off-hand slots of a player inventory)
	 * and migrates any old Aranarthium pieces.
	 *
	 * @param inventory The inventory to scan.
	 */
	public static void migrateInventory(Inventory inventory) {
		for (int i = 0; i < inventory.getSize(); i++) {
			ItemStack item = inventory.getItem(i);
			if (migrateItem(item)) {
				inventory.setItem(i, item);
			}
		}
	}
}
