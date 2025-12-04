package com.aearost.aranarthcore.items.aranarthium.armour;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomItemKeys.ARMOR_TYPE;

/**
 * Provides the necessary components of an Aquatic Aranarthium Boots item.
 */
public class AquaticAranarthiumBoots implements AranarthItem {

	/**
	 * @return The Aquatic Aranarthium Helmet.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.NETHERITE_BOOTS, 1);
		ArmorMeta meta = (ArmorMeta) item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "aranarthium_aquatic_boots");
			meta.setItemModel(key);
			meta.setTrim(new ArmorTrim(TrimMaterial.DIAMOND, TrimPattern.TIDE));
			meta.addEnchant(Enchantment.DEPTH_STRIDER, 5, true);
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(ARMOR_TYPE, PersistentDataType.STRING, "aquatic");

			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return ChatUtils.translateToColor("#AEEEEE&lAquatic Aranarthium Boots");
	}
	
	public String getLore() {
		return "&7&oThe touch of water...";
	}
	
}
