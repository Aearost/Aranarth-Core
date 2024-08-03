package com.aearost.aranarthcore.event;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;

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
			if (AranarthUtils.hasBlacklistedItems(player.getUniqueId())) {
				List<ItemStack> blacklistedItems = AranarthUtils.getBlacklistedItems(player.getUniqueId());
				for (ItemStack is : blacklistedItems) {
					if (is.isSimilar(e.getItem().getItemStack())) {
						e.setCancelled(true);
						if (AranarthUtils.getPlayer(player.getUniqueId()).getIsDeletingBlacklistedItems()) {
							// Trash the items
							e.getItem().remove();
						}
					}
				}
			}
		}
	}
}
