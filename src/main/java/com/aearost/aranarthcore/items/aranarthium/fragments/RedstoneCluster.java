package com.aearost.aranarthcore.items.aranarthium.fragments;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Provides the necessary components of an Redstone Cluster item.
 */
public class RedstoneCluster {

	/**
	 * @return The Redstone Cluster.
	 */
	public static ItemStack getRedstoneCluster() {
		ItemStack redstoneCluster = new ItemStack(Material.FERMENTED_SPIDER_EYE, 1);
		ItemMeta meta = redstoneCluster.getItemMeta();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			redstoneCluster.setItemMeta(meta);
		}
	    return redstoneCluster;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#aa0000&lRedstone Cluster");
	}
}
