package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Handles the effects of the Sonar, Grapple, Homing, Smoke, Shifting, Tether and Tide arrows.
 * Grapple and Tether ropes are pulled by sneaking while holding a bow, and snap if the shooter takes damage.
 */
public class SpecialArrowUtils {

	// Sonar
	private static final double SONAR_RADIUS = 16;
	private static final int SONAR_DURATION_TICKS = 100;

	// Grapple
	private static final double GRAPPLE_MAX_DISTANCE = 48;
	private static final double GRAPPLE_SPEED = 1.2;
	private static final double GRAPPLE_SLOW_DISTANCE = 3;
	private static final double GRAPPLE_SLOW_SPEED = 0.35;
	private static final double GRAPPLE_ARRIVE_DISTANCE = 1.5;
	private static final int GRAPPLE_STALL_TICKS = 3;
	private static final double GRAPPLE_END_BOOST = 1;
	private static final int GRAPPLE_MAX_REEL_TICKS = 100;

	// Homing
	private static final double HOMING_RANGE = 10;
	private static final double HOMING_CONE_RADIANS = Math.toRadians(30);
	private static final double HOMING_CLOSE_CONE_RADIANS = Math.toRadians(50);
	private static final double HOMING_STEER = 0.2;
	private static final double HOMING_CLOSE_STEER = 0.7;
	private static final int HOMING_MAX_TICKS = 100;

	// Smoke
	private static final double SMOKE_RADIUS = 6;
	private static final double SMOKE_EFFECT_RADIUS = 7;
	private static final int SMOKE_DURATION_TICKS = 160;
	private static final int SMOKE_PARTICLES_PER_TICK = 45;

	// Tether
	private static final double TETHER_MAX_DISTANCE = 48;
	private static final double TETHER_MOB_SPEED = 0.9;
	private static final double TETHER_PLAYER_SPEED = 0.8;
	private static final double TETHER_ARRIVE_DISTANCE = 2.5;
	private static final int TETHER_MAX_PULL_TICKS = 100;
	private static final long TETHER_ANCHOR_LIFETIME_MS = 15000;

	// Tide
	private static final int TIDE_MAX_TICKS = 200;

	/**
	 * The rope each player last attached with a Grapple Arrow (to the stuck arrow) or a Tether Arrow (to the entity hit).
	 * Only one rope exists per player, the newest one replaces the old one.
	 */
	private static final Map<UUID, RopeAnchor> ropeAnchors = new HashMap<>();
	/** Players whose rope is currently pulling. */
	private static final Set<UUID> pullingPlayers = new HashSet<>();

	/**
	 * A Grapple or Tether rope waiting to be pulled.
	 *
	 * @param tether Whether it is a Tether rope (pulls the entity) instead of a Grapple rope (pulls the shooter).
	 * @param entityUuid The stuck arrow for Grapple, the entity hit for Tether.
	 * @param createdAt When the rope was attached.
	 */
	private record RopeAnchor(boolean tether, UUID entityUuid, long createdAt) {}

	private static final Random random = new Random();

	private static final Color GRAPPLE_COLOR = Color.fromRGB(216, 196, 154);
	private static final Color TETHER_COLOR = Color.fromRGB(138, 98, 54);

	/**
	 * Outlines every living entity near the impact for the shooter only, through walls.
	 *
	 * @param shooter The player that shot the arrow.
	 * @param impact Where the arrow landed.
	 */
	public static void sonarPulse(Player shooter, Location impact) {
		World world = impact.getWorld();
		for (int i = 0; i < 24; i++) {
			double angle = 2 * Math.PI * i / 24;
			Location point = impact.clone().add(Math.cos(angle) * 1.5, 0.2, Math.sin(angle) * 1.5);
			world.spawnParticle(Particle.SCULK_CHARGE_POP, point, 1, 0, 0, 0, 0);
		}
		world.spawnParticle(Particle.SCULK_SOUL, impact, 6, 0.3, 0.3, 0.3, 0.02);

		List<Entity> glowed = new ArrayList<>();
		for (Entity entity : world.getNearbyEntities(impact, SONAR_RADIUS, SONAR_RADIUS, SONAR_RADIUS)) {
			if (!(entity instanceof LivingEntity) || entity instanceof ArmorStand || entity.equals(shooter)) {
				continue;
			}
			if (entity.getLocation().distanceSquared(impact) > SONAR_RADIUS * SONAR_RADIUS) {
				continue;
			}
			if (entity instanceof Player target) {
				if (AranarthUtils.isVanishedFrom(shooter, target.getUniqueId()) || !DominionUtils.canAttackPlayer(shooter, target)) {
					continue;
				}
			}
			sendClientGlow(shooter, entity, true);
			glowed.add(entity);
		}

		if (glowed.isEmpty()) {
			return;
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!shooter.isOnline()) {
					return;
				}
				for (Entity entity : glowed) {
					if (entity.isValid()) {
						sendClientGlow(shooter, entity, false);
					}
				}
			}
		}.runTaskLater(AranarthCore.getInstance(), SONAR_DURATION_TICKS);
	}

	/**
	 * Toggles the glowing outline of an entity for a single viewer only.
	 *
	 * @param viewer The player who sees the outline.
	 * @param entity The entity being outlined.
	 * @param glow Whether the outline is shown or removed.
	 */
	public static void sendClientGlow(Player viewer, Entity entity, boolean glow) {
		byte flags = 0;
		if (entity.getFireTicks() > 0) flags |= 0x01;
		if (entity instanceof Player p) {
			if (p.isSneaking()) flags |= 0x02;
			if (p.isSprinting()) flags |= 0x08;
		}
		if (entity instanceof LivingEntity living && living.isInvisible()) flags |= 0x20;
		if (glow || entity.isGlowing()) flags |= 0x40;

		List<EntityData<?>> data = Collections.singletonList(new EntityData<>(0, EntityDataTypes.BYTE, flags));
		WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entity.getEntityId(), data);
		PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
	}

	/**
	 * Remembers the arrow a Grapple Arrow anchored into, so the shooter can reel towards it.
	 *
	 * @param shooter The player that shot the arrow.
	 * @param arrow The arrow stuck in a block.
	 */
	public static void setGrappleAnchor(Player shooter, Arrow arrow) {
		if (pullingPlayers.contains(shooter.getUniqueId())) {
			return;
		}
		ropeAnchors.put(shooter.getUniqueId(), new RopeAnchor(false, arrow.getUniqueId(), System.currentTimeMillis()));
	}

	/**
	 * Remembers the entity a Tether Arrow hit, so the shooter can pull it in.
	 *
	 * @param shooter The player that shot the arrow.
	 * @param target The entity that was hit.
	 */
	public static void setTetherAnchor(Player shooter, Entity target) {
		if (pullingPlayers.contains(shooter.getUniqueId()) || !canTether(shooter, target, false)) {
			return;
		}
		ropeAnchors.put(shooter.getUniqueId(), new RopeAnchor(true, target.getUniqueId(), System.currentTimeMillis()));
		drawLine(shooter.getLocation().add(0, 1, 0), target.getLocation().add(0, target.getHeight() / 2, 0), TETHER_COLOR);
	}

	/**
	 * Clears all Grapple and Tether state of a player, which also stops any rope that is currently pulling.
	 *
	 * @param uuid The player's UUID.
	 */
	public static void clearRope(UUID uuid) {
		ropeAnchors.remove(uuid);
		pullingPlayers.remove(uuid);
	}

	/**
	 * Snaps the player's rope if it is currently pulling, such as when they take damage.
	 *
	 * @param player The player.
	 */
	public static void snapRope(Player player) {
		UUID uuid = player.getUniqueId();
		if (pullingPlayers.remove(uuid)) {
			ropeAnchors.remove(uuid);
			player.getWorld().playSound(player.getLocation(), Sound.ITEM_LEAD_BREAK, 1F, 1F);
		}
	}

	/**
	 * Starts pulling the player's Grapple or Tether rope, if they are holding a bow or crossbow.
	 * The rope keeps pulling until it reaches the end, unless the player takes damage.
	 *
	 * @param player The player that started sneaking.
	 */
	public static void tryPullRope(Player player) {
		UUID uuid = player.getUniqueId();
		if (pullingPlayers.contains(uuid) || !isHoldingBow(player)) {
			return;
		}
		RopeAnchor anchor = ropeAnchors.get(uuid);
		if (anchor == null) {
			return;
		}
		Entity anchorEntity = player.getServer().getEntity(anchor.entityUuid());
		if (anchor.tether()) {
			if (System.currentTimeMillis() - anchor.createdAt() > TETHER_ANCHOR_LIFETIME_MS
                    || !canTether(player, anchorEntity, true)) {
				ropeAnchors.remove(uuid);
				return;
			}
			if (anchorEntity.getLocation().distanceSquared(player.getLocation()) > TETHER_MAX_DISTANCE * TETHER_MAX_DISTANCE) {
				return;
			}
			pullingPlayers.add(uuid);
			startTetherPull(player, anchorEntity);
		} else {
			if (!(anchorEntity instanceof Arrow arrow) || !arrow.isValid() || !arrow.isInBlock()
					|| !arrow.getWorld().equals(player.getWorld())) {
				ropeAnchors.remove(uuid);
				return;
			}
			if (arrow.getLocation().distanceSquared(player.getLocation()) > GRAPPLE_MAX_DISTANCE * GRAPPLE_MAX_DISTANCE) {
				return;
			}
			pullingPlayers.add(uuid);
			startGrapplePull(player, arrow);
		}
	}

	/**
	 * Reels the player towards their Grapple Arrow, launching them upwards when they reach it.
	 */
	private static void startGrapplePull(Player player, Arrow arrow) {
		UUID uuid = player.getUniqueId();
		new BukkitRunnable() {
			int ticks = 0;
			double closestDistance = Double.MAX_VALUE;
			int stalledTicks = 0;

			@Override
			public void run() {
				ticks++;
				// Snapped by damage, or the player left
				if (!pullingPlayers.contains(uuid) || !player.isOnline() || player.isDead()) {
					pullingPlayers.remove(uuid);
					cancel();
					return;
				}
				if (!arrow.isValid() || !arrow.isInBlock() || !arrow.getWorld().equals(player.getWorld())) {
					finish();
					return;
				}
				Vector toAnchor = arrow.getLocation().toVector().subtract(player.getLocation().add(0, 0.5, 0).toVector());
				double distance = toAnchor.length();
				player.setFallDistance(0);

				// Pressed up against the wall the arrow is stuck in counts as reaching the end too
				if (distance < closestDistance - 0.05) {
					closestDistance = distance;
					stalledTicks = 0;
				} else {
					stalledTicks++;
				}
				boolean stalledAtEnd = distance <= GRAPPLE_SLOW_DISTANCE && stalledTicks >= GRAPPLE_STALL_TICKS;
				if (distance <= GRAPPLE_ARRIVE_DISTANCE || stalledAtEnd) {
					// The little hop at the end of the rope
					player.setVelocity(new Vector(0, GRAPPLE_END_BOOST, 0));
					finish();
					return;
				}
				if (ticks > GRAPPLE_MAX_REEL_TICKS) {
					finish();
					return;
				}
				player.setVelocity(toAnchor.normalize().multiply(distance > GRAPPLE_SLOW_DISTANCE ? GRAPPLE_SPEED : GRAPPLE_SLOW_SPEED));
				if (ticks % 2 == 0) {
					drawLine(player.getLocation().add(0, 1, 0), arrow.getLocation(), GRAPPLE_COLOR);
				}
			}

			private void finish() {
				pullingPlayers.remove(uuid);
				ropeAnchors.remove(uuid);
				player.setFallDistance(0);
				cancel();
			}
		}.runTaskTimer(AranarthCore.getInstance(), 0, 1);
	}

	/**
	 * Pulls the entity hit by a Tether Arrow towards the player until it reaches them.
	 */
	private static void startTetherPull(Player player, Entity target) {
		UUID uuid = player.getUniqueId();
		boolean isPlayer = target instanceof Player;
		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				ticks++;
				// Snapped by damage, or the player left
				if (!pullingPlayers.contains(uuid) || !player.isOnline() || player.isDead()) {
					pullingPlayers.remove(uuid);
					cancel();
					return;
				}
				if (!target.isValid() || !target.getWorld().equals(player.getWorld()) || ticks > TETHER_MAX_PULL_TICKS) {
					finish();
					return;
				}
				Vector toPlayer = player.getLocation().toVector().subtract(target.getLocation().toVector());
				target.setFallDistance(0);
				if (toPlayer.length() <= TETHER_ARRIVE_DISTANCE) {
					target.setVelocity(new Vector());
					finish();
					return;
				}
				if (target.isInsideVehicle()) {
					target.leaveVehicle();
				}
				Vector velocity = toPlayer.normalize().multiply(isPlayer ? TETHER_PLAYER_SPEED : TETHER_MOB_SPEED);
				// Keeps it from scraping along the ground
				velocity.setY(Math.max(velocity.getY(), 0) + 0.1);
				target.setVelocity(velocity);
				if (ticks % 2 == 0) {
					drawLine(player.getLocation().add(0, 1, 0), target.getLocation().add(0, target.getHeight() / 2, 0), TETHER_COLOR);
				}
			}

			private void finish() {
				pullingPlayers.remove(uuid);
				ropeAnchors.remove(uuid);
				cancel();
			}
		}.runTaskTimer(AranarthCore.getInstance(), 0, 1);
	}

	/**
	 * Determines whether a Tether Arrow may pull the entity.
	 *
	 * @param shooter The player that shot the arrow.
	 * @param target The entity that was hit.
	 * @param silent Whether to skip messaging the shooter.
	 */
	private static boolean canTether(Player shooter, Entity target, boolean silent) {
		if (!(target instanceof LivingEntity) || !target.isValid() || target instanceof ArmorStand || target instanceof Boss
				|| target instanceof Warden || target.equals(shooter) || !target.getWorld().equals(shooter.getWorld())) {
			return false;
		}
		if (target instanceof Player targetPlayer) {
			return targetPlayer.getGameMode() != GameMode.SPECTATOR && DominionUtils.canAttackPlayer(shooter, targetPlayer);
		}
		// Animals and other non-hostile mobs cannot be pulled out of another Dominion's land
		if (!(target instanceof Enemy)) {
			return silent ? isAllowedLocation(shooter, target.getLocation()) : canAffectLocation(shooter, target.getLocation());
		}
		return true;
	}

	/**
	 * Gently curves a Homing Arrow towards the nearest valid target in front of it.
	 *
	 * @param shooter The player that shot the arrow.
	 * @param arrow The arrow in flight.
	 */
	public static void startHoming(Player shooter, AbstractArrow arrow) {
		// Steers once straight away, so targets that are very close still get homed in on
		steerHoming(shooter, arrow);
		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				ticks++;
				if (!arrow.isValid() || arrow.isInBlock() || ticks > HOMING_MAX_TICKS) {
					cancel();
					return;
				}
				steerHoming(shooter, arrow);
			}
		}.runTaskTimer(AranarthCore.getInstance(), 1, 1);
	}

	/**
	 * Steers a Homing Arrow towards its target for a single tick.
	 * The closer the target, the harder it turns, and once the target is within the next couple of ticks of travel it aims straight at it.
	 */
	private static void steerHoming(Player shooter, AbstractArrow arrow) {
		Vector velocity = arrow.getVelocity();
		double speed = velocity.length();
		if (speed < 0.2) {
			return;
		}
		LivingEntity target = findHomingTarget(shooter, arrow, velocity);
		if (target == null) {
			return;
		}
		Vector toTarget = target.getLocation().add(0, target.getHeight() / 2, 0).toVector().subtract(arrow.getLocation().toVector());
		double distance = toTarget.length();
		toTarget.normalize();
		Vector steered;
		if (distance <= speed * 2) {
			steered = toTarget.multiply(speed);
		} else {
			double closeness = 1 - Math.min(1, distance / HOMING_RANGE);
			double steer = HOMING_STEER + (HOMING_CLOSE_STEER - HOMING_STEER) * closeness;
			steered = velocity.clone().normalize().multiply(1 - steer).add(toTarget.multiply(steer)).normalize().multiply(speed);
		}
		arrow.setVelocity(steered);
	}

	/**
	 * Finds the closest valid target within the Homing Arrow's cone of vision.
	 */
	private static LivingEntity findHomingTarget(Player shooter, AbstractArrow arrow, Vector velocity) {
		LivingEntity closest = null;
		double closestDistance = Double.MAX_VALUE;
		Location arrowLocation = arrow.getLocation();
		for (Entity entity : arrow.getNearbyEntities(HOMING_RANGE, HOMING_RANGE, HOMING_RANGE)) {
			if (!(entity instanceof LivingEntity living) || living instanceof ArmorStand || living.isDead()
					|| living.equals(shooter) || living.isInvisible() || living.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
				continue;
			}
			if (living instanceof Tameable tameable && tameable.getOwner() != null
					&& tameable.getOwner().getUniqueId().equals(shooter.getUniqueId())) {
				continue;
			}
			if (living instanceof Player target) {
				if (target.getGameMode() == GameMode.SPECTATOR || target.getGameMode() == GameMode.CREATIVE
						|| AranarthUtils.isVanishedFrom(shooter, target.getUniqueId()) || !DominionUtils.canAttackPlayer(shooter, target)) {
					continue;
				}
			}
			Vector toEntity = living.getLocation().add(0, living.getHeight() / 2, 0).toVector().subtract(arrowLocation.toVector());
			double distance = toEntity.length();
			// A wider cone up close, where a small aiming error is a much bigger angle
			double closeness = 1 - Math.min(1, distance / HOMING_RANGE);
			double cone = HOMING_CONE_RADIANS + (HOMING_CLOSE_CONE_RADIANS - HOMING_CONE_RADIANS) * closeness;
			if (distance > HOMING_RANGE || velocity.angle(toEntity) > cone) {
				continue;
			}
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = living;
			}
		}
		return closest;
	}

	/**
	 * Releases a cloud of thick smoke that gives Darkness to those inside and makes mobs lose their targets.
	 * The particles are not scaled by /particles, so nobody can turn them down to see through the cloud.
	 *
	 * @param shooter The player that shot the arrow.
	 * @param center Where the arrow landed.
	 */
	public static void smokeCloud(Player shooter, Location center) {
		World world = center.getWorld();
		Location cloudCenter = center.clone().add(0, 1, 0);
		world.playSound(cloudCenter, Sound.BLOCK_FIRE_EXTINGUISH, 1F, 0.6F);
		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				ticks++;
				if (ticks > SMOKE_DURATION_TICKS) {
					cancel();
					return;
				}

				// Billows out over the first half second, then thins out over the last second
				double spread = Math.min(1.0, 0.4 + ticks / 10.0 * 0.6);
				double fade = Math.min(1.0, (SMOKE_DURATION_TICKS - ticks) / 20.0);
				int count = (int) Math.max(6, Math.round(SMOKE_PARTICLES_PER_TICK * fade));
				for (int i = 0; i < count; i++) {
					double theta = Math.acos(1.0 - 2.0 * random.nextDouble());
					double phi = 2.0 * Math.PI * random.nextDouble();
					double r = SMOKE_RADIUS * spread * Math.cbrt(random.nextDouble());
					Location point = cloudCenter.clone().add(r * Math.sin(theta) * Math.cos(phi),
							r * Math.cos(theta) * 0.6, r * Math.sin(theta) * Math.sin(phi));
					if (point.getBlock().getType().isSolid()) {
						continue;
					}
					// Large grey dust is what actually blocks vision, the rest gives it a smoky texture
					int roll = random.nextInt(10);
					if (roll < 7) {
						int shade = 40 + random.nextInt(50);
						Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(shade, shade, shade), 4F);
						world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
					} else if (roll < 9) {
						world.spawnParticle(Particle.LARGE_SMOKE, point, 1, 0, 0, 0, 0.01);
					} else {
						world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, point, 0, 0, 0.01, 0, 1);
					}
				}

				if (ticks % 10 != 0) {
					return;
				}
				double radiusSq = SMOKE_EFFECT_RADIUS * SMOKE_EFFECT_RADIUS;
				double search = SMOKE_EFFECT_RADIUS + 16;
				for (Entity entity : world.getNearbyEntities(cloudCenter, search, search, search)) {
					if (!(entity instanceof LivingEntity living)) {
						continue;
					}
					boolean inside = living.getLocation().distanceSquared(cloudCenter) <= radiusSq;
					if (living instanceof Player target) {
						if (inside && !target.equals(shooter) && DominionUtils.canAttackPlayer(shooter, target)) {
							target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, false, true));
						}
					} else if (living instanceof Mob mob && !(mob instanceof Boss) && !(mob instanceof Warden)) {
						LivingEntity target = mob.getTarget();
						boolean targetInside = target != null && target.getWorld().equals(world)
								&& target.getLocation().distanceSquared(cloudCenter) <= radiusSq;
						if (target != null && (inside || targetInside)) {
							mob.setTarget(null);
						}
					}
				}
			}
		}.runTaskTimer(AranarthCore.getInstance(), 0, 1);
	}

	/**
	 * Swaps the positions of the shooter and the entity hit by a Shifting Arrow.
	 * If the arrow killed the target, the shooter is moved to where the target was instead.
	 *
	 * @param shooter The player that shot the arrow.
	 * @param target The entity that was hit.
	 * @param hitLocation Where the target was when the arrow hit it.
	 */
	public static void shift(Player shooter, Entity target, Location hitLocation) {
		if (!(target instanceof LivingEntity) || target instanceof ArmorStand || target instanceof Boss || target instanceof Warden
				|| target.equals(shooter) || !hitLocation.getWorld().equals(shooter.getWorld())) {
			return;
		}
		// Only a kill moves the shooter on their own, not the target logging out or despawning
		boolean killed = target.isDead();
		if (!killed && (!target.isValid() || !target.getWorld().equals(shooter.getWorld()))) {
			return;
		}
		if (!killed && target instanceof Player targetPlayer && !DominionUtils.canAttackPlayer(shooter, targetPlayer)) {
			return;
		}
		Location shooterLocation = shooter.getLocation();
		Location targetLocation = killed ? hitLocation.clone() : target.getLocation();
		if (!canAffectLocation(shooter, shooterLocation) || !canAffectLocation(shooter, targetLocation)) {
			return;
		}

		if (killed) {
			Location destination = targetLocation.clone();
			destination.setYaw(shooterLocation.getYaw());
			destination.setPitch(shooterLocation.getPitch());
			if (shooter.isInsideVehicle()) shooter.leaveVehicle();
			shooter.eject();
			shooter.teleport(destination);
			shooter.setFallDistance(0);
			World world = shooterLocation.getWorld();
			world.spawnParticle(Particle.PORTAL, shooterLocation.add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.3);
			world.spawnParticle(Particle.PORTAL, targetLocation.add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.3);
			return;
		}
		Location shooterDestination = targetLocation.clone();
		shooterDestination.setYaw(shooterLocation.getYaw());
		shooterDestination.setPitch(shooterLocation.getPitch());
		Location targetDestination = shooterLocation.clone();
		targetDestination.setYaw(targetLocation.getYaw());
		targetDestination.setPitch(targetLocation.getPitch());

		if (shooter.isInsideVehicle()) shooter.leaveVehicle();
		if (target.isInsideVehicle()) target.leaveVehicle();
		target.eject();
		shooter.eject();

		shooter.teleport(shooterDestination);
		target.teleport(targetDestination);
		shooter.setFallDistance(0);
		target.setFallDistance(0);

		World world = shooterLocation.getWorld();
		world.spawnParticle(Particle.PORTAL, shooterLocation.add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.3);
		world.spawnParticle(Particle.PORTAL, targetLocation.add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.3);
	}

	/**
	 * Keeps a Tide Arrow at its full speed while it travels through water.
	 *
	 * @param arrow The arrow in flight.
	 */
	public static void startTide(AbstractArrow arrow) {
		new BukkitRunnable() {
			int ticks = 0;
			Vector lastVelocity = arrow.getVelocity();

			@Override
			public void run() {
				ticks++;
				if (!arrow.isValid() || arrow.isInBlock() || ticks > TIDE_MAX_TICKS) {
					cancel();
					return;
				}
				if (arrow.isInWater()) {
					// Same drag and gravity as in the air, instead of the much heavier water drag
					Vector velocity = lastVelocity.clone().multiply(0.99);
					velocity.setY(velocity.getY() - 0.05);
					arrow.setVelocity(velocity);
					lastVelocity = velocity;
					arrow.getWorld().spawnParticle(Particle.BUBBLE, arrow.getLocation(), 2, 0.05, 0.05, 0.05, 0);
				} else {
					lastVelocity = arrow.getVelocity();
				}
			}
		}.runTaskTimer(AranarthCore.getInstance(), 1, 1);
	}

	/**
	 * Determines whether a special arrow may move things at the location, based on Dominion land.
	 * Sends the shooter a message when it is not allowed.
	 */
	private static boolean canAffectLocation(Player shooter, Location location) {
		if (isAllowedLocation(shooter, location)) {
			return true;
		}
		Dominion chunkDominion = DominionUtils.getDominionOfChunk(location.getChunk());
		shooter.sendMessage(ChatUtils.chatMessage(Lang.getFor(shooter, "arrow.not_in_dominion", "name", chunkDominion.getName())));
		return false;
	}

	/**
	 * @return Whether the location is unclaimed or claimed by the shooter's own Dominion.
	 */
	private static boolean isAllowedLocation(Player shooter, Location location) {
		Dominion chunkDominion = DominionUtils.getDominionOfChunk(location.getChunk());
		if (chunkDominion == null) {
			return true;
		}
		Dominion shooterDominion = DominionUtils.getPlayerDominion(shooter.getUniqueId());
		return shooterDominion != null && shooterDominion.isSameDominion(chunkDominion);
	}

	/**
	 * @return Whether the player is holding a bow or crossbow in either hand.
	 */
	private static boolean isHoldingBow(Player player) {
		return isBow(player.getInventory().getItemInMainHand()) || isBow(player.getInventory().getItemInOffHand());
	}

	private static boolean isBow(ItemStack item) {
		return item != null && (item.getType() == Material.BOW || item.getType() == Material.CROSSBOW);
	}

	/**
	 * Draws a rope-like line of dust particles between two points.
	 */
	private static void drawLine(Location from, Location to, Color color) {
		if (!from.getWorld().equals(to.getWorld())) {
			return;
		}
		Vector step = to.toVector().subtract(from.toVector());
		double length = step.length();
		if (length < 0.1) {
			return;
		}
		int points = (int) Math.min(40, Math.ceil(length * 2));
		step.multiply(1.0 / points);
		Location point = from.clone();
		Particle.DustOptions dust = new Particle.DustOptions(color, 0.7F);
		for (int i = 0; i <= points; i++) {
			from.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
			point.add(step);
		}
	}
}
