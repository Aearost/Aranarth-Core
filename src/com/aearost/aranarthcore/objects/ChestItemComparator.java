package com.aearost.aranarthcore.objects;

import java.util.Comparator;

import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.utils.ChatUtils;

public class ChestItemComparator implements Comparator<ItemStack> {

	@Override
	public int compare(ItemStack a, ItemStack b) {
		
		String aName = ChatUtils.getFormattedItemName(a.getType().name()).toLowerCase();
		String bName = ChatUtils.getFormattedItemName(b.getType().name()).toLowerCase();
		int x = aName.compareTo(bName);
		
		return x;
	}

}
