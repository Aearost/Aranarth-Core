package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.MusicInstrument;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Increases damage taken when the player has blown the Seek horn.
 */
public class HornSeekExtraDamage {
	public void execute(EntityDamageEvent e) {
		Entity entity = e.getEntity();
		if (entity instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			Long lastUsed = aranarthPlayer.getHorns().get(MusicInstrument.SEEK_GOAT_HORN);
			// If the effect of the horn is still active, increase the damage taken by 1.5x
			if (lastUsed + 30000 > System.currentTimeMillis()) {
				double finalDamage = e.getFinalDamage();
				double extra = finalDamage * 0.5;
				e.setDamage(e.getDamage() + extra);
			}
		}
	}
}
