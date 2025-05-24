package com.aearost.aranarthcore.items.aranarthium;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Provides the necessary components of an Diamond Fragment item.
 */
public class DiamondFragment {

	/**
	 * @return The Diamond Fragment.
	 */
	public static ItemStack getDiamondFragment() {
		ItemStack diamondFragment = new ItemStack(Material.PRISMARINE_CRYSTALS, 1);
		ItemMeta meta = diamondFragment.getItemMeta();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			diamondFragment.setItemMeta(meta);
		}
	    return diamondFragment;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#a0f0ed&lDiamond Fragment");
	}
}
