package com.aearost.aranarthcore.items.key;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomItemKeys.CRATE_KEY;

public class KeyRare implements AranarthItem {

	/**
	 * @return The Rare Crate Key.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.TRIAL_KEY, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "key_rare");
			meta.setItemModel(key);
			meta.getPersistentDataContainer().set(CRATE_KEY, PersistentDataType.STRING, "key_rare");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&6Rare Crate Key";
	}
	
}
