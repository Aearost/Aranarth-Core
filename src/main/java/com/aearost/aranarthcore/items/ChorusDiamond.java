package com.aearost.aranarthcore.items;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of a Chorus Diamond item.
 */
public class ChorusDiamond {

	/**
	 * @return The Chorus Diamond.
	 */
	public static ItemStack getChorusDiamond() {
		ItemStack chorusDiamond = new ItemStack(Material.DIAMOND, 1);
		ItemMeta meta = chorusDiamond.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			chorusDiamond.setItemMeta(meta);
		}
	    return chorusDiamond;
	}
	
	public static String getName() {
		return "&5&lChorus Diamond";
	}
	
	public static String getLore() {
		return "&bYou can't eat this one...";
	}
	
}
