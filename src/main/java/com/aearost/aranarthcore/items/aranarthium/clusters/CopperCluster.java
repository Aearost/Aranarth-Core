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
 * Provides the necessary components of a Copper Cluster item.
 */
public class CopperCluster implements AranarthItem {

	/**
	 * @return The Copper Cluster.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.BLAZE_POWDER, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			meta.getPersistentDataContainer().set(CLUSTER, PersistentDataType.STRING, "copper");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return ChatUtils.translateToColor("#b87333&lCopper Cluster");
	}
}
