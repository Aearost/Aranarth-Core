package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Prevents crops from being trampled by both players and other mobs
 */
public class SpawnInteract implements Listener {

	public SpawnInteract(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from placing blocks at spawn.
	 */
	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getBlock().getX(), e.getBlock().getZ())) {
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
		if (AranarthUtils.isSpawnLocation(e.getBlock().getX(), e.getBlock().getZ())) {
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
			if (AranarthUtils.isSpawnLocation(e.getRightClicked().getLocation().getBlockX(), e.getRightClicked().getLocation().getBlockZ())) {
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
			if (AranarthUtils.isSpawnLocation(e.getEntity().getLocation().getBlockX(), e.getEntity().getLocation().getBlockZ())) {
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
			if (AranarthUtils.isSpawnLocation(e.getEntity().getLocation().getBlockX(), e.getEntity().getLocation().getBlockZ())) {
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
}
