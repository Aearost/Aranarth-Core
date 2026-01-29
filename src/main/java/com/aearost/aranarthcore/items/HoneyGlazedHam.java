package com.aearost.aranarthcore.items;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomItemKeys.HONEY_GLAZED_HAM;

/**
 * Provides the necessary components of a Honey Glazed Ham item.
 */
public class HoneyGlazedHam implements AranarthItem {

	/**
	 * @return The Honey Glazed Ham.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.COOKED_PORKCHOP, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
//			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "honey_glazed_ham");
//			meta.setItemModel(key);
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(HONEY_GLAZED_HAM, PersistentDataType.STRING, "honey_glazed_ham");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&6Honey Glazed Ham";
	}
	
	public String getLore() {
		return "&eAin't that sweet?";
	}
	
}
