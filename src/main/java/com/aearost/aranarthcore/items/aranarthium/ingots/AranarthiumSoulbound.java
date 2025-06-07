package com.aearost.aranarthcore.items.aranarthium.ingots;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of an Soulbound Aranarthium Ingot item.
 */
public class AranarthiumSoulbound implements AranarthItem {

	/**
	 * @return The Soulbound Aranarthium Ingot.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.ECHO_SHARD, 1);
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
		return ChatUtils.translateToColor("#9400D3&lSoulbound Aranarthium");
	}
	
	public String getLore() {
		return "&7&oBound for life...";
	}
	
}
