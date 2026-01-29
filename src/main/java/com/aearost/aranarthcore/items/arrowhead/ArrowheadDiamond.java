package com.aearost.aranarthcore.items.arrowhead;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARROW_HEAD;


public class ArrowheadDiamond implements AranarthItem {

	/**
	 * @return The Diamond Arrowhead.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.PRISMARINE_SHARD, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(ARROW_HEAD, PersistentDataType.STRING, "diamond");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&bDiamond Arrowhead";
	}
	
}
