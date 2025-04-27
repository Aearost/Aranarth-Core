package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiQuiver;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Objects;

public class QuiverClick implements Listener {

	public QuiverClick(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles opening the quiver inventory.
	 * @param e The event.
	 */
	@EventHandler
	public void onQuiverClick(final PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (Objects.nonNull(e.getItem())) {
				if (e.getItem().getType() == Material.LIGHT_GRAY_BUNDLE) {
					if (Objects.nonNull(e.getItem().getItemMeta()) && e.getItem().getItemMeta().hasLore()) {
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
		}
	}
}
