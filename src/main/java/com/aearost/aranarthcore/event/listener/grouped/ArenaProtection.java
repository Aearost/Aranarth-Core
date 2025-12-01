package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Handles preventing behaviour in the arena world.
 */
public class ArenaProtection implements Listener {

	public ArenaProtection(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents vines from being grown in the arena world.
	 */
	@EventHandler
	public void onVineGrow(BlockSpreadEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("arena")) {
			Material material = e.getSource().getType();
			if (material == Material.VINE || material == Material.CAVE_VINES_PLANT) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents blocks from being destroyed in the arena world spawn
	 */
	@EventHandler
	public void onBlockBreak(final BlockBreakEvent e) {
		if (isArenaSpawn(e.getBlock().getLocation())) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
			if (aranarthPlayer.getCouncilRank() != 3) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot break this!"));
			}
		}
	}

	/**
	 * Prevents armour from being damaged in the arena world.
	 */
	@EventHandler
	public void onArmorTakeDamage(final PlayerItemDamageEvent e) {
		if (e.getPlayer().getWorld().getName().equalsIgnoreCase("arena")) {
			e.setCancelled(true);
		}

	}

	/**
	 * Prevents items from being dropped in the arena world aside from iron ingots and arrows.
	 */
	@EventHandler
	public void onItemDrop(ItemSpawnEvent e) {
		if (e.getLocation().getWorld().getName().equalsIgnoreCase("arena")) {
			if (e.getEntity().getItemStack().getType() != Material.IRON_INGOT
					&& e.getEntity().getItemStack().getType() != Material.ARROW) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents ice and snow from melting in the arena world.
	 */
	@EventHandler
	public void onMelt(BlockFadeEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("arena")) {
			Material material = e.getBlock().getType();
			if (material == Material.ICE || material == Material.PACKED_ICE || material == Material.BLUE_ICE ||
					material == Material.SNOW || material == Material.SNOW_BLOCK) {
				e.setCancelled(true);
			}
		}

	}

	/**
	 * Prevents hunger from being lost in the arena world.
	 */
	@EventHandler
	public void onArenaHungerDeplete(FoodLevelChangeEvent e) {
		if (e.getEntity().getWorld().getName().equalsIgnoreCase("arena")) {
			e.setCancelled(true);
		}
	}

	/**
	 * Determines if the location is within the Arena spawn area.
	 * @param loc The location.
	 * @return Confirmation if the location is within the Arena spawn area.
	 */
	private boolean isArenaSpawn(Location loc) {
		if (!loc.getWorld().getName().equals("arena")) {
			return false;
		}

		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		return (x >= -4 && x <= 4) && (y >= 100 && y <= 111) && (z >= -4 && z <=4);
	}

	/**
	 * Prevents players from dealing and taking damage at the arena spawn.
	 */
	@EventHandler
	private void onDamage(EntityDamageEvent e) {
		if (isArenaSpawn(e.getEntity().getLocation())) {
			if (e.getDamageSource().getCausingEntity() instanceof Player attacker) {
				attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm players here!"));
			}
			// Additionally prevents the target from being damaged, regardless of the source of damage
			e.setCancelled(true);
		}
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
		boolean isFromArenaSpawn = isArenaSpawn(from.getBlock().getLocation());
		boolean isToArenaSpawn = isArenaSpawn(to.getBlock().getLocation());

		// Consistently keep bending disabled at the arena spawn
		if (isArenaSpawn(to)) {
			BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
			if (bPlayer != null) {
				if (bPlayer.isToggled()) {
					bPlayer.toggleBending();
				}
			}
		}

		// Leaving the arena spawn
		if (!isToArenaSpawn) {
			BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
			if (bPlayer != null) {
				if (!bPlayer.isToggled()) {
					bPlayer.toggleBending();
					return;
				}
			}
		}
	}

	/**
	 * Prevents players from enabling their bending at spawn.
	 */
	@EventHandler
	public void onToggleBending(PlayerCommandPreprocessEvent e) {
		Player player = e.getPlayer();
		if (isArenaSpawn(player.getLocation())) {
			String[] parts = e.getMessage().split(" ");
			if (parts.length > 1) {
				if (parts[0].startsWith("/b")) {
					if (parts[1].startsWith("t")) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot toggle your bending here!"));
					}
				}
			}
		}
	}
}
