package com.aearost.aranarthcore.items;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.GOD_APPLE_FRAGMENT;

/**
 * Provides the necessary components of a God Apple Fragment item.
 */
public class GodAppleFragment implements AranarthItem {

	/**
	 * @return The God Apple Fragment.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.GOLD_NUGGET, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
//			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "god_apple_fragment");
//			meta.setItemModel(key);
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(GOD_APPLE_FRAGMENT, PersistentDataType.STRING, "god_apple_fragment");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&6God Apple Fragment";
	}
	
	public String getLore() {
		return "&eA gift from the gods...";
	}
	
}
