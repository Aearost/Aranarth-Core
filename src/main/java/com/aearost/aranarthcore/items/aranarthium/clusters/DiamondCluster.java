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

import static com.aearost.aranarthcore.objects.CustomItemKeys.CLUSTER;

/**
 * Provides the necessary components of a Diamond Cluster item.
 */
public class DiamondCluster implements AranarthItem {

	/**
	 * @return The Diamond Cluster.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.PRISMARINE_CRYSTALS, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "cluster_diamond");
			meta.setItemModel(key);
			meta.getPersistentDataContainer().set(CLUSTER, PersistentDataType.STRING, "diamond");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return ChatUtils.translateToColor("#a0f0ed&lDiamond Cluster");
	}
}
