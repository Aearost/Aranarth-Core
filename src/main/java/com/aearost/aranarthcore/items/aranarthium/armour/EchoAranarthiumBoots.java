package com.aearost.aranarthcore.items.aranarthium.armour;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
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
 * Provides the necessary components of an Echo Aranarthium Boots item.
 */
public class EchoAranarthiumBoots implements AranarthItem {

	/**
	 * @return The Echo Aranarthium Boots.
	 */
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.NETHERITE_BOOTS, 1);
		ArmorMeta meta = (ArmorMeta) item.getItemMeta();
		if (Objects.nonNull(meta)) {
			NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "aranarthium_echo_boots");
			meta.setItemModel(key);
			meta.setTrim(new ArmorTrim(TrimMaterial.LAPIS, TrimPattern.SILENCE));
			meta.addEnchant(Enchantment.MENDING, 1, true);
			ArrayList<String> lore = new ArrayList<>();
			meta.getPersistentDataContainer().set(ARMOR_TYPE, PersistentDataType.STRING, "echo");

			meta.setDisplayName(ChatUtils.translateToColor(getName()));
			lore.add(ChatUtils.translateToColor(getLore()));
			meta.setLore(lore);
			meta.setFireResistant(true);
			item.setItemMeta(meta);
		}
	    return item;
	}
	
	public String getName() {
		return "&#1F8FA6&lEcho Aranarthium Boots";
	}
	
	public String getLore() {
		return "&7&oSilence speaks volumes...";
	}
	
}
