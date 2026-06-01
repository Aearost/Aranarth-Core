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


public class ArrowheadLightning implements AranarthItem {

	/**
	 * @return The Lightning Arrowhead.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.GLOWSTONE_DUST, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "arrowhead_lightning");
			meta.setItemModel(key);
			meta.getPersistentDataContainer().set(ARROW_HEAD, PersistentDataType.STRING, "lightning");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			meta.setLore(List.of(
				ChatUtils.translateToColor("&7&o25% chance to strike lightning in rain"),
				ChatUtils.translateToColor("&7&oAlways strikes lightning in a thunderstorm")));
			item.setItemMeta(meta);
		}
	    return item;
	}

	public String getName() {
		return "&eLightning Arrowhead";
	}
	
}
