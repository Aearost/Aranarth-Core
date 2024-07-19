package com.aearost.aranarthcore.items;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.aearost.aranarthcore.utils.ChatUtils;

public class BewitchedMinecart {

	/**
	 * Returns an ItemStack of a single Honey Glazed Ham
	 * 
	 * @return
	 */
	public static ItemStack getBewitchedMinecart() {
		ItemStack bewitchedMinecart = new ItemStack(Material.MINECART, 1);
		ItemMeta meta = bewitchedMinecart.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		
		meta.setDisplayName(ChatUtils.translateToColor(getName()));
		lore.add(ChatUtils.translateToColor(getLore()));
	    meta.setLore(lore);
	    bewitchedMinecart.setItemMeta(meta);
	    
	    return bewitchedMinecart;
	}
	
	public static String getName() {
		return "&5&lBewitched Minecart";
	}
	
	public static String getLore() {
		return "&dZoom zoom zoom!";
	}
	
}
