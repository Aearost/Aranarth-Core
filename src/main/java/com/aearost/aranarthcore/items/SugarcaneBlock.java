package com.aearost.aranarthcore.items;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of a Sugarcane Block item.
 */
public class SugarcaneBlock implements AranarthItem {

	/**
	 * @return The Sugarcane Block.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.BAMBOO_BLOCK, 1);
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
		return "&aBlock of Sugarcane";
	}
	
	public String getLore() {
		return "&7An efficient way to be stored!";
	}
	
}
