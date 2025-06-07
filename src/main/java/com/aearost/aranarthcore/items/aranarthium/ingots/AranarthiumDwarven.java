package com.aearost.aranarthcore.items.aranarthium.ingots;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of a Dwarven Aranarthium Ingot item.
 */
public class AranarthiumDwarven implements AranarthItem {

	/**
	 * @return The Dwarven Aranarthium Ingot.
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
		return ChatUtils.translateToColor("#708090&lDwarven Aranarthium");
	}
	
	public String getLore() {
		return "&7&oForged by the Dwarves...";
	}
	
}
