package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.Objects;

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
				int result = AranarthUtils.isBlacklistingItem(player, aranarthPlayer, e.getItem().getItemStack());
				if (result == 0) {
					e.getItem().remove();
					e.getItem().setItemStack(null);
				} else if (result == 1) {
					e.setCancelled(true);
				}
			}
		}
	}
}
