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
public class Quiver {

	/**
	 * @return The Quiver.
	 */
	public static ItemStack getQuiver() {
		ItemStack quiver = new ItemStack(Material.BUNDLE, 1);
		ItemMeta meta = quiver.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			quiver.setItemMeta(meta);
		}
	    return quiver;
	}
	
	public static String getName() {
		return "&6&lQuiver";
	}
	
	public static String getLore() {
		return "&eNot a bundle!";
	}
	
}
