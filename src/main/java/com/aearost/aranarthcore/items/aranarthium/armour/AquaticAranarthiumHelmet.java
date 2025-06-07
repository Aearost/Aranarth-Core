package com.aearost.aranarthcore.items.aranarthium.armour;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.ArrayList;
import java.util.Objects;

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
		ArrayList<String> lore = new ArrayList<>();

		if (Objects.nonNull(meta)) {
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
