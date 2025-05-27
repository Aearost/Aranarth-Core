package com.aearost.aranarthcore.items.aranarthium;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Provides the necessary components of an Iron Fragment item.
 */
public class IronFragment {

	/**
	 * @return The Iron Fragment.
	 */
	public static ItemStack getIronFragment() {
		ItemStack ironFragment = new ItemStack(Material.IRON_NUGGET, 1);
		ItemMeta meta = ironFragment.getItemMeta();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			ironFragment.setItemMeta(meta);
		}
	    return ironFragment;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#eeeeee&lIron Fragment");
	}
}
