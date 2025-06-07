package com.aearost.aranarthcore.items.aranarthium.clusters;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

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
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			ironFragment.setItemMeta(meta);
		}
	    return ironFragment;
	}
	
	public String getName() {
		return ChatUtils.translateToColor("#eeeeee&lIron Cluster");
	}
}
