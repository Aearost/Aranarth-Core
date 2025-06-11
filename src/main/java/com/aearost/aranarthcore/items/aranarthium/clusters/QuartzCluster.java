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
 * Provides the necessary components of an Quartz Cluster item.
 */
public class QuartzCluster implements AranarthItem {

	/**
	 * @return The Quartz Cluster.
	 */
	public ItemStack getItem() {
		ItemStack quartzCluster = new ItemStack(Material.PHANTOM_MEMBRANE, 1);
		ItemMeta meta = quartzCluster.getItemMeta();
		if (Objects.nonNull(meta)) {
			meta.getPersistentDataContainer().set(CLUSTER, PersistentDataType.STRING, "quartz");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			quartzCluster.setItemMeta(meta);
		}
	    return quartzCluster;
	}
	
	public String getName() {
		return ChatUtils.translateToColor("#f8e8e8&lQuartz Cluster");
	}
}
