package com.aearost.aranarthcore.items.aranarthium;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Provides the necessary components of an Emerald Fragment item.
 */
public class EmeraldFragment {

	/**
	 * @return The Emerald Fragment.
	 */
	public static ItemStack getEmeraldFragment() {
		ItemStack emeraldFragment = new ItemStack(Material.TURTLE_SCUTE, 1);
		ItemMeta meta = emeraldFragment.getItemMeta();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			emeraldFragment.setItemMeta(meta);
		}
	    return emeraldFragment;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#50c878&lEmerald Fragment");
	}
}
