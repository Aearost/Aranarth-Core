package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public class BlacklistItemPickupPrevent implements Listener {

	public BlacklistItemPickupPrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from picking up blacklisted items.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerPickupItem(final EntityPickupItemEvent e) {
		if (e.getEntity() instanceof Player player) {
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
}
