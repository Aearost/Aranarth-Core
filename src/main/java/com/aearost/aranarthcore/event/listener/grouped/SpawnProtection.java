package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

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
	public void onMobSpawn(CreatureSpawnEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getLocation())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Prevents players from enabling their bending at spawn.
	 */
	@EventHandler
	public void onToggleBending(PlayerCommandPreprocessEvent e) {
		Player player = e.getPlayer();
		if (AranarthUtils.isSpawnLocation(player.getLocation())) {
			String[] parts = e.getMessage().split(" ");
			if (parts.length > 1) {
				if (parts[0].startsWith("/b")) {
					if (parts[1].startsWith("t")) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot toggle your bending at Spawn!"));
					}
				}
			}
		}
	}

	/**
	 * Automatically toggles the player's bending based on their movement to and from spawn.
	 */
	@EventHandler
	public void onExitSpawn(PlayerMoveEvent e) {
		toggleBendingForLocation(e.getPlayer(), e.getFrom(), e.getTo());
	}

	/**
	 * Automatically toggles the player's bending based on their teleportation to and from spawn.
	 */
	@EventHandler
	public void onTeleportFromSpawn(PlayerTeleportEvent e) {
		toggleBendingForLocation(e.getPlayer(), e.getFrom(), e.getTo());
	}

	/**
	 * Handles the actual toggling of the player's bending based on movement or teleportation to and from spawn.
	 * @param player The player.
	 * @param from The player's previous location before the movement or teleportation.
	 * @param to The player's previous location after the movement or teleportation.
	 */
	private void toggleBendingForLocation(Player player, Location from, Location to) {
		boolean fromSpawn = AranarthUtils.isSpawnLocation(from.getBlock().getLocation());
		boolean toSpawn = AranarthUtils.isSpawnLocation(to.getBlock().getLocation());

		// Leaving spawn
		if (fromSpawn && !toSpawn) {
			BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
			if (!bPlayer.isToggled()) {
				bPlayer.toggleBending();
				return;
			}
		}

		// Consistently keep bending disabled at spawn
		if (AranarthUtils.isSpawnLocation(to)) {
			BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
			if (bPlayer.isToggled()) {
				bPlayer.toggleBending();
			}
		}
	}

	/**
	 * Prevents players from dealing and taking damage at spawn.
	 */
	@EventHandler
	private void onDamage(EntityDamageEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getEntity().getLocation())) {
			if (e.getEntity() instanceof Player target) {
				if (e.getDamageSource().getCausingEntity() instanceof Player attacker) {
					attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm players at Spawn!"));
				}
				// Additionally prevents the target from being damaged, regardless of the source of damage
				e.setCancelled(true);
			}
		}
	}
}
