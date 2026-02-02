package com.aearost.aranarthcore.items.aranarthium.armour;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.ARMOR_TYPE;

/**
 * Provides the necessary components of an Aquatic Aranarthium Helmet item.
 */
public class AquaticAranarthiumHelmet implements AranarthItem {

	/**
	 * @return The Aquatic Aranarthium Helmet.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.NETHERITE_HELMET, 1);
		ArmorMeta meta = (ArmorMeta) item.getItemMeta();
		meta.setTrim(new ArmorTrim(TrimMaterial.DIAMOND, TrimPattern.COAST));
		meta.addEnchant(Enchantment.AQUA_AFFINITY, 0, false);
		ArrayList<String> lore = new ArrayList<>();
		meta.getPersistentDataContainer().set(ARMOR_TYPE, PersistentDataType.STRING, "aquatic");

		if (Objects.nonNull(meta)) {
//			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "aranarthium_aquatic_helmet");
//			meta.setItemModel(key);
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return ChatUtils.translateToColor("#AEEEEE&lAquatic Aranarthium Helmet");
	}
	
	public String getLore() {
		return "&7&oThe touch of water...";
	}
	
}
