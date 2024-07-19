package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.HoneyGlazedHam;

public class HoneyGlazedHamEat implements Listener {

	public HoneyGlazedHamEat(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Provides additional perks when eating honey glazed ham.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onHoneyGlazedHamEat(final PlayerItemConsumeEvent e) {
		if (e.getItem().isSimilar(HoneyGlazedHam.getHoneyGlazedHam())) {
			e.setCancelled(true);
			int newAmount = e.getItem().getAmount() - 1;
			ItemStack honeyGlazedHam = e.getItem();
			honeyGlazedHam.setAmount(newAmount);
			Player player = e.getPlayer();
			player.getInventory().setItem(player.getInventory().getHeldItemSlot(), honeyGlazedHam);
			int newHunger = player.getFoodLevel() + 10;
			float newSaturation = player.getSaturation() + 15.0F;
			if (newHunger > 20) {
				newHunger = 20;
			}
			if (newSaturation > 20.0F) {
				newSaturation = 20.0F;
			}
			player.setFoodLevel(newHunger);
			player.setSaturation(newSaturation);
		}
	}

}
