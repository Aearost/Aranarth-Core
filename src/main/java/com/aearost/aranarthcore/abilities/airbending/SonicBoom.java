package com.aearost.aranarthcore.abilities.airbending;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SonicBoom extends SoundAbility implements AddonAbility {

	private static final Particle.DustOptions CHARGE_DUST = new Particle.DustOptions(Color.fromRGB(0, 128, 128), 0.5f);

	@Attribute(Attribute.CHARGE_DURATION)
	private long chargeDuration;
	@Attribute(Attribute.RANGE)
	private double range;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.DAMAGE)
	private double maxDamage;

	private final double minDamage = 2.0;
	private final double hitRadius = 2.0;
	private final double travelSpeed = 1.5; // blocks per tick

	private long startTime;
	private boolean isCharged;
	private boolean isTraveling;
	private Location blastLocation;
	private Vector direction;
	private double distanceTraveled;
	private final Set<UUID> hitEntities = new HashSet<>();

	public SonicBoom(Player player) {
		super(player);

		this.chargeDuration = 750;
		this.range = 10.0;
		this.cooldown = 8000;
		this.maxDamage = 12.0;

		if (!bPlayer.canBend(this)) {
			remove();
			return;
		}

		this.startTime = System.currentTimeMillis();
		this.isCharged = false;
		this.isTraveling = false;
		this.distanceTraveled = 0;

		start();
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}

		if (isTraveling) {
			advanceBlast();
			return;
		}

		if (!bPlayer.canBend(this)) {
			remove();
			return;
		}

		if (!player.isSneaking()) {
			if (isCharged) {
				launch();
			} else {
				remove();
			}
			return;
		}

		if (System.currentTimeMillis() - startTime >= chargeDuration) {
			isCharged = true;
		}

		if (isCharged) {
			spawnChargeParticles();
		}
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	private void launch() {
		isTraveling = true;
		direction = player.getEyeLocation().getDirection().normalize();
		blastLocation = player.getEyeLocation().clone();
		bPlayer.addCooldown(this);
		player.getWorld().playSound(blastLocation, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);
	}

	private void advanceBlast() {
		double stepSize = 0.25;
		int steps = (int) (travelSpeed / stepSize);

		for (int i = 0; i < steps; i++) {
			if (distanceTraveled >= range) {
				remove();
				return;
			}

			blastLocation.add(direction.clone().multiply(stepSize));
			distanceTraveled += stepSize;

			// Spawn particles every ~0.5 blocks
			if (i % 2 == 0) {
				blastLocation.getWorld().spawnParticle(Particle.SONIC_BOOM, blastLocation, 1, 0, 0, 0, 0);
			}

			checkEntityCollisions();
		}
	}

	private void spawnChargeParticles() {
		Location eyeLoc = player.getEyeLocation();
		double spread = 0.35;
		for (int i = 0; i < 4; i++) {
			double offX = (Math.random() - 0.5) * spread * 2;
			double offY = (Math.random() - 0.5) * spread;
			double offZ = (Math.random() - 0.5) * spread * 2;
			eyeLoc.getWorld().spawnParticle(Particle.DUST, eyeLoc.clone().add(offX, offY, offZ), 1, 0, 0, 0, 0, CHARGE_DUST);
		}
	}

	private void checkEntityCollisions() {
		for (LivingEntity entity : blastLocation.getWorld().getLivingEntities()) {
			if (entity.equals(player)) continue;
			if (hitEntities.contains(entity.getUniqueId())) continue;

			Location entityCenter = entity.getLocation().add(0, entity.getHeight() / 2.0, 0);
			if (entityCenter.distance(blastLocation) <= hitRadius) {
				hitEntities.add(entity.getUniqueId());
				applyEffects(entity);
			}
		}
	}

	private void applyEffects(LivingEntity entity) {
		double t = Math.min(distanceTraveled / range, 1.0);
		double damage = maxDamage - (maxDamage - minDamage) * t;
		entity.damage(damage, player);
		applySoundDebuff(entity);
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return isTraveling ? blastLocation : player.getLocation();
	}

	@Override
	public String getName() {
		return "SonicBoom";
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public String getAuthor() {
		return "Aearost";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getDescription() {
		return "Channel the deepest point of your Throat Chakra, and release a sonic boom towards your target. " +
				"Hit entities will be dazed by the attack, temporarily applying Slowness I and Blindness I.\n" +
				ChatUtils.translateToColor("&fUsage: Hold Sneak until you see particles and then release sneak to fire the attack.");
	}

}
