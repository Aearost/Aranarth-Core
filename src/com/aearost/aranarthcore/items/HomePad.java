package com.aearost.aranarthcore.items;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.aearost.aranarthcore.utils.ChatUtils;

public class HomePad {

	/**
	 * Returns an ItemStack of a single Home Pad
	 * 
	 * @return
	 */
	public static ItemStack getHomePad() {
		ItemStack homePad = new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, 1);
		ItemMeta meta = homePad.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		
		meta.setDisplayName(ChatUtils.translateToColor(getName()));
	    lore.add(ChatUtils.translateToColor(getLore()));
	    meta.setLore(lore);
	    homePad.setItemMeta(meta);
	    
	    return homePad;
	}
	
	public static String getName() {
		return "&6&lHome Pad";
	}
	
	public static String getLore() {
		return "&ePlace this to set up a home pad!";
	}
	
}
