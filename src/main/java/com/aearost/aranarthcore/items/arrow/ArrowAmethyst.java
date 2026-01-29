package com.aearost.aranarthcore.items.arrow;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomItemKeys.ARROW;


public class ArrowAmethyst implements AranarthItem {

	/**
	 * @return The Amethyst Arrow.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.ARROW, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "arrow_amethyst");
			meta.setItemModel(key);
			meta.getPersistentDataContainer().set(ARROW, PersistentDataType.STRING, "amethyst");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "#A020F0Amethyst Arrow";
	}
	
}
