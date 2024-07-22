package com.aearost.aranarthcore.items;

import java.util.ArrayList;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.aearost.aranarthcore.utils.ChatUtils;

/**
 * Provides the necessary components of a Sugarcane Block item.
 */
public class SugarcaneBlock {

	/**
	 * @return The Sugarcane Block.
	 */
	public static ItemStack getSugarcaneBlock() {
		ItemStack sugarcaneBlock = new ItemStack(Material.BAMBOO_BLOCK, 1);
		ItemMeta meta = sugarcaneBlock.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			sugarcaneBlock.setItemMeta(meta);
		}
	    return sugarcaneBlock;
	}
	
	public static String getName() {
		return "&aBlock of Sugarcane";
	}
	
	public static String getLore() {
		return "&7An efficient way to be stored!";
	}
	
}
