package com.aearost.aranarthcore.event.mob;

import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

/**
 * Prevents parrots from being removed when jumping unless the player is sneaking.
 */
public class ParrotJumpCancelDismount {
	public void execute(CreatureSpawnEvent e) {
		if (e.getSpawnReason() == SpawnReason.SHOULDER_ENTITY) {
			Parrot parrot = (Parrot) e.getEntity();
			if (parrot.getOwner() instanceof Player player) {
				// Only prevent removal if the player is sneaking
				if (!player.isSneaking()) {
					e.setCancelled(true);
				}
			}
		}
	}

}
