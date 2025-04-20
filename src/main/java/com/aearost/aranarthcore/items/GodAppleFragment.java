package com.aearost.aranarthcore.items;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of a God Apple Fragment item.
 */
public class GodAppleFragment {

	/**
	 * @return The God Apple Fragment.
	 */
	public static ItemStack getGodAppleFragment() {
		ItemStack godAppleFragment = new ItemStack(Material.GOLD_NUGGET, 1);
		ItemMeta meta = godAppleFragment.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			godAppleFragment.setItemMeta(meta);
		}
	    return godAppleFragment;
	}
	
	public static String getName() {
		return "&6God Apple Fragment";
	}
	
	public static String getLore() {
		return "&eA gift from the gods...";
	}
	
}
