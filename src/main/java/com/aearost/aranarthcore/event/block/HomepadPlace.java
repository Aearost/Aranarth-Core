package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.objects.CustomKeys.HOMEPAD;

/**
 * Adds a new entry to the homes HashMap when a Homepad is placed.
 */
public class HomepadPlace {

	public void execute(BlockPlaceEvent e) {
		if (e.getItemInHand().hasItemMeta()) {
			ItemMeta meta = e.getItemInHand().getItemMeta();
			if (meta.getPersistentDataContainer().has(HOMEPAD)) {
				if (!AranarthCore.isSmpServer()) {
					e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou can only place this in the SMP!"));
					e.setCancelled(true);
					return;
				}
				Location location = e.getBlockPlaced().getLocation();
				AranarthUtils.addNewHomepad(location);
			}
		}
	}
}
