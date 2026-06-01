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


public class ArrowheadSpectral implements AranarthItem {

	/**
	 * @return The Spectral Arrowhead.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.FLINT, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "arrowhead_spectral");
			meta.setItemModel(key);
			meta.getPersistentDataContainer().set(ARROW_HEAD, PersistentDataType.STRING, "spectral");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			meta.setLore(List.of(ChatUtils.translateToColor("&7&oUsed to craft spectral arrows")));
			item.setItemMeta(meta);
		}
	    return item;
	}

	public String getName() {
		return "&eSpectral Arrowhead";
	}
	
}
