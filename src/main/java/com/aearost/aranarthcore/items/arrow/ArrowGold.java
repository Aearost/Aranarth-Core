package com.aearost.aranarthcore.items.arrow;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARROW;


public class ArrowGold implements AranarthItem {

	/**
	 * @return The Gold Arrow.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.ARROW, 1);
		ItemMeta meta = item.getItemMeta();
		if (Objects.nonNull(meta)) {
			ArrayList<String> lore = new ArrayList<>();
			CustomModelDataComponent cmdc = meta.getCustomModelDataComponent();
			cmdc.setFloats(List.of((float) 2));
			meta.setCustomModelDataComponent(cmdc);

			meta.getPersistentDataContainer().set(ARROW, PersistentDataType.STRING, "gold");
			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&6Gold Arrow";
	}
	
}
