package com.aearost.aranarthcore.items.arrowhead;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARROW_HEAD;


public class ArrowheadExplosive implements AranarthItem {

	/**
	 * @return The Explosive Arrowhead.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.GUNPOWDER, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "arrowhead_explosive");
			meta.setItemModel(key);
			meta.getPersistentDataContainer().set(ARROW_HEAD, PersistentDataType.STRING, "explosive");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&8Explosive Arrowhead";
	}
	
}
