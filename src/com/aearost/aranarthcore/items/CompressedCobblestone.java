package com.aearost.aranarthcore.items;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.aearost.aranarthcore.utils.ChatUtils;

public class CompressedCobblestone {

	/**
	 * Returns an ItemStack of a single Compressed Cobblestone
	 * 
	 * @return
	 */
	public static ItemStack getCompressedCobblestone() {
		ItemStack compressedCobblestone = new ItemStack(Material.COBBLESTONE, 1);
		ItemMeta meta = compressedCobblestone.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		
		meta.setDisplayName(ChatUtils.translateToColor(getName()));
	    lore.add(ChatUtils.translateToColor(getLore()));
	    meta.setLore(lore);
	    compressedCobblestone.setItemMeta(meta);
	    
	    return compressedCobblestone;
	}
	
	public static String getName() {
		return "&8&lCompressed Cobblestone";
	}
	
	public static String getLore() {
		return "&7Hard.......";
	}
	
}
