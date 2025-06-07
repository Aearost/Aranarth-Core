package com.aearost.aranarthcore.items.aranarthium.clusters;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Provides the necessary components of an Emerald Cluster item.
 */
public class EmeraldCluster implements AranarthItem {

	/**
	 * @return The Emerald Cluster.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.TURTLE_SCUTE, 1);
		ItemMeta meta = item.getItemMeta();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return ChatUtils.translateToColor("#50c878&lEmerald Cluster");
	}
}
