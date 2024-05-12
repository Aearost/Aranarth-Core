package com.aearost.aranarthcore.items;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.aearost.aranarthcore.utils.ChatUtils;

public class Quiver {

	/**
	 * Returns an ItemStack of a single Quiver
	 * 
	 * @return
	 */
	public static ItemStack getQuiver() {
		ItemStack quiver = new ItemStack(Material.BUNDLE, 1);
		ItemMeta meta = quiver.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		
		meta.setDisplayName(ChatUtils.translateToColor(getName()));
		lore.add(ChatUtils.translateToColor(getLore()));
	    meta.setLore(lore);
	    quiver.setItemMeta(meta);
	    
	    return quiver;
	}
	
	public static String getName() {
		return "&6&lQuiver";
	}
	
	public static String getLore() {
		return "&eNot a bundle!";
	}
	
}
