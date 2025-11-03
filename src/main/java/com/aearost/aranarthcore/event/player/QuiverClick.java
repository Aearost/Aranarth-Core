package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiQuiver;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Sound;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.items.CustomItemKeys.QUIVER;

/**
 * Handles opening the quiver inventory.
 */
public class QuiverClick {
	public void execute(PlayerInteractEvent e) {
		if (e.getItem().hasItemMeta()) {
			ItemMeta meta = e.getItem().getItemMeta();
			if (meta.getPersistentDataContainer().has(QUIVER)) {
				if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
					if (e.getPlayer().getWorld().getName().startsWith("world") || e.getPlayer().getWorld().getName().startsWith("smp")) {
						e.setCancelled(true);
						e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 1F, 0.8F);
						GuiQuiver gui = new GuiQuiver(e.getPlayer());
						gui.openGui();
					} else {
						e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou must be in Survival to open the quiver!"));
					}
				}
			}
		}
	}
}
