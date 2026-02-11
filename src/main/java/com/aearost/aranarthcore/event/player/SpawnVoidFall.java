package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles teleporting a player back to spawn when they fall into the void of the spawn world.
 */
public class SpawnVoidFall {

	public void execute(PlayerMoveEvent e) {
		// If they did not move to a different coordinate and only their mouse
		if (e.getTo() == null) {
			return;
		}

		if (e.getTo().getY() <= 50) {
			Location spawn = new Location(Bukkit.getWorld("spawn"), 0, 100, 0, 0, 0);
			Location locToTeleportTo = AranarthUtils.getSafeTeleportLocation(spawn);
			e.getPlayer().teleport(locToTeleportTo);
			e.getPlayer().playSound(e.getPlayer(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
		}
	}
}
