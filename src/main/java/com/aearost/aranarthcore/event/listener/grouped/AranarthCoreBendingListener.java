package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.abilities.airbending.soundbending.Amplification;
import com.aearost.aranarthcore.abilities.airbending.spiritual.AstralProjection;
import com.aearost.aranarthcore.abilities.airbending.soundbending.DeafeningScream;
import com.aearost.aranarthcore.abilities.airbending.soundbending.SonicClap;
import com.aearost.aranarthcore.abilities.airbending.soundbending.SonicPulse;
import com.aearost.aranarthcore.abilities.airbending.soundbending.SonicBoom;
import com.aearost.aranarthcore.abilities.airbending.soundbending.SoundAbility;
import com.aearost.aranarthcore.abilities.airbending.spiritual.AstralShot;
import com.aearost.aranarthcore.abilities.earthbending.lavabending.LavaGlaives;
import com.aearost.aranarthcore.abilities.earthbending.sandbending.SandWave;
import com.aearost.aranarthcore.abilities.earthbending.sandbending.Sandstorm;
import com.aearost.aranarthcore.abilities.earthbending.combo.CableSlash;
import com.aearost.aranarthcore.abilities.firebending.combustion.Barrage;
import com.aearost.aranarthcore.abilities.firebending.combustion.CombustionStrike;
import com.aearost.aranarthcore.abilities.firebending.combustion.NoxiousFumes;
import com.aearost.aranarthcore.abilities.airbending.spiritual.AngeredSpirits;
import com.aearost.aranarthcore.abilities.airbending.spiritual.EnergyBurst;
import com.aearost.aranarthcore.abilities.waterbending.plantbending.RazorLeaves;
import com.aearost.aranarthcore.abilities.waterbending.plantbending.Regrowth;
import com.aearost.aranarthcore.abilities.waterbending.plantbending.ToxicSpores;
import com.aearost.aranarthcore.abilities.waterbending.plantbending.VineWhip;
import com.aearost.aranarthcore.abilities.waterbending.combo.IceShards;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.ability.SandAbility;
import com.projectkorra.projectkorra.ability.SpiritualAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
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
		new ArrayList<>(CoreAbility.getAbilities(AstralShot.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(CableSlash.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(IceShards.class)).forEach(CoreAbility::remove);

		new BukkitRunnable() {
			@Override
			public void run() {
				CoreAbility.registerPluginAbilities(AranarthCore.getInstance(), "com.aearost.aranarthcore.abilities");
				Bukkit.getLogger().info("AranarthCore Bending Reloaded");
			}
		}.runTaskLater(AranarthCore.getInstance(), 1L);
	}

	/**
	 * Cancels water flow from IceShards dome blocks, or into the dome radius,
	 * preventing external water from entering and creating infinite sources.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onIceShardsBlockFromTo(final BlockFromToEvent e) {
		if (e.getBlock().getType() != Material.WATER) return;
		if (TempBlock.isTempBlock(e.getBlock())) {
			e.setCancelled(true);
			return;
		}
		final Location toLoc = e.getToBlock().getLocation();
		for (final IceShards inst : IceShards.getActiveInstances()) {
			if (!inst.getPlayer().getWorld().equals(toLoc.getWorld())) continue;
			final Location centre = inst.getPlayer().getLocation().clone().add(0, 1, 0);
			final double r = 12; // domeRadius + 2
			if (toLoc.distanceSquared(centre) <= r * r) {
				e.setCancelled(true);
				return;
			}
		}
	}

	/**
	 * Suppresses fluid physics on IceShards dome water blocks and any water within
	 * the dome radius, preventing flow ticks that bypass BlockFromToEvent.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onIceShardsBlockPhysics(final BlockPhysicsEvent e) {
		if (e.getBlock().getType() != Material.WATER) return;
		if (TempBlock.isTempBlock(e.getBlock())) {
			e.setCancelled(true);
			return;
		}
		final Location loc = e.getBlock().getLocation();
		for (final IceShards inst : IceShards.getActiveInstances()) {
			if (!inst.getPlayer().getWorld().equals(loc.getWorld())) continue;
			final Location centre = inst.getPlayer().getLocation().clone().add(0, 1, 0);
			final double r = 12; // domeRadius + 2
			if (loc.distanceSquared(centre) <= r * r) {
				e.setCancelled(true);
				return;
			}
		}
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
					} else if (abilityName.equalsIgnoreCase("astralshot")) {
						new AstralShot(player);
					} else if (abilityName.equalsIgnoreCase("angeredspirits")) {
						if (!AngeredSpirits.hasActiveInstance(player.getUniqueId())) {
							new AngeredSpirits(player);
						}
					} else if (abilityName.equalsIgnoreCase("energyburst")) {
						if (!EnergyBurst.hasActiveInstance(player.getUniqueId())) {
							new EnergyBurst(player);
						}
					}
				} else if (ability instanceof SoundAbility) {
					if (abilityName.equalsIgnoreCase("sonicboom")) {
						new SonicBoom(player);
					} else if (abilityName.equalsIgnoreCase("amplification")) {
						if (!CoreAbility.hasAbility(player, Amplification.class)) {
							new Amplification(player);
						}
					} else if (abilityName.equalsIgnoreCase("deafeningscream")) {
						if (!CoreAbility.hasAbility(player, DeafeningScream.class)) {
							new DeafeningScream(player);
						}
					} else if (abilityName.equalsIgnoreCase("sonicpulse")) {
						if (!CoreAbility.hasAbility(player, SonicPulse.class)) {
							new SonicPulse(player);
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
					} else if (abilityName.equalsIgnoreCase("razorleaves")) {
						RazorLeaves existing = RazorLeaves.getActiveInstance(player.getUniqueId());
						if (existing == null) {
							new RazorLeaves(player);
						} else {
							// Re-source in SOURCED phase; ignored in CASTING phase
							existing.onSneak();
						}
					} else if (abilityName.equalsIgnoreCase("toxicspores")) {
						if (!ToxicSpores.hasActiveInstance(player.getUniqueId())) {
							new ToxicSpores(player);
						}
					} else if (abilityName.equalsIgnoreCase("regrowth")) {
						if (!Regrowth.hasActiveInstance(player.getUniqueId())) {
							new Regrowth(player);
						}
					}
				}
			}
			// Firebending
			else if (ability instanceof FireAbility && bendingPlayer.isElementToggled(Element.FIRE)) {
				if (abilityName.equalsIgnoreCase("barrage")) {
					if (!Barrage.hasActiveInstance(player.getUniqueId())) {
						new Barrage(player);
					}
				} else if (abilityName.equalsIgnoreCase("combustionstrike")) {
					if (!CombustionStrike.hasActiveInstance(player.getUniqueId())) {
						new CombustionStrike(player);
					}
				} else if (abilityName.equalsIgnoreCase("noxiousfumes")) {
					if (!NoxiousFumes.hasActiveInstance(player.getUniqueId())) {
						new NoxiousFumes(player);
					}
				}
			}
			// Earthbending
			else if (ability instanceof EarthAbility && bendingPlayer.isElementToggled(Element.EARTH)) {
				if (ability instanceof SandAbility) {
					if (abilityName.equalsIgnoreCase("sandstorm")) {
						Sandstorm sandstorm = Sandstorm.getActiveInstance(player.getUniqueId());
						if (sandstorm != null) {
							sandstorm.startCasting();
						}
					} else if (abilityName.equalsIgnoreCase("sandwave")) {
						if (!SandWave.hasActiveInstance(player.getUniqueId())) {
							org.bukkit.block.Block target = player.getTargetBlock(null, 5);
							if (target != null && EarthAbility.isSandbendable(player, target.getType())) {
								SandWave.setPendingSource(player.getUniqueId(), target);
							}
						}
					}
				} else if (ability instanceof LavaAbility) {
					if (abilityName.equalsIgnoreCase("lavaglaives")) {
						if (!LavaGlaives.hasActiveInstance(player.getUniqueId())) {
							new LavaGlaives(player);
						}
					}
				} else if (abilityName.equalsIgnoreCase("earthtunnel") || abilityName.equalsIgnoreCase("collapse")) {
					e.setCancelled(AranarthBendingUtils.preventAbilityNearDominion(player));
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
			return;
		}

		// RazorLeaves: left-click fires a leaf in the player's current look direction
		RazorLeaves razorLeaves = RazorLeaves.getActiveInstance(player.getUniqueId());
		if (razorLeaves != null) {
			razorLeaves.onLeftClick();
			return;
		}

		// NoxiousFumes: left-click begins channeling from the READY state
		NoxiousFumes noxiousFumes = NoxiousFumes.getActiveInstance(player.getUniqueId());
		if (noxiousFumes != null) {
			noxiousFumes.startChanneling();
			return;
		}

		// ToxicSpores: left-click begins channeling from the READY state
		ToxicSpores toxicSpores = ToxicSpores.getActiveInstance(player.getUniqueId());
		if (toxicSpores != null) {
			toxicSpores.startChanneling();
			return;
		}

		// IceShards: left-click fires the charged shards
		IceShards iceShards = IceShards.getActiveInstance(player.getUniqueId());
		if (iceShards != null) {
			iceShards.fire();
			return;
		}

		// AngeredSpirits: left-click fires the next spirit during the firing window
		AngeredSpirits angeredSpirits = AngeredSpirits.getActiveInstance(player.getUniqueId());
		if (angeredSpirits != null) {
			angeredSpirits.onLeftClick();
			return;
		}

		// LavaGlaives: left-click fires the next glaive (right first, then left)
		LavaGlaives lavaGlaives = LavaGlaives.getActiveInstance(player.getUniqueId());
		if (lavaGlaives != null) {
			lavaGlaives.onLeftClick();
			return;
		}

		// SonicClap: instant left-click fire
		BendingPlayer bpClap = BendingPlayer.getBendingPlayer(player);
		if (bpClap != null && bpClap.getBoundAbilityName().equalsIgnoreCase("sonicclap")
				&& bpClap.isElementToggled(Element.AIR)) {
			new SonicClap(player);
			return;
		}

		// SandWave: left-click launches the wave from a previously sneaked source
		if (SandWave.hasPendingSource(player.getUniqueId())) {
			BendingPlayer bp = BendingPlayer.getBendingPlayer(player);
			if (bp != null && bp.getBoundAbilityName().equalsIgnoreCase("sandwave")
					&& !SandWave.hasActiveInstance(player.getUniqueId())) {
				org.bukkit.block.Block sourceBlock = SandWave.getPendingSource(player.getUniqueId());
				SandWave.clearPendingSource(player.getUniqueId());
				new SandWave(player, sourceBlock);
			}
		}
	}

	/**
	 * Cancels block breaking while an ability is active.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockBreakVineWhip(BlockBreakEvent e) {
		Player player = e.getPlayer();
		if (VineWhip.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (RazorLeaves.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (Sandstorm.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (IceShards.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (Barrage.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (CombustionStrike.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (NoxiousFumes.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (ToxicSpores.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (Regrowth.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (AngeredSpirits.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		if (EnergyBurst.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		if (SandWave.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		if (LavaGlaives.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Immediately cancels an ability when the player switches to a different ability slot.
	 */
	@EventHandler
	public void onSlotChange(PlayerItemHeldEvent e) {
		VineWhip vineWhip = VineWhip.getActiveInstance(e.getPlayer().getUniqueId());
		if (vineWhip != null) {
			vineWhip.cancelInstantly();
		}
		RazorLeaves razorLeaves = RazorLeaves.getActiveInstance(e.getPlayer().getUniqueId());
		if (razorLeaves != null) {
			razorLeaves.endWithCooldown();
		}
		Sandstorm sandstorm = Sandstorm.getActiveInstance(e.getPlayer().getUniqueId());
		if (sandstorm != null) {
			sandstorm.cancelFromSlotChange();
		}
		// IceShards slot-change cancel is handled inside progress(), but we also
		// call remove() here for immediate cleanup without waiting a tick.
		IceShards iceShards = IceShards.getActiveInstance(e.getPlayer().getUniqueId());
		if (iceShards != null) {
			iceShards.remove();
		}
		NoxiousFumes noxiousFumes = NoxiousFumes.getActiveInstance(e.getPlayer().getUniqueId());
		if (noxiousFumes != null) {
			noxiousFumes.endChanneling();
		}
		ToxicSpores toxicSpores = ToxicSpores.getActiveInstance(e.getPlayer().getUniqueId());
		if (toxicSpores != null) {
			toxicSpores.endChanneling();
		}
		Regrowth regrowth = Regrowth.getActiveInstance(e.getPlayer().getUniqueId());
		if (regrowth != null) {
			regrowth.remove();
		}
		AngeredSpirits angeredSpirits = AngeredSpirits.getActiveInstance(e.getPlayer().getUniqueId());
		if (angeredSpirits != null) {
			angeredSpirits.onSlotChange();
		}
		EnergyBurst energyBurst = EnergyBurst.getActiveInstance(e.getPlayer().getUniqueId());
		if (energyBurst != null) {
			energyBurst.onSlotChange();
		}
		// Slot change before the strike is fired cancels without cooldown; once TRAVELING it continues on its own
		CombustionStrike combustionStrike = CombustionStrike.getActiveInstance(e.getPlayer().getUniqueId());
		if (combustionStrike != null && combustionStrike.getPhase() != CombustionStrike.Phase.TRAVELING) {
			combustionStrike.cancelInstantly();
		}
		// Slot change while charging cancels without cooldown; slot change while READY or FLYING applies cooldown
		LavaGlaives lavaGlaives = LavaGlaives.getActiveInstance(e.getPlayer().getUniqueId());
		if (lavaGlaives != null) {
			if (lavaGlaives.getPhase() == LavaGlaives.Phase.CHARGING) {
				lavaGlaives.cancelInstantly();
			} else {
				lavaGlaives.endWithCooldown();
			}
		}
		SandWave.clearPendingSource(e.getPlayer().getUniqueId());
	}

	/**
	 * Locks XYZ for any ability that roots the player in place.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerMoveRooted(PlayerMoveEvent e) {
		Player player = e.getPlayer();
		UUID uuid = player.getUniqueId();

		Sandstorm sandstorm = Sandstorm.getActiveInstance(uuid);
		IceShards iceShards = IceShards.getActiveInstance(uuid);
		boolean rooted = (sandstorm != null && sandstorm.isCasting())
				|| (iceShards != null && iceShards.isCharging());

		if (!rooted) return;
		lockXYZ(e);
	}

	/** Snaps the player's XYZ back to the event's from-location while preserving head rotation. */
	private static void lockXYZ(PlayerMoveEvent e) {
		Location to = e.getTo();
		if (to == null) return;
		Location from = e.getFrom();
		if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) return;
		Location locked = from.clone();
		locked.setYaw(to.getYaw());
		locked.setPitch(to.getPitch());
		e.setTo(locked);
	}


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
	 * Also handles Sandstorm source-block selection on left-click.
	 * Sub-ability activation uses PlayerAnimationEvent and is unaffected.
	 */
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();

		if (AstralProjection.isProjecting(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (VineWhip.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (RazorLeaves.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (ToxicSpores.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (Regrowth.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (LavaGlaives.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}

		// Sandstorm: left-clicking a sandbendable block selects the source
		if (e.getAction() == Action.LEFT_CLICK_BLOCK && e.getClickedBlock() != null) {
			BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
			if (bendingPlayer != null
					&& bendingPlayer.getBoundAbilityName().equalsIgnoreCase("sandstorm")
					&& EarthAbility.isSandbendable(player, e.getClickedBlock().getType())
					&& !Sandstorm.hasActiveInstance(player.getUniqueId())) {
				new Sandstorm(player, e.getClickedBlock());
			}
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
		SandWave.clearPendingSource(player.getUniqueId());
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
		SandWave.clearPendingSource(player.getUniqueId());
	}

	/**
	 * Applies the Amplification damage multiplier to all hits from the SoundAbility that consumed the buff.
	 * The multiplier is locked in on first contact and reused for every subsequent hit from the same cast.
	 */
	@EventHandler
	public void onAmplificationDamage(AbilityDamageEntityEvent e) {
		Ability ability = e.getAbility();
		if (!(ability instanceof SoundAbility)) return;
		if (ability instanceof Amplification) return;

		Player p = ability.getPlayer();
		if (p == null) return;

		Amplification amp = Amplification.getActiveAmplification(p);
		if (amp == null) return;

		amp.applyMultiplier(e);
	}

}
