package com.aearost.aranarthcore.utils;

import java.util.Comparator;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

public class ComparatorArrowEffect implements Comparator<ItemStack> {

	@Override
	public int compare(ItemStack arrow1, ItemStack arrow2) {
		
		if (arrow1.hasItemMeta()) {
			if (arrow2.hasItemMeta()) {
				PotionMeta arrow1Meta = (PotionMeta) arrow1.getItemMeta();
				PotionMeta arrow2Meta = (PotionMeta) arrow2.getItemMeta();
				
				return arrow1Meta.getBasePotionType().name().compareToIgnoreCase(
						arrow2Meta.getBasePotionType().name());
			}
		}
		return arrow1.getType().name().compareToIgnoreCase(arrow2.getType().name());
	}

}
