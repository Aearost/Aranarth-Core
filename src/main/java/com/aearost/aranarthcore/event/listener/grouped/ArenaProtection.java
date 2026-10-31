package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Handles preventing behaviour in the arena world.
 */
public class ArenaProtection implements Listener {

	/**
	 * A sign at the arena spawn and the location in the arena that it brings the player to.
	 */
	private record ArenaDropOff(int signX, int signY, int signZ, double x, double y, double z, float yaw, float pitch) {
		private boolean isSign(Location loc) {
			return loc.getBlockX() == signX && loc.getBlockY() == signY && loc.getBlockZ() == signZ;
		}
	}

	// The drop off locations of arena1, each with its own sign. The random teleport sign selects one of them.
	private static final List<ArenaDropOff> ARENA1_DROP_OFFS = List.of(
			new ArenaDropOff(10, 2, 59, 4960.5, 182, 4937.5, -45, 0),
			new ArenaDropOff(-4, 2, 62, 4998.5, 180, 5069.5, -135, 0),
			new ArenaDropOff(-8, 4, 59, 4969.5, 189, 5077.5, -135, 0),
			new ArenaDropOff(8, 2, 66, 5022.5, 180, 4945.5, 0, 0),
			new ArenaDropOff(1, 3, 68, 5044.5, 185, 5024.5, 45, 0),
			new ArenaDropOff(-6, 8, 67, 5014.5, 252, 5068.5, -135, 0),
			new ArenaDropOff(-7, 3, 73, 5078.5, 186.625, 5089.5, 135, 0)
	);

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
	 * Provides whether the player bypasses the building protections in the arena world.
	 */
	private boolean canModify(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		return aranarthPlayer.isInAdminMode() || aranarthPlayer.isInArchitectMode();
	}

	/**
	 * Prevents blocks from being destroyed in the arena world spawn.
	 */
	@EventHandler
	public void onBlockBreak(final BlockBreakEvent e) {
		if (AranarthUtils.isArenaSpawnLocation(e.getBlock().getLocation())) {
			if (!canModify(e.getPlayer())) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.getFor(e.getPlayer(), "access.no_break")));
			}
		}
	}

	/**
	 * Prevents blocks from being placed anywhere in the arena world unless in admin or architect mode.
	 */
	@EventHandler
	public void onBlockPlace(final BlockPlaceEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("arena")) {
			if (!canModify(e.getPlayer())) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.getFor(e.getPlayer(), "arena.cannot_place")));
			}
		}
	}

	/**
	 * Prevents mobs from naturally spawning at the arena spawn.
	 */
	@EventHandler
	public void onMobSpawn(CreatureSpawnEvent e) {
		if (AranarthUtils.isArenaSpawnLocation(e.getLocation())) {
			if (e.getEntityType() == EntityType.ARMOR_STAND || e.getEntityType() == EntityType.MANNEQUIN) {
				return;
			}
			CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();
			if (reason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG && reason != CreatureSpawnEvent.SpawnReason.COMMAND
					&& reason != CreatureSpawnEvent.SpawnReason.CUSTOM) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents entity explosions from destroying blocks at the arena spawn.
	 */
	@EventHandler
	public void onEntityExplode(EntityExplodeEvent e) {
		e.blockList().removeIf(block -> AranarthUtils.isArenaSpawnLocation(block.getLocation()));
	}

	/**
	 * Prevents block explosions from destroying blocks at the arena spawn.
	 */
	@EventHandler
	public void onBlockExplode(BlockExplodeEvent e) {
		e.blockList().removeIf(block -> AranarthUtils.isArenaSpawnLocation(block.getLocation()));
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

	private Double pendingArenaCap = null;
	private UUID pendingArenaCapTarget = null;

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
			reducedHp = e.getDamage(); // 0.5 hearts or less - keep as is
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

		// Store the intended cap - enforced at HIGHEST in onBendingEntityDamageInArena
		// since another plugin inflates the damage event after LOWEST
		pendingArenaCap = reducedHp;
		pendingArenaCapTarget = e.getEntity().getUniqueId();
		e.setDamage(reducedHp);
	}

	/**
	 * Clears the pending arena cap if the ability damage was cancelled after it was stored,
	 * since no damage event will follow to consume it.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBendingDamageInArenaCancelled(AbilityDamageEntityEvent e) {
		if (e.isCancelled() && e.getEntity().getUniqueId().equals(pendingArenaCapTarget)) {
			pendingArenaCap = null;
			pendingArenaCapTarget = null;
		}
	}

	/**
	 * Enforces the arena damage cap at HIGHEST priority on the damage event that ProjectKorra
	 * fires internally after AbilityDamageEntityEvent. This counteracts any other plugin that
	 * inflates bending damage between LOWEST and HIGHEST. Listens to EntityDamageEvent so that
	 * sourceless bending damage (such as bending fire tick) is also consumed and capped.
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBendingEntityDamageInArena(EntityDamageEvent e) {
		if (pendingArenaCap == null) {
			return;
		}
		if (!e.getEntity().getUniqueId().equals(pendingArenaCapTarget)) {
			return;
		}
		double cap = pendingArenaCap;
		pendingArenaCap = null;
		pendingArenaCapTarget = null;

		if (!e.getEntity().getWorld().getName().equalsIgnoreCase("arena")) {
			return;
		}
		e.setDamage(cap);
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
				// Players in admin or architect mode can edit the signs instead of using them
				if (canModify(player)) {
					return;
				}

				// Random teleport into arena1
				if (loc.getBlockX() == 0 && loc.getBlockY() == 2 && loc.getBlockZ() == 56) {
					e.setCancelled(true);
					teleportToDropOff(player, ARENA1_DROP_OFFS.get(new Random().nextInt(ARENA1_DROP_OFFS.size())));
					return;
				}
				// Specific drop off signs of arena1
				for (ArenaDropOff dropOff : ARENA1_DROP_OFFS) {
					if (dropOff.isSign(loc)) {
						e.setCancelled(true);
						teleportToDropOff(player, dropOff);
						return;
					}
				}
				// Arrows
				if (loc.getBlockX() == -7 && loc.getBlockY() == 2 && loc.getBlockZ() == 25) {
					e.setCancelled(true);
					for (int i = 0; i < 9; i++) {
						player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
					}
				}
				// Iron Ingots
				else if (loc.getBlockX() == -7 && loc.getBlockY() == 2 && loc.getBlockZ() == 27) {
					e.setCancelled(true);
					for (int i = 0; i < 9; i++) {
						player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 64));
					}
				}
				// Water Bottles
				else if (loc.getBlockX() == -7 && loc.getBlockY() == 2 && loc.getBlockZ() == 26) {
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
	 * Teleports the player to the arena drop off location.
	 * @param player The player.
	 * @param dropOff The drop off location.
	 */
	private void teleportToDropOff(Player player, ArenaDropOff dropOff) {
		Location arenaLoc = new Location(Bukkit.getWorld("arena"), dropOff.x(), dropOff.y(), dropOff.z(), dropOff.yaw(), dropOff.pitch());
		AranarthUtils.teleportPlayer(player, player.getLocation(), arenaLoc, true, Lang.getFor(player, "teleport.title_arena"), Lang.getFor(player, "arena.tp_success"), success -> {
			if (success) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "arena.tp_success")));
			} else {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "arena.tp_fail")));
			}
		});
	}

	/**
	 * Prevents players from modifying signs in the arena.
	 */
	@EventHandler
	public void onSignOpen(PlayerOpenSignEvent e) {
		if (e.getSign().getLocation().getWorld().getName().equalsIgnoreCase("arena")) {
			if (!canModify(e.getPlayer())) {
				e.setCancelled(true);
			}
		}
	}
}
