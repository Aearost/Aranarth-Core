package com.aearost.aranarthcore.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Provides additional perks when eating honey glazed ham.
 */
public class HoneyGlazedHamEat {
	public void execute(final PlayerItemConsumeEvent e) {
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
