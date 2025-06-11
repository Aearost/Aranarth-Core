package com.aearost.aranarthcore.items;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.items.CustomItemKeys.CHORUS_DIAMOND;

/**
 * Provides the necessary components of a Chorus Diamond item.
 */
public class ChorusDiamond implements AranarthItem {

	/**
	 * @return The Chorus Diamond.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.DIAMOND, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(CHORUS_DIAMOND, PersistentDataType.STRING, "chorus_diamond");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&5&lChorus Diamond";
	}
	
	public String getLore() {
		return "&bYou can't eat this one...";
	}
	
}
