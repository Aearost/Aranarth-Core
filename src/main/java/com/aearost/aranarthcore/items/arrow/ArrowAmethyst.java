package com.aearost.aranarthcore.items.arrow;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARROW;


public class ArrowAmethyst implements AranarthItem {

	/**
	 * @return The Amethyst Arrow.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.ARROW, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(ARROW, PersistentDataType.STRING, "amethyst");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "#A020F0Amethyst Arrow";
	}
	
}
