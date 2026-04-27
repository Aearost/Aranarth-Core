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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles all logic regarding the use of AranarthCore bending abilities.
 */
public class AranarthCoreBendingListener implements Listener {

	public AranarthCoreBendingListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles reloading the abilities when ProjectKorra is reloaded.
	 * @param event The event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPKReload(final BendingReloadEvent event) {
		// End all active projections before PK clears and re-registers abilities,
		// so players are safely returned to their body location.
		AstralProjection.endAllProjections();

		new BukkitRunnable() {
			@Override
			public void run() {
				CoreAbility.registerPluginAbilities(AranarthCore.getInstance(), "com.aearost.aranarthcore.abilities");
				Bukkit.getLogger().info("AranarthCore Bending Reloaded");
			}
		}.runTaskLater(AranarthCore.getInstance(), 1L);
	}

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
						// Guard: do not start a new projection while already projecting
						if (!AstralProjection.isProjecting(player.getUniqueId())) {
							new AstralProjection(e.getPlayer());
						}
					}
				}
			}
			// Waterbending
			else if (ability instanceof WaterAbility && bendingPlayer.isElementToggled(Element.WATER)) {
				if (ability instanceof PlantAbility) {
					if (abilityName.equalsIgnoreCase("vinewhip")) {
						// Guard: only one active instance per player
						if (!VineWhip.hasActiveInstance(e.getPlayer().getUniqueId())) {
							new VineWhip(e.getPlayer());
						}
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
	 * Handles left-click activation for AstralProjection sub-abilities and VineWhip firing.
	 */
	@EventHandler
	public void onLeftClick(PlayerAnimationEvent e) {
		if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) {
			return;
		}
		Player player = e.getPlayer();

		// AstralProjection sub-ability activation (Slot 0 = Aura, 1 = Scream, 2 = Possess)
		if (AstralProjection.isProjecting(player.getUniqueId())) {
			AstralProjection projection = AstralProjection.getActiveProjection(player.getUniqueId());
			switch (player.getInventory().getHeldItemSlot()) {
				case 0 -> projection.activateAura();
				case 1 -> projection.activateScream();
				case 2 -> projection.activatePossess();
			}
			return;
		}

		// VineWhip: left-click while selecting fires the vine
		VineWhip vineWhip = VineWhip.getActiveInstance(player.getUniqueId());
		if (vineWhip != null) {
			vineWhip.onLeftClick();
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
		boolean areAllied = playerDominion != null && dominion.isAllied(playerDominion);
		return !dominion.getMembers().contains(player.getUniqueId()) && !areAllied;
	}

	// Below for VineWhip overrides

	/**
	 * Cancels block breaking while VineWhip is active to prevent the left-click
	 * that fires the vine from also damaging or breaking blocks.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockBreakVineWhip(BlockBreakEvent e) {
		if (VineWhip.hasActiveInstance(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Immediately cancels VineWhip (no retraction animation) when the player
	 * switches to a different ability slot.
	 */
	@EventHandler
	public void onSlotChange(PlayerItemHeldEvent e) {
		VineWhip vineWhip = VineWhip.getActiveInstance(e.getPlayer().getUniqueId());
		if (vineWhip != null) {
			vineWhip.cancelInstantly();
		}
	}

	// Below for AstralProjection overrides

	/**
	 * Prevents the projecting player from dealing any melee or projectile damage.
	 * Their sub-abilities use living.damage() directly and are unaffected by this.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onDamageByProjector(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		Player attacker = null;

		if (damager instanceof Player p) {
			attacker = p;
		} else if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
			attacker = p;
		}

		if (attacker != null && AstralProjection.isProjecting(attacker.getUniqueId())
				&& !AstralProjection.isSubAbilityDamaging(attacker.getUniqueId())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Restores armor from PDC when the player joins, covering the case where the
	 * server crashed while the player was mid-projection (so endAbility never ran).
	 */
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		AstralProjection.restoreArmorFromPdc(e.getPlayer());
	}

	/**
	 * When a mannequin belonging to a projection is damaged, the damage is transferred
	 * to the projecting player and the projection ends.
	 */
	@EventHandler
	public void onMannequinDamage(final EntityDamageEvent e) {
		if (e.getEntityType() != EntityType.MANNEQUIN) {
			return;
		}
		if (e.getCause() == EntityDamageEvent.DamageCause.KILL) {
			return;
		}

		UUID projectorUuid = null;
		for (UUID uuid : AstralProjection.getActiveProjections().keySet()) {
			AstralProjection projection = AstralProjection.getActiveProjection(uuid);
			if (projection.getMannequin() != null && projection.getMannequin().equals(e.getEntity())) {
				projectorUuid = uuid;
				break;
			}
		}

		if (projectorUuid == null) {
			return;
		}

		double damage = e.getFinalDamage();
		e.setDamage(0);

		AstralProjection projection = AstralProjection.getActiveProjection(projectorUuid);
		if (projection != null) {
			projection.endAbilityWithDamage(damage);
		}
	}

	/**
	 * Cancels commands while the player is astral projecting.
	 */
	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {
		if (AstralProjection.isProjecting(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot run commands while Astral Projecting!"));
		}
	}

	/**
	 * Cancels all block interactions while the player is astral projecting or has VineWhip active.
	 * Sub-ability activation uses PlayerAnimationEvent and is unaffected.
	 */
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (AstralProjection.isProjecting(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (VineWhip.hasActiveInstance(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Cancels inventory clicks while the player is astral projecting.
	 */
	@EventHandler
	public void onInteract(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player player && AstralProjection.isProjecting(player.getUniqueId())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Cancels item drops while the player is astral projecting.
	 */
	@EventHandler
	public void onDropItem(PlayerDropItemEvent e) {
		if (AstralProjection.isProjecting(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Ends the projection cleanly if the player dies while projecting.
	 */
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		Player player = e.getPlayer();
		if (AstralProjection.isProjecting(player.getUniqueId())) {
			AstralProjection.getActiveProjection(player.getUniqueId()).endAbility();
		}
	}

	/**
	 * Ends the projection when the player disconnects.
	 */
	@EventHandler
	public void onDisconnect(PlayerQuitEvent e) {
		Player player = e.getPlayer();
		if (AstralProjection.isProjecting(player.getUniqueId())) {
			AstralProjection.getActiveProjection(player.getUniqueId()).endAbility();
		}
	}

}
