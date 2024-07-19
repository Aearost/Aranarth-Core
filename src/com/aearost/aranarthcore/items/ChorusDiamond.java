package com.aearost.aranarthcore.items;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.aearost.aranarthcore.utils.ChatUtils;

public class ChorusDiamond {

	/**
	 * Returns an ItemStack of a single Chorus Diamond
	 * 
	 * @return
	 */
	public static ItemStack getChorusDiamond() {
		ItemStack chorusDiamond = new ItemStack(Material.DIAMOND, 1);
		ItemMeta meta = chorusDiamond.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		
		meta.setDisplayName(ChatUtils.translateToColor(getName()));
	    lore.add(ChatUtils.translateToColor(getLore()));
	    meta.setLore(lore);
	    chorusDiamond.setItemMeta(meta);
	    
	    return chorusDiamond;
	}
	
	public static String getName() {
		return "&5&lChorus Diamond";
	}
	
	public static String getLore() {
		return "&bYou can't eat this one...";
	}
	
}
