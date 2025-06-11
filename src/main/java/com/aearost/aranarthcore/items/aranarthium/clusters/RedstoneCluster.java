package com.aearost.aranarthcore.items.aranarthium.clusters;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

import static com.aearost.aranarthcore.items.CustomItemKeys.CLUSTER;

/**
 * Provides the necessary components of an Redstone Cluster item.
 */
public class RedstoneCluster implements AranarthItem {

	/**
	 * @return The Redstone Cluster.
	 */
	public ItemStack getItem() {
		ItemStack redstoneCluster = new ItemStack(Material.FERMENTED_SPIDER_EYE, 1);
		ItemMeta meta = redstoneCluster.getItemMeta();
		if (Objects.nonNull(meta)) {
			meta.getPersistentDataContainer().set(CLUSTER, PersistentDataType.STRING, "redstone");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			redstoneCluster.setItemMeta(meta);
		}
	    return redstoneCluster;
	}
	
	public String getName() {
		return ChatUtils.translateToColor("#aa0000&lRedstone Cluster");
	}
}
