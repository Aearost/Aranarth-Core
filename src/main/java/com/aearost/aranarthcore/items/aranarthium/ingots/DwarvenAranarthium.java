package com.aearost.aranarthcore.items.aranarthium.ingots;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of a Dwarven Aranarthium Ingot item.
 */
public class DwarvenAranarthium {

	/**
	 * @return The Dwarven Aranarthium Ingot.
	 */
	public static ItemStack getDwarvenAranarthiumIngot() {
		ItemStack dwarvenAranarthiumIngot = new ItemStack(Material.ECHO_SHARD, 1);
		ItemMeta meta = dwarvenAranarthiumIngot.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			dwarvenAranarthiumIngot.setItemMeta(meta);
		}
	    return dwarvenAranarthiumIngot;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#708090&lDwarven Aranarthium");
	}
	
	public static String getLore() {
		return "&7&oForged by the Dwarves...";
	}
	
}
