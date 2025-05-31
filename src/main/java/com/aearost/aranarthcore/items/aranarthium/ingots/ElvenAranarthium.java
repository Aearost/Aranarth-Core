package com.aearost.aranarthcore.items.aranarthium.ingots;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of an Elven Aranarthium Ingot item.
 */
public class ElvenAranarthium {

	/**
	 * @return The Elven Aranarthium Ingot.
	 */
	public static ItemStack getElvenAranarthiumIngot() {
		ItemStack elvenAranarthiumIngot = new ItemStack(Material.ECHO_SHARD, 1);
		ItemMeta meta = elvenAranarthiumIngot.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			elvenAranarthiumIngot.setItemMeta(meta);
		}
	    return elvenAranarthiumIngot;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#FAF0E6&lElven Aranarthium");
	}
	
	public static String getLore() {
		return "&7&oIt is strangely light...";
	}
	
}
