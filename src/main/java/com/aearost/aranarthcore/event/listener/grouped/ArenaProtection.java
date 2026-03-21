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
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Random;

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
	public void onTeleport(PlayerTeleportEvent e) {
		if (e.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || e.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
			toggleBendingForLocation(e.getPlayer(), e.getFrom(), e.getTo());
		}
	}

	/**
	 * Handles toggling the player's bending as they move at the arena spawn.
	 * @param e The event.
	 */
	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		if (e.getPlayer().getLocation().getWorld().getName().equals("arena")) {
			toggleBendingForLocation(e.getPlayer(), e.getFrom(), e.getTo());
		}
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
		if (isFromArenaSpawn && !isToArenaSpawn) {
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

	/**
	 * Adds functionality to the signs at the Arena spawn
	 */
	@EventHandler
	public void onSignClick(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null && e.getClickedBlock().getType().name().endsWith("_SIGN")) {
			Player player = e.getPlayer();
			Location loc = e.getClickedBlock().getLocation();
			// Small arena
			if (loc.getBlockX() == -1 && loc.getBlockY() == 106 && loc.getBlockZ() == -3) {
				Location arenaLoc = new Location(Bukkit.getWorld("arena"), 1000, 101, 1000);
				AranarthUtils.teleportPlayer(player, player.getLocation(), arenaLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the &eSmall Arena"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou could not be teleported to the &eSmall Arena"));
					}
				});
			}
			// Large arena
			else if (loc.getBlockX() == 1 && loc.getBlockY() == 106 && loc.getBlockZ() == -3) {
				Location arenaLoc = null;
				int locationNum = new Random().nextInt(5);
				if (locationNum == 0) {
					arenaLoc = new Location(Bukkit.getWorld("arena"), -1000, 101, -1000);
				} else if (locationNum == 1) {
					arenaLoc = new Location(Bukkit.getWorld("arena"), -960, 104, -1050, 45, 0);
				} else if (locationNum == 2) {
					arenaLoc = new Location(Bukkit.getWorld("arena"), -1068, 105, -1026, -45, 0);
				} else if (locationNum == 3) {
					arenaLoc = new Location(Bukkit.getWorld("arena"), -1042, 103, -937, -135, 0);
				} else if (locationNum == 4) {
					arenaLoc = new Location(Bukkit.getWorld("arena"), -940, 104, -947, 135, 0);
				}
				AranarthUtils.teleportPlayer(player, player.getLocation(), arenaLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the &eLarge Arena"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou could not be teleported to the &eLarge Arena"));
					}
				});
			}
			// Arrows
			else if (loc.getBlockX() == 1 && loc.getBlockY() == 106 && loc.getBlockZ() == 3) {
				for (int i = 0; i < 9; i++) {
					player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
				}
			}
			// Water Bottles
			else if (loc.getBlockX() == -1 && loc.getBlockY() == 106 && loc.getBlockZ() == 3) {
				ItemStack bottle = new ItemStack(Material.POTION);
				PotionMeta meta = (PotionMeta) bottle.getItemMeta();
				meta.setBasePotionType(PotionType.WATER);
				bottle.setItemMeta(meta);
				for (int i = 0; i < 9; i++) {
					player.getInventory().addItem(bottle);
				}
			}
		}
	}
}
