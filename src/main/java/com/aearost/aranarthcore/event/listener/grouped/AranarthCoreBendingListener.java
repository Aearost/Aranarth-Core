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
import com.aearost.aranarthcore.abilities.earthbending.lavabending.Eruption;
import com.aearost.aranarthcore.abilities.earthbending.lavabending.MagmaGlaives;
import com.aearost.aranarthcore.abilities.earthbending.lavabending.MagmaWave;
import com.aearost.aranarthcore.abilities.chiblocking.DaggerVolley;
import com.aearost.aranarthcore.abilities.chiblocking.HighJump;
import com.aearost.aranarthcore.abilities.earthbending.metalbending.CableWhip;
import com.aearost.aranarthcore.abilities.earthbending.metalbending.CableThrash;
import com.aearost.aranarthcore.abilities.earthbending.metalbending.MetalBlade;
import com.aearost.aranarthcore.abilities.earthbending.metalbending.MetalShots;
import com.aearost.aranarthcore.abilities.earthbending.metalbending.MetalStrips;
import com.aearost.aranarthcore.abilities.earthbending.sandbending.Burial;
import com.aearost.aranarthcore.abilities.earthbending.sandbending.SandWave;
import com.aearost.aranarthcore.abilities.earthbending.sandbending.Sandstorm;
import com.aearost.aranarthcore.abilities.earthbending.combo.CableSlash;
import com.aearost.aranarthcore.abilities.firebending.combustion.Barrage;
import com.aearost.aranarthcore.abilities.firebending.combustion.CombustionStrike;
import com.aearost.aranarthcore.abilities.firebending.combustion.JetFumes;
import com.aearost.aranarthcore.abilities.firebending.combustion.NoxiousFumes;
import com.aearost.aranarthcore.abilities.airbending.spiritual.AngeredSpirits;
import com.aearost.aranarthcore.abilities.airbending.spiritual.EnergyBurst;
import com.aearost.aranarthcore.abilities.waterbending.bloodbending.BloodFreeze;
import com.aearost.aranarthcore.abilities.waterbending.bloodbending.BloodGrip;
import com.aearost.aranarthcore.abilities.waterbending.bloodbending.Disalignment;
import com.projectkorra.projectkorra.ability.BloodAbility;
import com.aearost.aranarthcore.abilities.waterbending.plantbending.RazorLeaves;
import com.aearost.aranarthcore.abilities.waterbending.plantbending.Regrowth;
import com.aearost.aranarthcore.abilities.waterbending.plantbending.ToxicSpores;
import com.aearost.aranarthcore.abilities.waterbending.plantbending.VineWhip;
import com.aearost.aranarthcore.abilities.waterbending.combo.IceDiscs;
import com.aearost.aranarthcore.abilities.waterbending.combo.IceShards;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
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
import com.projectkorra.projectkorra.event.PlayerCooldownChangeEvent;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
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
		new ArrayList<>(CoreAbility.getAbilities(CableWhip.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(CableThrash.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(MetalBlade.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(MetalShots.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(IceDiscs.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(IceShards.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(JetFumes.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(DaggerVolley.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(BloodGrip.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(BloodFreeze.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(Disalignment.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(Eruption.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(MagmaWave.class)).forEach(CoreAbility::remove);
		new ArrayList<>(CoreAbility.getAbilities(Burial.class)).forEach(CoreAbility::remove);

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

		// Sneak press during FIRING — cancel so PK cannot activate IceSpike via SHIFT_DOWN
		if (e.isSneaking()) {
			IceDiscs iceDiscs = IceDiscs.getActiveInstance(player.getUniqueId());
			if (iceDiscs != null && iceDiscs.getPhase() == IceDiscs.Phase.FIRING) {
				e.setCancelled(true);
				return;
			}
		}

		// Sneak release — cancel IceDiscs immediately rather than waiting for the next progress() tick
		if (!e.isSneaking()) {
			IceDiscs iceDiscs = IceDiscs.getActiveInstance(player.getUniqueId());
			if (iceDiscs != null && iceDiscs.getPhase() == IceDiscs.Phase.CHARGING) {
				iceDiscs.cancelWithCooldown();
				return;
			}
		}

		if (e.isSneaking() && player.getGameMode() == GameMode.SURVIVAL) {
			if (BloodGrip.isControlled(player.getUniqueId())) {
				e.setCancelled(true);
				return;
			}
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
				if (ability instanceof BloodAbility) {
					if (abilityName.equalsIgnoreCase("bloodgrip")) {
						if (!BloodGrip.hasActiveInstance(player.getUniqueId())) {
							new BloodGrip(player);
						}
					} else if (abilityName.equalsIgnoreCase("bloodfreeze")) {
						if (!BloodFreeze.hasActiveInstance(player.getUniqueId())) {
							new BloodFreeze(player);
						}
					} else if (abilityName.equalsIgnoreCase("disalignment")) {
						if (!Disalignment.hasActiveInstance(player.getUniqueId())) {
							new Disalignment(player);
						}
					}
				} else if (ability instanceof PlantAbility) {
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
					if (abilityName.equalsIgnoreCase("magmaglaives")) {
						if (!MagmaGlaives.hasActiveInstance(player.getUniqueId())) {
							new MagmaGlaives(player);
						}
					} else if (abilityName.equalsIgnoreCase("eruption")) {
						if (!Eruption.hasActiveInstance(player.getUniqueId())) {
							new Eruption(player);
						}
					} else if (abilityName.equalsIgnoreCase("magmawave")) {
						// Tap sneak selects a nearby lava source; left-click fires the wave.
						if (!MagmaWave.hasActiveInstance(player.getUniqueId())
								&& !bendingPlayer.isOnCooldown("MagmaWave")) {
							MagmaWave.trySelectSource(player);
						}
					}
				} else if (abilityName.equalsIgnoreCase("cablewhip")) {
					if (!CableWhip.hasActiveInstance(player.getUniqueId())) {
						new CableWhip(player);
					}
				} else if (abilityName.equalsIgnoreCase("metalstrips")) {
					MetalStrips.startRecall(player);
				} else if (abilityName.equalsIgnoreCase("metalshots")) {
					MetalShots existing = MetalShots.getActiveInstance(player.getUniqueId());
					if (existing != null) {
						existing.onSneak();
					} else {
						new MetalShots(player);
					}
				} else if (abilityName.equalsIgnoreCase("metalblade")) {
					if (!MetalBlade.hasActiveInstance(player.getUniqueId())) {
						new MetalBlade(player);
					}
				} else if (abilityName.equalsIgnoreCase("cablethrash")) {
					if (!CableThrash.hasActiveInstance(player.getUniqueId())) {
						new CableThrash(player);
					}
				} else if (abilityName.equalsIgnoreCase("earthtunnel") || abilityName.equalsIgnoreCase("collapse")) {
					e.setCancelled(AranarthBendingUtils.preventAbilityNearDominion(player));
				}
			}
			// Chiblocking
			else if (ability instanceof ChiAbility) {
				if (abilityName.equalsIgnoreCase("highjump") && !HighJump.hasActiveInstance(player.getUniqueId())) {
					boolean isOnGround = player.getLocation().getBlock()
							.getRelative(BlockFace.DOWN).getType().isSolid();
					if (isOnGround) {
						new HighJump(player, HighJump.JumpType.EVADE);
					} else {
						new HighJump(player, HighJump.JumpType.DOUBLEJUMP);
					}
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

		if (BloodGrip.isControlled(player.getUniqueId())) {
			return;
		}

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

		// BloodGrip: left-click while controlling flings the target in the look direction
		BloodGrip bloodGrip = BloodGrip.getActiveInstance(player.getUniqueId());
		if (bloodGrip != null) {
			bloodGrip.onLeftClick();
			return;
		}

		// Disalignment: left-click while charged applies the disalignment to the target
		Disalignment disalignment = Disalignment.getActiveInstance(player.getUniqueId());
		if (disalignment != null) {
			disalignment.onLeftClick();
			return;
		}

		// CableWhip: left-click while ready cracks one whip in the current look direction
		CableWhip cableWhip = CableWhip.getActiveInstance(player.getUniqueId());
		if (cableWhip != null) {
			cableWhip.onLeftClick();
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

		// JetFumes: left-click cancels active flight
		JetFumes jetFumes = JetFumes.getActiveInstance(player.getUniqueId());
		if (jetFumes != null) {
			jetFumes.onLeftClick();
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

		// IceDiscs: left-click fires the next disc from the pillar
		IceDiscs iceDiscs = IceDiscs.getActiveInstance(player.getUniqueId());
		if (iceDiscs != null) {
			iceDiscs.fireDisc();
			return;
		}

		// AngeredSpirits: left-click fires the next spirit during the firing window
		AngeredSpirits angeredSpirits = AngeredSpirits.getActiveInstance(player.getUniqueId());
		if (angeredSpirits != null) {
			angeredSpirits.onLeftClick();
			return;
		}

		// Eruption: left-click during SOURCING confirms the target and begins the warm-up.
		Eruption eruption = Eruption.getActiveInstance(player.getUniqueId());
		if (eruption != null) {
			eruption.onLeftClick();
			return;
		}

		// MagmaWave: left-click fires the wave from the previously-tapped lava source.
		if (MagmaWave.hasPendingSource(player.getUniqueId())
				&& !MagmaWave.hasActiveInstance(player.getUniqueId())) {
			BendingPlayer bpMagmaWave = BendingPlayer.getBendingPlayer(player);
			if (bpMagmaWave != null && bpMagmaWave.getBoundAbilityName().equalsIgnoreCase("magmawave")) {
				new MagmaWave(player);
				return;
			}
		}

		// MagmaGlaives: left-click fires the next glaive (right first, then left)
		MagmaGlaives magmaGlaives = MagmaGlaives.getActiveInstance(player.getUniqueId());
		if (magmaGlaives != null) {
			magmaGlaives.onLeftClick();
			return;
		}

		// MetalShots: left-click fires a projectile from the first available floating source block
		MetalShots metalShots = MetalShots.getActiveInstance(player.getUniqueId());
		if (metalShots != null) {
			metalShots.onLeftClick();
			return;
		}

		// HighJump: left-click to jump straight up (or lunge forward while sprinting/in water)
		BendingPlayer bpChi = BendingPlayer.getBendingPlayer(player);
		if (bpChi != null && bpChi.getBoundAbilityName().equalsIgnoreCase("highjump")
				&& !HighJump.hasActiveInstance(player.getUniqueId())) {
			if (player.isSprinting() || player.isInWater()) {
				new HighJump(player, HighJump.JumpType.LUNGE);
			} else {
				new HighJump(player, HighJump.JumpType.JUMP);
			}
			return;
		}

		// DaggerVolley: instant left-click fire — advances the 3 → 6 → 9 arrow cycle
		BendingPlayer bpDaggerVolley = BendingPlayer.getBendingPlayer(player);
		if (bpDaggerVolley != null && bpDaggerVolley.getBoundAbilityName().equalsIgnoreCase("daggervolley")) {
			new DaggerVolley(player);
			return;
		}

		// MetalStrips: instant left-click fire — one iron ingot per shot
		BendingPlayer bpMetal = BendingPlayer.getBendingPlayer(player);
		if (bpMetal != null && bpMetal.getBoundAbilityName().equalsIgnoreCase("metalstrips")
				&& bpMetal.isElementToggled(Element.EARTH)) {
			MetalStrips.markLeftClick(player.getUniqueId());
			new MetalStrips(player);
			return;
		}

		// SonicClap: instant left-click fire
		BendingPlayer bpClap = BendingPlayer.getBendingPlayer(player);
		if (bpClap != null && bpClap.getBoundAbilityName().equalsIgnoreCase("sonicclap")
				&& bpClap.isElementToggled(Element.AIR)) {
			new SonicClap(player);
			return;
		}

		// Burial: left-click while sneaking fires the crevice toward the target
		BendingPlayer bpBurial = BendingPlayer.getBendingPlayer(player);
		if (bpBurial != null && bpBurial.getBoundAbilityName().equalsIgnoreCase("burial")
				&& bpBurial.isElementToggled(Element.EARTH)
				&& player.isSneaking()
				&& !Burial.hasActiveInstance(player.getUniqueId())) {
			new Burial(player);
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
	public void onBlockBreak(BlockBreakEvent e) {
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
		if (IceDiscs.hasActiveInstance(player.getUniqueId())) {
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
		if (JetFumes.hasActiveInstance(player.getUniqueId())) {
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
		if (Burial.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		if (MagmaGlaives.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		if (Eruption.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		if (MagmaWave.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		if (CableWhip.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		if (MetalShots.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		if (MetalBlade.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		if (CableThrash.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		if (BloodGrip.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		if (BloodFreeze.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
		}
		Disalignment disalignment = Disalignment.getActiveInstance(player.getUniqueId());
		if (disalignment != null && disalignment.getPhase() != Disalignment.Phase.DISALIGNING) {
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
		IceDiscs iceDiscs = IceDiscs.getActiveInstance(e.getPlayer().getUniqueId());
		if (iceDiscs != null) {
			iceDiscs.cancelWithCooldown();
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
		MagmaGlaives magmaGlaives = MagmaGlaives.getActiveInstance(e.getPlayer().getUniqueId());
		if (magmaGlaives != null) {
			if (magmaGlaives.getPhase() == MagmaGlaives.Phase.CHARGING) {
				magmaGlaives.cancelInstantly();
			} else {
				magmaGlaives.endWithCooldown();
			}
		}
		// Slot change during SOURCING cancels without cooldown; once warm-up begins the cooldown applies.
		Eruption eruption = Eruption.getActiveInstance(e.getPlayer().getUniqueId());
		if (eruption != null) {
			if (eruption.getPhase() == Eruption.Phase.SOURCING) {
				eruption.cancelInstantly();
			} else {
				eruption.endWithCooldown();
			}
		}
		// Slot change clears any pending source and ends an active wave with cooldown.
		MagmaWave.clearPendingSource(e.getPlayer().getUniqueId());
		MagmaWave magmaWave = MagmaWave.getActiveInstance(e.getPlayer().getUniqueId());
		if (magmaWave != null) {
			magmaWave.endWithCooldown();
		}

		// Slot change during charge or with no whips fired - no cooldown, otherwise apply it.
		CableWhip cableWhip = CableWhip.getActiveInstance(e.getPlayer().getUniqueId());
		if (cableWhip != null) {
			if (cableWhip.getWhipsDone() > 0) {
				cableWhip.endWithCooldown();
			} else {
				cableWhip.cancelInstantly();
			}
		}
		MetalShots metalShots = MetalShots.getActiveInstance(e.getPlayer().getUniqueId());
		if (metalShots != null) {
			metalShots.endWithCooldown();
		}
		// Slot change during charge cancels without cooldown; slot change while READY applies cooldown
		MetalBlade metalBlade = MetalBlade.getActiveInstance(e.getPlayer().getUniqueId());
		if (metalBlade != null) {
			if (metalBlade.getPhase() == MetalBlade.Phase.CHARGING) {
				metalBlade.cancelInstantly();
			} else {
				metalBlade.endWithCooldown();
			}
		}
		CableThrash cableThrash = CableThrash.getActiveInstance(e.getPlayer().getUniqueId());
		if (cableThrash != null) {
			cableThrash.endWithCooldown();
		}
		BloodGrip bloodGrip = BloodGrip.getActiveInstance(e.getPlayer().getUniqueId());
		if (bloodGrip != null) {
			bloodGrip.endWithCooldown();
		}
		// Slot change during charging cancels without cooldown; slot change while casting applies cooldown.
		BloodFreeze bloodFreeze = BloodFreeze.getActiveInstance(e.getPlayer().getUniqueId());
		if (bloodFreeze != null) {
			if (bloodFreeze.getPhase() == BloodFreeze.Phase.CHARGING) {
				bloodFreeze.remove();
			} else {
				bloodFreeze.endWithCooldown();
			}
		}
		// Slot change during charging or charged cancels without cooldown; disalignment in progress continues uninterrupted.
		Disalignment disalignment = Disalignment.getActiveInstance(e.getPlayer().getUniqueId());
		if (disalignment != null && disalignment.getPhase() != Disalignment.Phase.DISALIGNING) {
			disalignment.remove();
		}
		SandWave.clearPendingSource(e.getPlayer().getUniqueId());

		// Slot change during CASTING cancels without cooldown; later phases run to completion.
		Burial burial = Burial.getActiveInstance(e.getPlayer().getUniqueId());
		if (burial != null && burial.getPhase() == Burial.Phase.CASTING) {
			burial.cancelInstantly();
		}
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
	 * Blocks FireJet from receiving a cooldown while the player is in JetFumes FLYING phase.
	 * Every PK addCooldown() call fires this cancellable event, so this intercepts all sources
	 * (combo manager, CoreAbility.remove(), etc.) without any timing fragility.
	 * endFlight() switches phase to DISPERSING before applying the real cooldown, so that
	 * call is unaffected. remove() clears ACTIVE_INSTANCES before applying cooldowns, so
	 * death/disconnect edge cases are also unaffected.
	 */
	@EventHandler
	public void onFireJetCooldownDuringJetFumes(PlayerCooldownChangeEvent e) {
		if (!e.getAbility().equalsIgnoreCase("FireJet")) return;
		if (e.getResult() != PlayerCooldownChangeEvent.Result.ADDED) return;
		if (!e.isOnline()) return;
		Player player = (Player) e.getPlayer();
		JetFumes jf = JetFumes.getActiveInstance(player.getUniqueId());
		if (jf == null || jf.getPhase() != JetFumes.Phase.FLYING) return;
		e.setCancelled(true);
	}

	/**
	 * Ends an active BloodGrip immediately when the caster takes any damage while casting
	 * or controlling, applying the cooldown and freeing the controlled target.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBloodGripCasterDamaged(final EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player player)) return;
		BloodGrip bloodGrip = BloodGrip.getActiveInstance(player.getUniqueId());
		if (bloodGrip != null) {
			bloodGrip.cancelFromDamage();
		}
	}

	/**
	 * Ends an active BloodFreeze immediately when the caster takes any damage while casting,
	 * applying the cooldown and releasing the frozen target.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBloodFreezeCasterDamaged(final EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player player)) return;
		BloodFreeze bloodFreeze = BloodFreeze.getActiveInstance(player.getUniqueId());
		if (bloodFreeze != null) {
			bloodFreeze.cancelFromDamage();
		}
	}

	/**
	 * Suppresses vanilla suffocation damage for entities currently buried by Burial.
	 * Manual ticking damage is applied by the ability itself instead.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBurialSuffocation(final EntityDamageEvent e) {
		if (e.getCause() != EntityDamageEvent.DamageCause.SUFFOCATION) {
			return;
		}
		if (Burial.isBuried(e.getEntity().getUniqueId())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Cancels a Disalignment charge or charged state when the caster takes damage. Has no effect
	 * once the disalignment has been applied, as the effect persists independently of the caster.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onDisalignmentCasterDamaged(final EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player player)) return;
		Disalignment disalignment = Disalignment.getActiveInstance(player.getUniqueId());
		if (disalignment != null) {
			disalignment.cancelFromDamage();
		}
	}

	/**
	 * Suppresses lava and fire damage for the Eruption caster throughout the ability,
	 * and for enemy entities that are still within the geyser's ground footprint while
	 * the column is actively rising. Once entities are airborne and the column is gone
	 * the suppression ends, so natural lava damage from other sources is unaffected.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEruptionLavaDamage(EntityDamageEvent e) {
		EntityDamageEvent.DamageCause cause = e.getCause();
		if (cause != EntityDamageEvent.DamageCause.LAVA
				&& cause != EntityDamageEvent.DamageCause.FIRE
				&& cause != EntityDamageEvent.DamageCause.FIRE_TICK
				&& cause != EntityDamageEvent.DamageCause.HOT_FLOOR) {
			return;
		}
		if (!(e.getEntity() instanceof LivingEntity entity)) {
			return;
		}
		for (Eruption eruption : Eruption.getActiveInstances().values()) {
			if (eruption.isLavaProtected(entity)) {
				e.setCancelled(true);
				return;
			}
		}
	}

	/**
	 * Cancels fire and lava damage to the JetFumes caster while they are in flight.
	 * The ability starts near a fire/lava source, so without this the player would
	 * immediately take environmental fire damage the moment the ability activates.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onJetFumesFireDamage(final EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player p)) return;
		JetFumes jf = JetFumes.getActiveInstance(p.getUniqueId());
		if (jf == null || jf.getPhase() != JetFumes.Phase.FLYING) return;
		EntityDamageEvent.DamageCause cause = e.getCause();
		if (cause == EntityDamageEvent.DamageCause.FIRE
				|| cause == EntityDamageEvent.DamageCause.FIRE_TICK
				|| cause == EntityDamageEvent.DamageCause.LAVA
				|| cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
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
		if (BloodGrip.isControlled(player.getUniqueId())) {
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
		if (MagmaGlaives.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (Eruption.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (MagmaWave.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (CableWhip.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (BloodGrip.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (BloodFreeze.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		Disalignment disalignmentInteract = Disalignment.getActiveInstance(player.getUniqueId());
		if (disalignmentInteract != null && disalignmentInteract.getPhase() != Disalignment.Phase.DISALIGNING) {
			e.setCancelled(true);
			return;
		}
		if (IceDiscs.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (MetalShots.hasActiveInstance(player.getUniqueId())) {
			e.setCancelled(true);
			return;
		}
		if (Burial.hasActiveInstance(player.getUniqueId())) {
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
		MagmaWave.clearPendingSource(player.getUniqueId());
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
		MagmaWave.clearPendingSource(player.getUniqueId());
		DaggerVolley.resetStage(player.getUniqueId());
	}

	/**
	 * Enforces owner-only pickup for iron ingots fired by MetalStrips. Any player other than the
	 * caster who tagged the item is prevented from collecting it. When the owner steps on their
	 * own ingot, it is removed from the MetalStrips tracking map so it is no longer subject to recall.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPickupMetalStrip(final EntityPickupItemEvent e) {
		if (!(e.getEntity() instanceof Player player)) return;

		final String ownerUuid = e.getItem().getPersistentDataContainer().get(
				MetalStrips.getStripOwnerKey(), PersistentDataType.STRING);
		if (ownerUuid == null) return;

		if (!player.getUniqueId().toString().equals(ownerUuid)) {
			e.setCancelled(true);
			return;
		}

		// Owner is collecting their ingot — swap the tagged stack for a clean iron ingot before
		// the pickup completes so the player never receives the internal instance-ID metadata.
		e.getItem().setItemStack(new ItemStack(Material.IRON_INGOT, 1));
		MetalStrips.removeTrackedItem(e.getItem(), player.getUniqueId());
	}

	private static final double[] RANK_DAMAGE_MULTIPLIERS = { 1.0, 1.05, 1.15, 1.3, 1.5, 1.75, 2.0, 2.25, 2.5 };

	/**
	 * Scales bending damage based on the caster's rank.
	 * Peasant (rank 0) deals 1x damage, scaling up to 2.5x at Emperor (rank 8).
	 */
	@EventHandler
	public void onRankBendingDamage(AbilityDamageEntityEvent e) {
		Player p = e.getAbility().getPlayer();
		if (p == null) return;

		AranarthPlayer ap = AranarthUtils.getAranarthPlayers().get(p.getUniqueId());
		if (ap == null) return;

		int rank = ap.getRank();
		if (rank <= 0) return;

		double multiplier = RANK_DAMAGE_MULTIPLIERS[Math.min(rank, RANK_DAMAGE_MULTIPLIERS.length - 1)];
		e.setDamage(e.getDamage() * multiplier);
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

	/**
	 * Cancels vanilla arrow damage for DaggerVolley projectiles and replaces it with the
	 * per-arrow damage roll so that region protection and rank scaling are applied through
	 * ProjectKorra's standard pipeline.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onDaggerVolleyArrowHit(final EntityDamageByEntityEvent e) {
		if (!(e.getDamager() instanceof Arrow arrow)) return;
		if (!arrow.hasMetadata("daggervolley")) return;
		if (!(e.getEntity() instanceof LivingEntity target)) return;
		if (!(arrow.getShooter() instanceof Player shooter)) return;

		e.setCancelled(true);

		final DaggerVolley dv = CoreAbility.getAbility(shooter, DaggerVolley.class);
		if (dv == null) {
			// Ability already ended (all other arrows resolved); discard without dealing damage.
			arrow.remove();
			return;
		}
		dv.damageEntityFromArrow(target, arrow);
	}

	/**
	 * Suppresses lava and fire damage for the MagmaWave caster throughout the ability,
	 * and for enemy entities that remain within the active wave's footprint.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onMagmaWaveLavaDamage(EntityDamageEvent e) {
		EntityDamageEvent.DamageCause cause = e.getCause();
		if (cause != EntityDamageEvent.DamageCause.LAVA
				&& cause != EntityDamageEvent.DamageCause.FIRE
				&& cause != EntityDamageEvent.DamageCause.FIRE_TICK
				&& cause != EntityDamageEvent.DamageCause.HOT_FLOOR) {
			return;
		}
		if (!(e.getEntity() instanceof LivingEntity entity)) {
			return;
		}
		for (MagmaWave wave : MagmaWave.getActiveInstances().values()) {
			if (wave.isLavaProtected(entity)) {
				e.setCancelled(true);
				return;
			}
		}
	}

	/**
	 * Prevents wave lava blocks from flowing to adjacent blocks.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onMagmaWaveBlockFromTo(final BlockFromToEvent e) {
		if (e.getBlock().getType() != Material.LAVA) {
			return;
		}
		if (MagmaWave.isWaveBlock(e.getBlock())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Suppresses physics updates on wave lava blocks so their placed levels remain stable.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onMagmaWaveBlockPhysics(final BlockPhysicsEvent e) {
		if (e.getBlock().getType() != Material.LAVA) {
			return;
		}
		if (MagmaWave.isWaveBlock(e.getBlock())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Applies MetalBlade's melee buff when the player punches a living entity while the blade is
	 * ready. The vanilla damage is replaced entirely by the ability's damage so that region
	 * protection and rank scaling are applied through ProjectKorra's standard pipeline.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onMetalBladeMeleeHit(final EntityDamageByEntityEvent e) {
		if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
			return;
		}
		if (!(e.getDamager() instanceof Player player)) {
			return;
		}
		if (!(e.getEntity() instanceof LivingEntity target)) {
			return;
		}

		final MetalBlade metalBlade = MetalBlade.getActiveInstance(player.getUniqueId());
		if (metalBlade == null || metalBlade.getPhase() != MetalBlade.Phase.READY) {
			return;
		}

		metalBlade.onMeleeHit(target, e);
	}

}
