package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Prevents players from picking up blacklisted items.
 */
public class BlacklistItemPickupPrevent {
	public void execute(EntityPickupItemEvent e) {
		Player player = (Player) e.getEntity();
		if (player.hasPermission("aranarth.blacklist")) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (Objects.isNull(aranarthPlayer.getBlacklist())) {
				return;
			}

			if (!aranarthPlayer.getBlacklist().isEmpty()) {
				int result = AranarthUtils.getBlacklistMethod(player, aranarthPlayer, e.getItem().getItemStack());
				if (result == 1) {
					e.getItem().remove();
					e.getItem().setItemStack(null);
					// 0.5 to 0.75
					float pitch = ThreadLocalRandom.current().nextFloat(0.5F, 0.75F);
					player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.2F, pitch);
				} else if (result == 0) {
					e.setCancelled(true);
				}
			}
		}
	}
}
