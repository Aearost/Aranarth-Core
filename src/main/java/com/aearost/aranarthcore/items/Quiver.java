package com.aearost.aranarthcore.items;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomItemKeys.QUIVER;

/**
 * Provides the necessary components of a Quiver item.
 */
public class Quiver implements AranarthItem {

	/**
	 * @return The Quiver.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.LEATHER, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "quiver");
			meta.setItemModel(key);
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(QUIVER, PersistentDataType.STRING, "quiver");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			meta.setMaxStackSize(1);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&6&lQuiver";
	}
	
	public String getLore() {
		return "&eStore your arrows!";
	}
	
}
