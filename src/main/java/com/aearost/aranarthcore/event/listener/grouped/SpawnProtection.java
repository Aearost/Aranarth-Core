package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Prevents crops from being trampled by both players and other mobs
 */
public class SpawnProtection implements Listener {

	public SpawnProtection(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from placing blocks at spawn.
	 */
	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getBlock().getLocation())) {
			if (!e.getPlayer().hasPermission("aranarth.protect.bypass")) {
				if (e.getPlayer().getWorld().getName().equals("world")) {
					e.setCancelled(true);
					e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot do this at Spawn!"));
				}
			}
		}
	}

	/**
	 * Prevents players from breaking blocks at spawn.
	 */
	@EventHandler
	public void onBreak(BlockBreakEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getBlock().getLocation())) {
			if (!e.getPlayer().hasPermission("aranarth.protect.bypass")) {
				if (e.getPlayer().getWorld().getName().equals("world")) {
					e.setCancelled(true);
					e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot do this at Spawn!"));
				}
			}
		}
	}

	/**
	 * Prevents players from interacting with non-alive entities at spawn.
	 */
	@EventHandler
	public void onInteractEntity(PlayerInteractEntityEvent e) {
		if (e.getRightClicked() != null) {
			if (AranarthUtils.isSpawnLocation(e.getRightClicked().getLocation())) {
				if (!e.getPlayer().hasPermission("aranarth.protect.bypass")) {
					if (e.getPlayer().getWorld().getName().equals("world")) {
						e.setCancelled(true);
						e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot do this at Spawn!"));
					}
				}
			}
		}
	}

	/**
	 * Prevents players from placing entities at spawn.
	 */
	@EventHandler
	public void onPlaceEntity(EntityPlaceEvent e) {
		if (e.getEntity() != null) {
			if (AranarthUtils.isSpawnLocation(e.getEntity().getLocation())) {
				if (!e.getPlayer().hasPermission("aranarth.protect.bypass")) {
					if (e.getPlayer().getWorld().getName().equals("world")) {
						e.setCancelled(true);
						e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot do this at Spawn!"));
					}
				}
			}
		}
	}

	/**
	 * Prevents players from attacking non-alive entities at spawn.
	 */
	@EventHandler
	public void onAttackEntity(EntityDamageEvent e) {
		if (e.getEntity() != null) {
			if (AranarthUtils.isSpawnLocation(e.getEntity().getLocation())) {
				if (e.getDamageSource().getCausingEntity() instanceof Player player) {
					if (!player.hasPermission("aranarth.protect.bypass")) {
						if (player.getWorld().getName().equals("world")) {
							e.setCancelled(true);
							player.sendMessage(ChatUtils.chatMessage("&cYou cannot do this at Spawn!"));
						}
					}
				}
			}
		}
	}

	/**
	 * Prevents fire from spreading at spawn.
	 */
	@EventHandler
	public void onFireSpread(BlockSpreadEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getBlock().getLocation())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Prevents dynamic fire from spreading at spawn and prevents lightning fire from spawning.
	 */
	@EventHandler
	public void onLightningStrike(BlockIgniteEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getBlock().getLocation())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Prevents mobs from spawning at spawn.
	 */
	@EventHandler
	public void onMobSpawn(EntitySpawnEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getLocation())) {
			e.setCancelled(true);
		}
	}
}
