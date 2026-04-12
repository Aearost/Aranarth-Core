package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityCombustEvent;

/**
 * Handles all logic regarding preventing fire spread and burning.
 */
public class FireProtectionListener implements Listener {

	public FireProtectionListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents scorched armour wearers from ever having fire ticks applied.
	 */
	@EventHandler
	public void onEntityCombust(EntityCombustEvent e) {
		if (e.getEntity() instanceof Player player && AranarthUtils.isWearingArmorType(player, "scorched")) {
			e.setCancelled(true);
			player.setFireTicks(0);
		}
	}

	/**
	 * Prevents fire from spreading.
	 */
	@EventHandler
	public void onFireSpread(BlockSpreadEvent e) {
		String worldName = e.getBlock().getWorld().getName();
		if (e.getSource().getType() == Material.FIRE) {
			if (worldName.startsWith("world") || worldName.startsWith("smp") || worldName.startsWith("resource")) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents fire from burning blocks in Dominions.
	 */
	@EventHandler
	public void onFireBurn(BlockBurnEvent e) {
		String worldName = e.getBlock().getWorld().getName();
		if (worldName.startsWith("world") || worldName.startsWith("smp") || worldName.startsWith("resource")) {
			e.setCancelled(true);
		}
	}

}
