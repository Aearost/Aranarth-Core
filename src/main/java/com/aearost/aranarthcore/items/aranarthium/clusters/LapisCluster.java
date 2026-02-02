package com.aearost.aranarthcore.items.aranarthium.clusters;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.CLUSTER;

/**
 * Provides the necessary components of an Lapis Cluster item.
 */
public class LapisCluster implements AranarthItem {

	/**
	 * @return The Lapis Cluster.
	 */
	public ItemStack getItem() {
		ItemStack lapisCluster = new ItemStack(Material.BLUE_DYE, 1);
		ItemMeta meta = lapisCluster.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "cluster_lapis");
			meta.setItemModel(key);
			meta.getPersistentDataContainer().set(CLUSTER, PersistentDataType.STRING, "lapis");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lapisCluster.setItemMeta(meta);
		}
	    return lapisCluster;
	}
	
	public String getName() {
		return ChatUtils.translateToColor("#4169e1&lLapis Cluster");
	}
}
