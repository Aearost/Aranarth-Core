package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.abilities.airbending.AstralProjection;
import com.aearost.aranarthcore.abilities.waterbending.VineWhip;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.*;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Handles all logic regarding the use of AranarthCore bending abilities.
 */
public class AranarthCoreBendingListener implements Listener {

	private final HashMap<UUID, AstralProjection> activeProjections = new HashMap<>();

	public AranarthCoreBendingListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles reloading the abilities when ProjectKorra is reloaded.
	 * @param event The event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPKReload(final BendingReloadEvent event) {
		new BukkitRunnable() {
			@Override
			public void run() {
				CoreAbility.registerPluginAbilities(AranarthCore.getInstance(), "com.aearost.aranarthcore.abilities");
				Bukkit.getLogger().info("AranarthCore Bending Reloaded");
			}
		}.runTaskLater(AranarthCore.getInstance(), 1L);
	}
	
	/**
	 * Deals with cancelling explosion block damage.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerSneak(final PlayerToggleSneakEvent e) {
		Player player = e.getPlayer();
		BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
		if (bendingPlayer == null) {
			return;
		}

		CoreAbility ability = bendingPlayer.getBoundAbility();
		String abilityName = bendingPlayer.getBoundAbilityName();

		if (e.isSneaking() && player.getGameMode() == GameMode.SURVIVAL) {
			if (!bendingPlayer.canCurrentlyBendWithWeapons()) {
				return;
			}

			// Airbending
			if (ability instanceof AirAbility && bendingPlayer.isElementToggled(Element.AIR)) {
				if (ability instanceof SpiritualAbility) {
					if (abilityName.equalsIgnoreCase("astralprojection")) {
						AstralProjection astralProjection = new AstralProjection(e.getPlayer());
						activeProjections.put(player.getUniqueId(), astralProjection);
					}
				}
			}
			// Waterbending
			else if (ability instanceof WaterAbility && bendingPlayer.isElementToggled(Element.WATER)) {
				if (ability instanceof PlantAbility) {
					if (abilityName.equalsIgnoreCase("vinewhip")) {
						new VineWhip(e.getPlayer());
					}
				}
			}
			// Earthbending
			else if (ability instanceof EarthAbility && bendingPlayer.isElementToggled(Element.EARTH)) {
				if (abilityName.equalsIgnoreCase("earthtunnel") || abilityName.equalsIgnoreCase("collapse")) {
					e.setCancelled(preventEarthAbility(player));
				}
			}
		}
	}

	/**
	 * Prevents the Earth ability from being used if they do not have access to the Dominion.
	 * @param player The player using the ability.
	 * @return Whether the player can use the ability.
	 */
	private boolean preventEarthAbility(Player player) {
		List<Chunk> chunks = new ArrayList<>();
		int chunkX = player.getLocation().getChunk().getX();
		int chunkZ = player.getLocation().getChunk().getZ();
		for (int x = chunkX - 1; x <= chunkX + 1; x++) {
			for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
				Chunk chunk = player.getWorld().getChunkAt(x, z);
				chunks.add(chunk);
			}
		}

		for (Chunk chunk : chunks) {
			if (shouldCancelEarthAbility(DominionUtils.getDominionOfChunk(chunk), player)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether the EarthTunnel should be canceled.
	 * @param dominion The Dominion of the chunk.
	 * @param player The Player using the ability.
	 * @return Whether the EarthTunnel should be canceled.
	 */
	private boolean shouldCancelEarthAbility(Dominion dominion, Player player) {
		if (dominion == null) {
			return false;
		}

		Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		boolean areAllied = false;
		for (int i = 0; i < dominion.getAllied().size(); i++) {
			UUID uuid = dominion.getAllied().get(i);
			Dominion alliedDominion = DominionUtils.getPlayerDominion(uuid);
			if (alliedDominion.getLeader().equals(playerDominion.getLeader())) {
				areAllied = true;
				break;
			}
		}

		boolean containsMember = dominion.getMembers().contains(player.getUniqueId());
		return !dominion.getMembers().contains(player.getUniqueId()) && !areAllied;
	}

	//  Below for AstralProjection overrides
	@EventHandler
	public void onMannequinDamage(final EntityDamageEvent e) {
		if (e.getEntityType() == EntityType.MANNEQUIN) {
			if (e.getCause() == EntityDamageEvent.DamageCause.KILL) {
				return;
			}

			e.setDamage(0);
			UUID toRemove = null;
			for (UUID uuid : activeProjections.keySet()) {
				AstralProjection projection = activeProjections.get(uuid);
				if (projection.getMannequin().equals(e.getEntity())) {
					toRemove = uuid;
				}
			}

			if (activeProjections.get(toRemove) != null) {
				activeProjections.get(toRemove).endAbility();
				activeProjections.remove(toRemove);
			}
		}
	}

	@EventHandler
	public void onGamemodeChange(PlayerGameModeChangeEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR && e.getNewGameMode() == GameMode.SURVIVAL) {
			if (e.getCause() == PlayerGameModeChangeEvent.Cause.PLUGIN) {
                activeProjections.remove(e.getPlayer().getUniqueId());
			}
		}
	}

	@EventHandler
	public void onSpectateEntity(PlayerInteractAtEntityEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onSpectatePlayerAttempt(PlayerTeleportEvent e) {
		Player player = e.getPlayer();
		if (player.getGameMode() == GameMode.SPECTATOR) {
			if (e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
				e.setCancelled(true);
				player.setSpectatorTarget(null);
			}
		}
	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			if (activeProjections.containsKey(e.getPlayer().getUniqueId())) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot run commands while Astral Projecting!"));
			}
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onInteract(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onHotbarUse(PlayerItemHeldEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onDisconnect(PlayerQuitEvent e) {
		Player player = e.getPlayer();
		// Ends AstralProjections on the player disconnecting from the server
		if (activeProjections.containsKey(player.getUniqueId())) {
			activeProjections.get(player.getUniqueId()).endAbility();
		}
	}

}
