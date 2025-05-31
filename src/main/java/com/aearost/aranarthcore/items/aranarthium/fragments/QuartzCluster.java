package com.aearost.aranarthcore.items.aranarthium.fragments;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Provides the necessary components of an Quartz Cluster item.
 */
public class QuartzCluster {

	/**
	 * @return The Quartz Cluster.
	 */
	public static ItemStack getQuartzCluster() {
		ItemStack quartzCluster = new ItemStack(Material.PHANTOM_MEMBRANE, 1);
		ItemMeta meta = quartzCluster.getItemMeta();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			quartzCluster.setItemMeta(meta);
		}
	    return quartzCluster;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#f8e8e8&lQuartz Cluster");
	}
}
