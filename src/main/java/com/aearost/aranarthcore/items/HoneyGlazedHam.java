package com.aearost.aranarthcore.items;

import java.util.ArrayList;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.aearost.aranarthcore.utils.ChatUtils;

/**
 * Provides the necessary components of a Honey Glazed Ham item.
 */
public class HoneyGlazedHam {

	/**
	 * @return The Honey Glazed Ham.
	 */
	public static ItemStack getHoneyGlazedHam() {
		ItemStack honeyGlazeHam = new ItemStack(Material.COOKED_PORKCHOP, 1);
		ItemMeta meta = honeyGlazeHam.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			honeyGlazeHam.setItemMeta(meta);
		}
	    return honeyGlazeHam;
	}
	
	public static String getName() {
		return "&6Honey Glazed Ham";
	}
	
	public static String getLore() {
		return "&eAin't that sweet?";
	}
	
}
