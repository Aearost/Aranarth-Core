package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;

public class HomePadStep implements Listener {

	public HomePadStep(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * If the player steps on a pressure plate that is a home pad, prompt them with
	 * the GUI to allow them to accept or decline.
	 * @param e The event.
	 */
	@EventHandler
	public void onHomepadStep(final PlayerMoveEvent e) {
		Player player = e.getPlayer();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		boolean isStandingOnHomePad = aranarthPlayer.getIsStandingOnHomePad();

		// When they step on the homepad
		if (e.getTo().getBlock().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
			// If they are not on one of the pressure plates
			if (!isStandingOnHomePad) {
				// If the current location is a home pad
				if (Objects.nonNull(AranarthUtils.getHomePad(e.getTo()))) {
					if (!AranarthUtils.getHomePad(e.getTo()).getHomeName().equals("NEW")) {
						aranarthPlayer.setIsStandingOnHomePad(true);
						aranarthPlayer.setCurrentGuiPageNum(0);
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						GuiTeleport gui = new GuiTeleport(player);
						gui.openGui();
					}
				}
			}
		}
		// When they step off the homepad
		else if (e.getTo().getBlock().getType() != Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
			if (isStandingOnHomePad) {
				if (Objects.nonNull(AranarthUtils.getHomePad(e.getFrom()))) {
					aranarthPlayer.setIsStandingOnHomePad(false);
					aranarthPlayer.setCurrentGuiPageNum(0);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				}
			}
		}
	}

}
