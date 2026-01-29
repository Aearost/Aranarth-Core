package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.objects.CustomItemKeys.SUGARCANE_BLOCK;

/**
 * Prevents the placement of Sugarcane Blocks.
 */
public class SugarcaneBlockPlace {
	
	public void execute(BlockPlaceEvent e) {
		if (e.getItemInHand().hasItemMeta()) {
			ItemMeta meta = e.getItemInHand().getItemMeta();
			if (meta.getPersistentDataContainer().has(SUGARCANE_BLOCK)) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot place a Block of Sugarcane!"));
			}
		}
	}

}
