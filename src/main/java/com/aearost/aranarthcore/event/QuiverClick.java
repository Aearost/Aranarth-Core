package com.aearost.aranarthcore.event;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiQuiver;
import com.aearost.aranarthcore.utils.ChatUtils;

public class QuiverClick implements Listener {

	public QuiverClick(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents stripping a log or wood block if the player is not sneaking.
	 * @param e The event.
	 */
	@EventHandler
	public void onQuiverClick(final PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (Objects.nonNull(e.getItem())) {
				if (e.getItem().getType() == Material.BUNDLE) {
					if (Objects.nonNull(e.getItem().getItemMeta()) && e.getItem().getItemMeta().hasLore()) {
						if (e.getPlayer().getGameMode() == GameMode.SURVIVAL) {
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
