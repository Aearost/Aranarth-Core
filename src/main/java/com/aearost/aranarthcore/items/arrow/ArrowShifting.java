package com.aearost.aranarthcore.items.arrow;

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

import static com.aearost.aranarthcore.objects.CustomKeys.ARROW;


public class ArrowShifting implements AranarthItem {

	/**
	 * @return The Shifting Arrow.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.ARROW, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "arrow_shifting");
			meta.setItemModel(key);
			meta.getPersistentDataContainer().set(ARROW, PersistentDataType.STRING, "shifting");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			meta.setLore(List.of(ChatUtils.translateToColor("&7&oSwaps places with the target hit")));
			item.setItemMeta(meta);
		}
	    return item;
	}

	public String getName() {
		return "&#2fa08aShifting Arrow";
	}
	
}
