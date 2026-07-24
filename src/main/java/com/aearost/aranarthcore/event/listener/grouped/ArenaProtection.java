package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
	 * Prevents blocks from being destroyed in the arena world spawn.
	 */
	@EventHandler
	public void onBlockBreak(final BlockBreakEvent e) {
		if (isArenaSpawn(e.getBlock().getLocation())) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
			if (!aranarthPlayer.isInAdminMode()) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot break this!"));
			}
		}
	}

	/**
	 * Prevents blocks from being placed in the arena world spawn if somehow obtained.
	 */
	@EventHandler
	public void onBlockPlace(final BlockPlaceEvent e) {
		if (isArenaSpawn(e.getBlock().getLocation())) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
			if (!aranarthPlayer.isInAdminMode()) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot place this here!"));
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

		return (x >= -10 && x <= 10) && (y >= 100 && y <= 111) && (z >= -10 && z <= 10);
	}

	/**
	 * Tracks the arena-capped damage value from AbilityDamageEntityEvent so it can be enforced
	 * in EntityDamageByEntityEvent at HIGHEST priority (after any other plugins inflate the value).
	 * Set to null when not in an ability damage pipeline.
	 */
	private Double pendingArenaCap = null;

	/**
	 * Reduces bending ability damage in the arena world using a tiered scale.
	 * Stores the capped value so it can be enforced in onBendingEntityDamageInArena.
	 * Damage is expressed in health points (2 HP = 1 heart).
	 */
	@EventHandler
	public void onBendingDamageInArena(AbilityDamageEntityEvent e) {
		if (!e.getEntity().getWorld().getName().equalsIgnoreCase("arena")) {
			return;
		}

		double hearts = e.getDamage() / 2.0;

		double reducedHp;
		if (hearts <= 0.5) {
			reducedHp = e.getDamage(); // 0.5 hearts or less — keep as is
		} else if (hearts <= 1.5) {
			reducedHp = 2.0;       // cap at 1 heart
		} else if (hearts <= 3.0) {
			reducedHp = 3.0;       // cap at 1.5 hearts
		} else if (hearts <= 6.0) {
			reducedHp = 4.0;       // cap at 2 hearts
		} else if (hearts <= 10.0) {
			reducedHp = 5.0;       // cap at 2.5 hearts
		} else {
			reducedHp = 6.0;       // cap at 3 hearts (hard max)
		}

		// Store the intended cap — enforced at HIGHEST in onBendingEntityDamageInArena
		// since another plugin inflates the EntityDamageByEntityEvent after LOWEST
		pendingArenaCap = reducedHp;
		e.setDamage(reducedHp);
	}

	/**
	 * Enforces the arena damage cap at HIGHEST priority on the EntityDamageByEntityEvent
	 * that ProjectKorra fires internally after AbilityDamageEntityEvent. This counteracts
	 * any other plugin that inflates bending damage between LOWEST and HIGHEST.
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBendingEntityDamageInArena(EntityDamageByEntityEvent e) {
		if (pendingArenaCap == null) {
			return;
		}
		double cap = pendingArenaCap;
		pendingArenaCap = null;

		if (!e.getEntity().getWorld().getName().equalsIgnoreCase("arena")) {
			return;
		}
		e.setDamage(cap);
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
	 * Adds functionality to the signs at the Arena spawn
	 */
	@EventHandler
	public void onSignClick(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null && e.getClickedBlock().getType().name().endsWith("_SIGN")) {
			Player player = e.getPlayer();
			Location loc = e.getClickedBlock().getLocation();
			if (loc.getWorld().getName().equalsIgnoreCase("arena")) {
				// Small arena
				if (loc.getBlockX() == -1 && loc.getBlockY() == 106 && loc.getBlockZ() == -3) {
					e.setCancelled(true);
					Location arenaLoc = new Location(Bukkit.getWorld("arena"), 1000, 101, 1000);
					AranarthUtils.teleportPlayer(player, player.getLocation(), arenaLoc, true, "&e&lSmall Arena", "&7You have teleported to the Small Arena", success -> {
						if (success) {
							player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the &eSmall Arena"));
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cYou could not be teleported to the &eSmall Arena"));
						}
					});
				}
				// Large arena
				else if (loc.getBlockX() == 1 && loc.getBlockY() == 106 && loc.getBlockZ() == -3) {
					e.setCancelled(true);
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
					AranarthUtils.teleportPlayer(player, player.getLocation(), arenaLoc, true, "&e&lLarge Arena", "&7You have teleported to the Large Arena", success -> {
						if (success) {
							player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the &eLarge Arena"));
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cYou could not be teleported to the &eLarge Arena"));
						}
					});
				}
				// Arrows
				else if (loc.getBlockX() == 1 && loc.getBlockY() == 106 && loc.getBlockZ() == 3) {
					e.setCancelled(true);
					for (int i = 0; i < 9; i++) {
						player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
					}
				}
				// Iron Ingots
				else if (loc.getBlockX() == 0 && loc.getBlockY() == 106 && loc.getBlockZ() == 3) {
					e.setCancelled(true);
					for (int i = 0; i < 9; i++) {
						player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 64));
					}
				}
				// Water Bottles
				else if (loc.getBlockX() == -1 && loc.getBlockY() == 106 && loc.getBlockZ() == 3) {
					e.setCancelled(true);
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

	/**
	 * Prevents players from modifying signs in the arena.
	 */
	@EventHandler
	public void onSignOpen(PlayerOpenSignEvent e) {
		if (e.getSign().getLocation().getWorld().getName().equalsIgnoreCase("arena")) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
			if (!aranarthPlayer.isInAdminMode()) {
				e.setCancelled(true);
			}
		}
	}
}
