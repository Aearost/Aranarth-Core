package com.aearost.aranarthcore.items;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.items.CustomItemKeys.HOMEPAD;


public class Homepad implements AranarthItem {

	/**
	 * @return The HomePad.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(HOMEPAD, PersistentDataType.STRING, "homepad");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&6&lHome Pad";
	}
	
	public String getLore() {
		return "&ePlace this to set up a home pad!";
	}
	
}
