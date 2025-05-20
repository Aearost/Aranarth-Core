package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiQuiver;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handles opening the quiver inventory.
 */
public class QuiverClick {
	public void execute(PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (e.getPlayer().getWorld().getName().startsWith("world")) {
				e.setCancelled(true);
				GuiQuiver gui = new GuiQuiver(e.getPlayer());
				gui.openGui();
			} else {
				e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou must be in Survival mode to open the quiver!"));
			}
		}
	}
}
