package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerFishEventListener implements Listener {

	public PlayerFishEventListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Gives players wearing full Aquatic Aranarthium 1 extra fish.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerFish(final PlayerFishEvent e) {
		if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
			return;
		}
		Player player = e.getPlayer();
		if (!AranarthUtils.isWearingArmorType(player, "aquatic")) {
			return;
		}
		if (!(e.getCaught() instanceof Item caughtItem)) {
			return;
		}
		ItemStack fishItem = caughtItem.getItemStack();
		Material fishType = fishItem.getType();
		if (fishType == Material.COD || fishType == Material.SALMON
				|| fishType == Material.PUFFERFISH || fishType == Material.TROPICAL_FISH) {
			fishItem.setAmount(fishItem.getAmount() + 1);
			caughtItem.setItemStack(fishItem);
		}
	}
}
