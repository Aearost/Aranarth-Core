package com.aearost.aranarthcore.items.aranarthium.ingots;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Provides the necessary components of an Enflamed Aranarthium Ingot item.
 */
public class EnflamedAranarthium {

	/**
	 * @return The Enflamed Aranarthium Ingot.
	 */
	public static ItemStack getEnflamedAranarthiumIngot() {
		ItemStack enflamedAranarthiumIngot = new ItemStack(Material.ECHO_SHARD, 1);
		ItemMeta meta = enflamedAranarthiumIngot.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();

		if (Objects.nonNull(meta)) {
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			enflamedAranarthiumIngot.setItemMeta(meta);
		}
	    return enflamedAranarthiumIngot;
	}
	
	public static String getName() {
		return ChatUtils.translateToColor("#ff4500&lEnflamed Aranarthium");
	}
	
	public static String getLore() {
		return "&7&oThe kiss of fire...";
	}
	
}
