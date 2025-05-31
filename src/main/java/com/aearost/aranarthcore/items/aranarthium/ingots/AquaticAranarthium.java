package com.aearost.aranarthcore.items.aranarthium.ingots;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of an Aquatic Aranarthium Ingot item.
 */
public class AquaticAranarthium {

	/**
	 * @return The Aquatic Aranarthium Ingot.
	 */
	public static ItemStack getAquaticAranarthiumIngot() {
		ItemStack aquaticAranarthiumIngot = new ItemStack(Material.ECHO_SHARD, 1);
		ItemMeta meta = aquaticAranarthiumIngot.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			aquaticAranarthiumIngot.setItemMeta(meta);
		}
	    return aquaticAranarthiumIngot;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#AEEEEE&lAquatic Aranarthium");
	}
	
	public static String getLore() {
		return "&7&oThe touch of water...";
	}
	
}
