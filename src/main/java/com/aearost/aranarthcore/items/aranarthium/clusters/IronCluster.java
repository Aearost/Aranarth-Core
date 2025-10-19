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

import static com.aearost.aranarthcore.items.CustomItemKeys.CLUSTER;

/**
 * Provides the necessary components of an Iron Cluster item.
 */
public class IronCluster implements AranarthItem {

	/**
	 * @return The Iron Cluster.
	 */
	public ItemStack getItem() {
		ItemStack ironFragment = new ItemStack(Material.IRON_NUGGET, 1);
		ItemMeta meta = ironFragment.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "cluster_iron");
			meta.setItemModel(key);
			meta.getPersistentDataContainer().set(CLUSTER, PersistentDataType.STRING, "iron");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			ironFragment.setItemMeta(meta);
		}
	    return ironFragment;
	}
	
	public String getName() {
		return ChatUtils.translateToColor("#eeeeee&lIron Cluster");
	}
}
