package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * Prevents players from picking up blacklisted items.
 */
public class BlacklistItemPickupPrevent {
	public void execute(EntityPickupItemEvent e) {
		Player player = (Player) e.getEntity();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (Objects.isNull(aranarthPlayer.getBlacklist())) {
			return;
		}
		if (!aranarthPlayer.getBlacklist().isEmpty()) {
			List<ItemStack> blacklistedItems = aranarthPlayer.getBlacklist();
			for (ItemStack is : blacklistedItems) {
				if (is.isSimilar(e.getItem().getItemStack())) {
					e.setCancelled(true);
					if (aranarthPlayer.getIsDeletingBlacklistedItems()) {
						// Trash the items
						e.getItem().remove();
					}
				}
			}
		}
	}
}
