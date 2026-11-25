package com.aearost.aranarthcore.items.aranarthium.armour;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.AranarthiumAppearanceUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.ARMOR_TYPE;

/**
 * Provides the necessary components of a Soulbound Aranarthium Chestplate item.
 */
public class SoulboundAranarthiumChestplate implements AranarthItem {

	/**
	 * @return The Soulbound Aranarthium Chestplate.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.NETHERITE_CHESTPLATE, 1);
		ArmorMeta meta = (ArmorMeta) item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "aranarthium_soulbound_chestplate");
			meta.setItemModel(key);
			AranarthiumAppearanceUtils.applyAppearance(meta, "soulbound", Material.NETHERITE_CHESTPLATE);
			meta.addEnchant(Enchantment.MENDING, 1, true);
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(ARMOR_TYPE, PersistentDataType.STRING, "soulbound");

			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			meta.setFireResistant(true);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&#9400D3&lSoulbound Aranarthium Chestplate";
	}
	
	public String getLore() {
		return "&7&oBound for life...";
	}
	
}
