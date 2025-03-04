package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class ParrotJumpCancelDismount implements Listener {

	public ParrotJumpCancelDismount(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents parrots from being removed when jumping unless the player is sneaking.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerJump(final CreatureSpawnEvent e) {
		if (e.getEntity() instanceof Parrot) {
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

}
