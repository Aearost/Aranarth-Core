package com.aearost.aranarthcore.items.aranarthium;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Provides the necessary components of an Copper Fragment item.
 */
public class CopperFragment {

	/**
	 * @return The Copper Fragment.
	 */
	public static ItemStack getCopperFragment() {
		ItemStack copperFragment = new ItemStack(Material.BLAZE_POWDER, 1);
		ItemMeta meta = copperFragment.getItemMeta();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			copperFragment.setItemMeta(meta);
		}
	    return copperFragment;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#b87333Copper Fragment");
	}
}
