package com.aearost.aranarthcore.items.arrowhead;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.ARROW_HEAD;


public class ArrowheadDragon implements AranarthItem {

	/**
	 * @return The Dragon's Breath Arrowhead.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.AMETHYST_CLUSTER, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "arrowhead_dragon");
			meta.setItemModel(key);
			meta.getPersistentDataContainer().set(ARROW_HEAD, PersistentDataType.STRING, "dragon");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			meta.setLore(List.of(
					ChatUtils.translateToColor("&7&oCan spawn dragon's breath cloud"),
					ChatUtils.translateToColor("&7&o60% chance to shatter if missed")));
			item.setItemMeta(meta);
		}
	    return item;
	}

	public String getName() {
		return "&5Dragon's Breath Arrowhead";
	}
	
}
