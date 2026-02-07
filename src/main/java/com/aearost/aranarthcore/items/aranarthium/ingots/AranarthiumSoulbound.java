package com.aearost.aranarthcore.items.aranarthium.ingots;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.ARANARTHIUM_INGOT;

/**
 * Provides the necessary components of an Soulbound Aranarthium Ingot item.
 */
public class AranarthiumSoulbound implements AranarthItem {

	/**
	 * @return The Soulbound Aranarthium Ingot.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.ECHO_SHARD, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "aranarthium_soulbound");
			meta.setItemModel(key);
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(ARANARTHIUM_INGOT, PersistentDataType.STRING, "soulbound");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return ChatUtils.translateToColor("#9400D3&lSoulbound Aranarthium");
	}
	
	public String getLore() {
		return "&7&oBound for life...";
	}
	
}
