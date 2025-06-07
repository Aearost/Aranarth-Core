package com.aearost.aranarthcore.items;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of a Honey Glazed Ham item.
 */
public class HoneyGlazedHam implements AranarthItem {

	/**
	 * @return The Honey Glazed Ham.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.COOKED_PORKCHOP, 1);
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
		return "&6Honey Glazed Ham";
	}
	
	public String getLore() {
		return "&eAin't that sweet?";
	}
	
}
