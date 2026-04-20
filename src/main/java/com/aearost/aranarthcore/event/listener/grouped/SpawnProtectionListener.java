package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;

import java.util.List;
import java.util.Random;

/**
 * Prevents crops from being trampled by both players and other mobs
 */
public class SpawnProtectionListener implements Listener {

	public SpawnProtectionListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from placing blocks at spawn.
	 */
	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getBlock().getLocation())) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
			if (!aranarthPlayer.isInAdminMode()) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents players from breaking blocks at spawn.
	 */
	@EventHandler
	public void onBreak(BlockBreakEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getBlock().getLocation())) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
			if (!aranarthPlayer.isInAdminMode()) {
				e.setCancelled(true);
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
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
				if (!aranarthPlayer.isInAdminMode()) {
					e.setCancelled(true);
				}
			}
		}
	}

	/**
	 * Prevents players from taking or placing items on armor stands at spawn.
	 */
	@EventHandler
	public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getRightClicked().getLocation())) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
			if (!aranarthPlayer.isInAdminMode()) {
				e.setCancelled(true);
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
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
				if (!aranarthPlayer.isInAdminMode()) {
					e.setCancelled(true);
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
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					if (!aranarthPlayer.isInAdminMode()) {
						e.setCancelled(true);
					}
				}
			}
		}
	}

	/**
	 * Prevents players from placing item frames and paintings at spawn.
	 */
	@EventHandler
	public void onEntityPlace(HangingPlaceEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getEntity().getLocation())) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
			if (!aranarthPlayer.isInAdminMode()) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents players from modifying signs at spawn.
	 */
	@EventHandler
	public void onSignOpen(PlayerOpenSignEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getSign().getLocation())) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
			if (!aranarthPlayer.isInAdminMode()) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents dynamic fire from spreading at spawn and prevents lightning fire from spawning.
	 */
	@EventHandler
	public void onFireCreate(BlockIgniteEvent e) {
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
			if (e.getEntityType() != EntityType.ARMOR_STAND && e.getEntityType() != EntityType.MANNEQUIN) {
				// Allow the spawning of the Quest NPC
				if (e.getEntity() instanceof Villager villager) {
					if (!ChatUtils.stripColorFormatting(villager.getCustomName()).equals("Quest Master")) {
						e.setCancelled(true);
					} else {
						return;
					}
				}

				// Cannot get the player who placed it but can get nearby players and prevent
				List<Entity> nearby = e.getEntity().getNearbyEntities(8, 8, 8);
				for (Entity entity : nearby) {
					if (entity instanceof Player player) {
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
						if (!aranarthPlayer.isInAdminMode()) {
							e.setCancelled(true);
						}
					}
				}
			}
		}
	}

	/**
	 * Prevents containers from being opened at spawn.
	 */
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		Block block = e.getClickedBlock();
		if (block != null) {
			if (AranarthUtils.isSpawnLocation(block.getLocation())) {
				if (AranarthUtils.isContainerBlock(block) || block.getType().name().endsWith("_SIGN") || block.getType() == Material.NOTE_BLOCK
						|| block.getType() == Material.SMOKER || block.getType() == Material.BLAST_FURNACE || block.getType() == Material.FURNACE
						|| block.getType() == Material.JUKEBOX || block.getType() == Material.LEVER || block.getType().name().endsWith("_TRAPDOOR")
						|| block.getType().name().endsWith("_DOOR") || block.getType().name().endsWith("_GATE")
						|| block.getType() == Material.CRAFTER || block.getType() == Material.HOPPER || block.getType().name().endsWith("_SHELF")
						|| block.getType() == Material.DECORATED_POT || block.getType() == Material.FLOWER_POT || block.getType() == Material.CHISELED_BOOKSHELF
						|| block.getType() == Material.SWEET_BERRY_BUSH || block.getType() == Material.CAVE_VINES || block.getType() == Material.CAVE_VINES_PLANT) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
					if (!aranarthPlayer.isInAdminMode()) {
						// Allow server shops to be used at spawn
						if (block.getType().name().endsWith("_SIGN") && ShopUtils.getShopFromLocation(block.getLocation()) != null) {
							return;
						}

						e.setCancelled(true);
					}
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
		if (AranarthUtils.isSpawnLocation(player.getLocation())) {
			String[] parts = e.getMessage().split(" ");
			if (parts.length > 1) {
				if (parts[0].startsWith("/b") || parts[0].startsWith("/bending")) {
					if (parts[1].equals("t") || parts[1].equals("toggle")) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot toggle your bending at Spawn!"));
					}
				}
			}
		}
	}

	/**
	 * Prevents ender pearls from being thrown in the spawn world.
	 */
	@EventHandler
	private void onEnderPearlThrow(ProjectileLaunchEvent e) {
		if (e.getLocation().getWorld().getName().equals("spawn")) {
			if (e.getEntityType() == EntityType.ENDER_PEARL || e.getEntityType() == EntityType.ARROW
				|| e.getEntityType() == EntityType.SPECTRAL_ARROW || e.getEntityType() == EntityType.TRIDENT
				|| e.getEntityType() == EntityType.FIREBALL || e.getEntityType() == EntityType.SMALL_FIREBALL
				|| e.getEntityType() == EntityType.DRAGON_FIREBALL || e.getEntityType() == EntityType.WITHER_SKULL
				|| e.getEntityType() == EntityType.SPLASH_POTION || e.getEntityType() == EntityType.LINGERING_POTION
				|| e.getEntityType() == EntityType.FISHING_BOBBER || e.getEntityType() == EntityType.WIND_CHARGE) {

				if (e.getEntity().getShooter() != null && e.getEntity().getShooter() instanceof Player player) {
					e.setCancelled(true);
					player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this item at spawn!"));
				}
			}
		}
	}



	/**
	 * Prevents chorus fruits from being eaten in the spawn world.
	 */
	@EventHandler
	private void onChorusFruitEat(PlayerItemConsumeEvent e) {
		if (e.getPlayer().getLocation().getWorld().getName().equals("spawn")) {
			if (e.getItem().getType() == Material.CHORUS_FRUIT) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot use this item at spawn!"));
			}
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
	 * Automatically toggles the player's bending based on their teleportation to and from spawn.
	 */
	@EventHandler
	public void onPlayerDeath(PlayerRespawnEvent e) {
		toggleBendingForLocation(e.getPlayer(), e.getRespawnLocation(), e.getRespawnLocation());
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
			if (bPlayer != null) {
				if (!bPlayer.isToggled()) {
					Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> bPlayer.toggleBending(), 1L);
					return;
				}
			}
		}

		// Consistently keep bending disabled at spawn
		if (AranarthUtils.isSpawnLocation(to)) {
			BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
			if (bPlayer != null) {
				if (bPlayer.isToggled()) {
					bPlayer.toggleBending();
				}
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

	/**
	 * Handles teleporting a player back to spawn when they fall into the void of the spawn world.
	 */
	@EventHandler
	private void onMove(PlayerMoveEvent e) {
		Player player = e.getPlayer();
		if (player.getWorld().getName().equals("spawn")) {
			// Ensures that any movement at spawn toggles bending if not toggled already
			toggleBendingForLocation(e.getPlayer(), e.getFrom(), e.getTo());

			// If they did not move to a different coordinate and only their mouse
			if (e.getTo() == null) {
				return;
			}

			int x = e.getTo().getBlockX();
			int y = e.getTo().getBlockY();
			int z = e.getTo().getBlockZ();

			// World portals
			boolean isEnteringSurvivalPortal = (x >= -4 && x <= 5) && (y >= 101 && y <= 111) && (z == -100);
			boolean isEnteringResourcePortal = (x >= -24 && x <= -15) && (y >= 107 && y <= 117) && (z == -109);
			boolean isEnteringArenaPortal = (x >= 16 && x <= 24) && (y >= 112 && y <= 121) && (z == -97);

			// Tutorial entry portals
			boolean isEnteringBendingPortal = (x >= 50 && x <= 52) && (y >= 103 && y <= 106) && z == -59;
			boolean isEnteringAranarthiumPortal = (x >= 55 && x <= 57) && (y >= 105 && y <= 108) && z == -49;
			boolean isEnteringRecipePortal = (z >= -59 && z <= -57) && (y >= 107 && y <= 110) && x == 65;
			boolean isEnteringCalendarPortal = (z >= -69 && z <= -67) && (y >= 109 && y <= 112) && x == 65;
			boolean isEnteringShopsPortal = (z >= -68 && z <= -66) && (y >= 111 && y <= 114) && x  == 47;
			boolean isEnteringRanksPortal = (z >= -76 && z <= -74) && (y >= 111 && y <= 114) && x == 51;
			boolean isEnteringDominionsPortal = (x >= 56 && x <= 58) && (y >= 112 && y <= 115) && z == -79;

			// Tutorial exit portals
			boolean isExitingBendingPortal = (x >= 2999 && x <= 3001) && (y >= 100 && y <= 102) && z == 65;
			boolean isExitingAranarthiumPortal = (x >= 3999 && x <= 4001) && (y >= 100 && y <= 102) && z == 71;
			boolean isExitingRecipesPortal = (x >= 8999 && x <= 9001) && (y >= 100 && y <= 102) && z == 143;
			boolean isExitingCalendarPortal = (x >= 4999 && x <= 5001) && (y >= 100 && y <= 102) && z == 107;
			boolean isExitingShopsPortal = (x >= 5999 && x <= 6001) && (y >= 100 && y <= 102) && z == 89;
			boolean isExitingRanksPortal = (x >= 6999 && x <= 7001) && (y >= 100 && y <= 102) && z == 89;
			boolean isExitingDominionsPortal = (x >= 7999 && x <= 8001) && (y >= 100 && y <= 102) && z == 95;

			// World portals
			if (isEnteringSurvivalPortal) {
				teleportPlayerToWorld(player, "world");
				return;
			} else if (isEnteringResourcePortal) {
				teleportPlayerToWorld(player, "resource");
				return;
			} else if (isEnteringArenaPortal) {
				teleportPlayerToWorld(player, "arena");
				return;
			}
			// Tutorial entry portals
			else if (isEnteringBendingPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 3000.5, 100, 0.5, 0, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the &5Bending Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThis tutorial is currently disabled"));
					}
				});
				return;
			} else if (isEnteringAranarthiumPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 4000.5, 100, 0.5, 0, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the &bAranarthium Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThis tutorial is currently disabled"));
					}
				});
				return;
			} else if (isEnteringRecipePortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 9000.5, 100, 0.5, 0, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the &fRecipe Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThis tutorial is currently disabled"));
					}
				});
				return;
			} else if (isEnteringCalendarPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 5000.5, 100, 0.5, 0, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the &eCalendar Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThis tutorial is currently disabled"));
					}
				});
				return;
			} else if (isEnteringShopsPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 6000.5, 100, 0.5, 0, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the &aShops Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThis tutorial is currently disabled"));
					}
				});
				return;
			} else if (isEnteringRanksPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 7000.5, 100, 0.5, 0, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the &4Ranks Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThis tutorial is currently disabled"));
					}
				});
				return;
			} else if (isEnteringDominionsPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 8000.5, 100, 0.5, 0, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the &6Dominions Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThis tutorial is currently disabled"));
					}
				});
				return;
			}
			// Tutorial exit portals
			else if (isExitingBendingPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 51.5, 103, -56.5, 0, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have exited the &5Bending Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong. Please use &e/spawn"));
					}
				});
				return;
			} else if (isExitingAranarthiumPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 56.5, 104, -51.5, 180, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have exited the &bAranarthium Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong. Please use &e/spawn"));
					}
				});
				return;
			} else if (isExitingRecipesPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 62.5, 106, -57.5, 90, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have exited the &fRecipe Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong. Please use &e/spawn"));
					}
				});
				return;
			} else if (isExitingCalendarPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 63.5, 109, -67.5, 90, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have exited the &eCalendar Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong. Please use &e/spawn"));
					}
				});
				return;
			} else if (isExitingShopsPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 50.5, 110, -66.5, -90, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have exited the &aShops Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong. Please use &e/spawn"));
					}
				});
				return;
			} else if (isExitingRanksPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 55, 110, -74.5, -45, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have exited the &4Ranks Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong. Please use &e/spawn"));
					}
				});
				return;
			} else if (isExitingDominionsPortal) {
				Location tutorialLoc = new Location(Bukkit.getWorld("spawn"), 57.5, 111, -74.5, 0, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), tutorialLoc, true, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have exited the &6Dominions Tutorial"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong. Please use &e/spawn"));
					}
				});
				return;
			}
			else {
				boolean inSpawn = (x >= -170 && x <= 170) && (z >= -220 && z <= 130);
				boolean inOuterBounds = (x >= -250 && x <= 250) && (z >= -300 && z <= 200);
				boolean inBufferZone = inOuterBounds && !inSpawn;
				boolean isTooLow = e.getTo().getY() <= 50;

				if (isTooLow || inBufferZone) {
					Location spawn = new Location(Bukkit.getWorld("spawn"), 0, 100, 0, 180, 0);
					Location locToTeleportTo = AranarthUtils.getSafeTeleportLocation(spawn);
					e.getPlayer().teleport(locToTeleportTo);
					e.getPlayer().playSound(e.getPlayer(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
				}
			}
		}
	}

	/**
	 * Teleports the player to the input world name.
	 * @param player The player.
	 * @param worldName The name of the world the player is teleporting to.
	 */
	private void teleportPlayerToWorld(Player player, String worldName) {
		if (AranarthUtils.getTeleportTask(player.getUniqueId()) != null) {
			player.sendMessage(ChatUtils.chatMessage("&cYou are already teleporting somewhere!"));
			return;
		}

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (System.currentTimeMillis() < aranarthPlayer.getLastWorldCommandUse() + 60000) {
			if (!aranarthPlayer.isInAdminMode()) {
				int wait = (int) ((aranarthPlayer.getLastWorldCommandUse() + 60000) - System.currentTimeMillis()) / 1000;
				player.sendMessage(ChatUtils.chatMessage("&cYou must wait another &e" + wait + " seconds &cto teleport again!"));
				return;
			}
		}

		String adjustedName = "&eSurvival";
		World world = Bukkit.getWorld(worldName);
		if (world != null) {
			Random random = new Random();
			Location selectedLocation = null;
			boolean isLocationFound = false;
			while (!isLocationFound) {
				int x = 0;
				int z = 0;

				if (worldName.equals("world")) {
					x = random.nextInt(24501) - 12250;
					z = random.nextInt(24501) - 12250;
				} else if (worldName.equals("resource")) {
					x = random.nextInt(5001) - 2500;
					z = random.nextInt(5001) - 2500;
					adjustedName = "the &eResource &7world";
				} else if (worldName.equals("arena")) {
					selectedLocation = new Location(Bukkit.getWorld("arena"), 0.5, 105, 0.5, 180, 0);
					adjustedName = "the &eArena";
					break;
				}

				if (world.getHighestBlockAt(x, z).getType() != Material.WATER) {
					isLocationFound = true;
					selectedLocation = world.getHighestBlockAt(x, z).getLocation();
					selectedLocation.add(0, 1, 0);
				}
			}


			String finalAdjustedName = adjustedName;
			AranarthUtils.teleportPlayer(player, player.getLocation(), selectedLocation, true, success -> {
				if (success) {
					aranarthPlayer.setLastWorldCommandUse(System.currentTimeMillis());
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to " + finalAdjustedName));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to " + finalAdjustedName));
				}
			});
		}
	}

	/**
	 * Prevents water from flowing at spawn.
	 */
	@EventHandler
	private void onWaterFlow(BlockFromToEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getBlock().getLocation())) {
			if (e.getBlock().getType() == Material.WATER) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents vines from being grown in the spawn world.
	 */
	@EventHandler
	public void onVineGrow(BlockSpreadEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("spawn")) {
			Material material = e.getSource().getType();
			if (material == Material.VINE || material == Material.CAVE_VINES_PLANT) {
				e.setCancelled(true);
			}
		}
	}

}
