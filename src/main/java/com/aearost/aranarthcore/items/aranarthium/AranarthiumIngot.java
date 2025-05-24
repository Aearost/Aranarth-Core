package com.aearost.aranarthcore.items.aranarthium;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of an Aranarthium Ingot item.
 */
public class AranarthiumIngot {

	/**
	 * @return The Aranarthium Ingot.
	 */
	public static ItemStack getAranarthiumIngot() {
		ItemStack aranarthiumIngot = new ItemStack(Material.ECHO_SHARD, 1);
		ItemMeta meta = aranarthiumIngot.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			aranarthiumIngot.setItemMeta(meta);
		}
	    return aranarthiumIngot;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#ffb3b3&lA#ffc7a8&lr#ffdb9d&la#fff093&ln#d7f7a5&la#affeb7&lr#a4f8d8&lt#99f2f9&lh#b1d3f9&li#c9b5f9&lu#e197f9&lm");
	}
	
	public static String getLore() {
		return "&7It seems sacred...";
	}
	
}
