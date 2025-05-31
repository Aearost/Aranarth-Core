package com.aearost.aranarthcore.items.aranarthium.ingots;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of a Mannish Aranarthium Ingot item.
 */
public class MannishAranarthium {

	/**
	 * @return The Mannish Aranarthium Ingot.
	 */
	public static ItemStack getMannishAranarthiumIngot() {
		ItemStack mannishAranarthiumIngot = new ItemStack(Material.ECHO_SHARD, 1);
		ItemMeta meta = mannishAranarthiumIngot.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			mannishAranarthiumIngot.setItemMeta(meta);
		}
	    return mannishAranarthiumIngot;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#696969&lMannish Aranarthium");
	}
	
	public static String getLore() {
		return "&7&oYou feel its strength...";
	}
	
}
