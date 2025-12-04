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

import static com.aearost.aranarthcore.objects.CustomItemKeys.SUGARCANE_BLOCK;

/**
 * Provides the necessary components of a Sugarcane Block item.
 */
public class SugarcaneBlock implements AranarthItem {

	/**
	 * @return The Sugarcane Block.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.BAMBOO_BLOCK, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "sugarcane_block");
			meta.setItemModel(key);
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(SUGARCANE_BLOCK, PersistentDataType.STRING, "sugarcane_block");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&aBlock of Sugarcane";
	}
	
	public String getLore() {
		return "&7An efficient way to be stored!";
	}
	
}
