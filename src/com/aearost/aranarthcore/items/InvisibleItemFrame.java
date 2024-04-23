package com.aearost.aranarthcore.items;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import com.aearost.aranarthcore.utils.ChatUtils;

/**
 * Inspired by tiffany352
 * Source: https://github.com/tiffany352/InvisibleItemFrames/blob/main/src/main/java/com/tiffnix/invisibleitemframes/InvisibleItemFrames.java
 */
public class InvisibleItemFrame {

	public static Plugin PLUGIN = null;
	public static NamespacedKey IS_INVISIBLE;
	public static ItemStack ITEM_FRAME;
	
	public InvisibleItemFrame(Plugin plugin) {
		PLUGIN = plugin;
		IS_INVISIBLE = new NamespacedKey(plugin, "invisible");
		ITEM_FRAME = getInvisibleItemFrame();
	}
	
	/**
	 * Returns an ItemStack of a single Invisible Item Frame
	 * 
	 * @return
	 */
	public static ItemStack getInvisibleItemFrame() {
		ItemStack invisibleItemFrame = new ItemStack(Material.ITEM_FRAME, 1);
		ItemMeta meta = invisibleItemFrame.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		
		meta.setDisplayName(ChatUtils.translateToColor(getName()));
	    lore.add(ChatUtils.translateToColor(getLore()));
	    meta.setLore(lore);
	    invisibleItemFrame.setItemMeta(meta);
	    
	    return invisibleItemFrame;
	}
	
	public static boolean isInvisibleItemFrame(Entity entity) {
		if (entity != null) {
			if (entity.getType() == EntityType.ITEM_FRAME) {
				return entity.getPersistentDataContainer().has(IS_INVISIBLE, PersistentDataType.BYTE);
			}
		}
		return false;
	}
	
	public static String getName() {
		return "&6&lInvisible Item Frame";
	}
	
	public static String getLore() {
		return "&eYou can place it, but you won't see it!";
	}
	
}
