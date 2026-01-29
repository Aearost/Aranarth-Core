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

import static com.aearost.aranarthcore.objects.CustomItemKeys.ARROW_HEAD;


public class ArrowheadGold implements AranarthItem {

	/**
	 * @return The Gold Arrowhead.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.GLOWSTONE_DUST, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "arrowhead_gold");
			meta.setItemModel(key);
			meta.getPersistentDataContainer().set(ARROW_HEAD, PersistentDataType.STRING, "gold");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&6Gold Arrowhead";
	}
	
}
