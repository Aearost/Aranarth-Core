package com.aearost.aranarthcore.items;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of a Quiver item.
 */
public class Quiver implements AranarthItem {

	/**
	 * @return The Quiver.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.LIGHT_GRAY_BUNDLE, 1);
		ItemMeta meta = item.getItemMeta();
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
		return "&6&lQuiver";
	}
	
	public String getLore() {
		return "&eStore your arrows!";
	}
	
}
