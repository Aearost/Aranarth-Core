package com.aearost.aranarthcore.items.aranarthium.fragments;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Provides the necessary components of an Lapis Cluster item.
 */
public class LapisCluster {

	/**
	 * @return The Lapis Cluster.
	 */
	public static ItemStack getLapisCluster() {
		ItemStack lapisCluster = new ItemStack(Material.BLUE_DYE, 1);
		ItemMeta meta = lapisCluster.getItemMeta();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lapisCluster.setItemMeta(meta);
		}
	    return lapisCluster;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#4169e1&lLapis Cluster");
	}
}
