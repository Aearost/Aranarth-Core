package com.aearost.aranarthcore.items.aranarthium;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Provides the necessary components of an Gold Fragment item.
 */
public class GoldFragment {

	/**
	 * @return The Gold Fragment.
	 */
	public static ItemStack getGoldFragment() {
		ItemStack goldFragment = new ItemStack(Material.GOLD_NUGGET, 1);
		ItemMeta meta = goldFragment.getItemMeta();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			goldFragment.setItemMeta(meta);
		}
	    return goldFragment;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#fcd34d&lGold Fragment");
	}
}
