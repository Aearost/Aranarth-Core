package com.aearost.aranarthcore.utils;

import java.util.Comparator;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

public class PotionEffectComparable implements Comparator<ItemStack> {

	@Override
	public int compare(ItemStack potion1, ItemStack potion2) {
		PotionMeta potion1Meta = (PotionMeta) potion1.getItemMeta();
		PotionMeta potion2Meta = (PotionMeta) potion2.getItemMeta();
		
		return potion1Meta.getBasePotionType().name().compareTo(potion2Meta.getBasePotionType().name());
	}

}
