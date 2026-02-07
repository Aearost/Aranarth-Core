package com.aearost.aranarthcore.items.aranarthium.armour;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.ARMOR_TYPE;

/**
 * Provides the necessary components of a Soulbound Aranarthium Boots item.
 */
public class SoulboundAranarthiumBoots implements AranarthItem {

	/**
	 * @return The Soulbound Aranarthium Boots.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.NETHERITE_BOOTS, 1);
		ArmorMeta meta = (ArmorMeta) item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "aranarthium_soulbound_boots");
			meta.setItemModel(key);
			meta.setTrim(new ArmorTrim(TrimMaterial.AMETHYST, TrimPattern.RAISER));
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(ARMOR_TYPE, PersistentDataType.STRING, "soulbound");

			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return ChatUtils.translateToColor("#9400D3&lSoulbound Aranarthium Boots");
	}
	
	public String getLore() {
		return "&7&oBound for life...";
	}
	
}
