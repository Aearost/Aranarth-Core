package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.aearost.aranarthcore.objects.CustomKeys.ARMOR_TYPE;

/**
 * Plays the plugin-side Aranarthium armour animations.
 */
public class AranarthiumEffectsUtils {

	public static final int PERIOD_TICKS = 2;
	// Same reach as World#spawnParticle
	private static final double VIEW_RANGE = 32;

	// Scorched horn smoke: one short puff every 6 seconds, like a whale surfacing to breathe
	private static final int SMOKE_CYCLE_TICKS = 120;
	private static final int SMOKE_PUFF_TICKS = 20;
	// Horn tip relative to the neck pivot, in blocks at scale 1, measured from the Scorched 3D helmet model
	// x = towards the wearer's left, y = up, z = forward
	private static final Vector SCORCHED_HORN_TIP = new Vector(0.53, 1.09, -0.49);
	private static final double NECK_HEIGHT = 1.5;
	private static final double SNEAK_DROP = 0.3;

	// Points on the body relative to the feet, in blocks at scale 1 (x = left, y = up, z = forward), following body yaw
	private static final Vector SHOULDER = new Vector(0.39, 1.56, 0);
	private static final Vector CHEST_GEM = new Vector(0, 1.19, 0.21);
	private static final Vector HEAD_TOP = new Vector(0, 0.55, 0);

	// Aquatic: bubbles from the helmet and boots under water, and a short tide call every 10 seconds
	private static final double BUBBLES_PER_SECOND = 4;
	private static final int TIDE_CYCLE_TICKS = 200;
	private static final int TIDE_CALL_TICKS = 50;
	private static final double TIDE_PER_SECOND = 3;
	// Elven: a leaf every second or so from the shoulders
	private static final double LEAVES_PER_SECOND = 1.1;
	private static final Color[] LEAF_COLOURS = { Color.fromRGB(0x52962E), Color.fromRGB(0x408023), Color.fromRGB(0x6AA83A) };
	// Fae: pink and gold fairy dust drifting around the wearer
	private static final double DUST_PER_SECOND = 7;
	private static final Color[] FAE_DUST_COLOURS = { Color.fromRGB(0xFFD1EF), Color.fromRGB(0xF5BA27), Color.fromRGB(0xFFFFFF), Color.fromRGB(0xFF9FD4) };
	// Soulbound: enchanting glyphs drift into the chest crystal for 1.6 seconds every 10 seconds
	private static final int GLYPH_CYCLE_TICKS = 200;
	private static final int GLYPH_DRIFT_TICKS = 32;
	private static final double GLYPHS_PER_SECOND = 9;
	// Echo: one vibration at a time flies into the wearer, like a sculk sensor
	private static final int LISTEN_COOLDOWN_TICKS = 15;
	private static final Map<UUID, Integer> lastListen = new HashMap<>();

	// Ardent moonlight: a client-side light block on the wearer, shown to nearby players while it is dark
	private static final int MOONLIGHT_LEVEL = 12;
	private static final int DARK_LIGHT_LEVEL = 7;
	private static final double MOONLIGHT_RANGE = 48;
	private static final int MOONLIGHT_RESEND_TICKS = 40;
	private static final Map<UUID, Moonlight> moonlights = new HashMap<>();

	/**
	 * Where one Ardent wearer's light currently is, and who was shown it.
	 */
	private static class Moonlight {
		private Location at;
		private Set<UUID> shownTo = new HashSet<>();
		private int sentAt;
	}

	/**
	 * Runs every few ticks and plays every plugin-side armour animation.
	 */
	public static void tick() {
		int now = Bukkit.getCurrentTick();
		updateMoonlights(now);
		for (Player wearer : Bukkit.getOnlinePlayers()) {
			if (!canShowEffects(wearer)) {
				continue;
			}
			String helmet = getPieceType(wearer.getInventory().getHelmet());
			String boots = getPieceType(wearer.getInventory().getBoots());
			ItemStack chestItem = wearer.getInventory().getChestplate();
			String chest = getPieceType(chestItem);
			boolean isElytra = chestItem != null && chestItem.getType() == Material.ELYTRA;

			if ("scorched".equals(helmet)) {
				playScorchedSmoke(wearer, now);
			}
			if ("aquatic".equals(helmet) && wearer.isUnderWater() && roll(BUBBLES_PER_SECOND)) {
				spawn(wearer, Particle.BUBBLE, headPoint(wearer, HEAD_TOP), 0, 0, 1, 0, 0.12, null);
			}
			if ("aquatic".equals(boots) && wearer.isInWater() && roll(BUBBLES_PER_SECOND)) {
				spawn(wearer, Particle.BUBBLE, wearer.getLocation().add(0, 0.1, 0), 0, 0, 1, 0, 0.12, null);
			}
			if (chest != null && !isElytra) {
				switch (chest) {
					case "aquatic" -> playAquaticTide(wearer, now);
					case "elven" -> playElvenLeaves(wearer);
					case "fae" -> playFaeDust(wearer);
					case "soulbound" -> playSoulboundGlyphs(wearer, now);
					default -> { }
				}
			}
			if (chest != null && isElytra && wearer.isGliding()) {
				playGlideTrail(wearer, chest);
			}
		}
	}

	/**
	 * Every 6 seconds the Scorched helmet's horns let out one thick puff of smoke for about a second.
	 */
	private static void playScorchedSmoke(Player wearer, int now) {
		// Offset per wearer so a group of Scorched players doesn't puff in perfect sync
		if (!inWindow(wearer, now, SMOKE_CYCLE_TICKS, SMOKE_PUFF_TICKS) || wearer.getEyeLocation().getBlock().isLiquid()) {
			return;
		}
		for (int side : new int[] { 1, -1 }) {
			Vector tip = SCORCHED_HORN_TIP.clone().setX(SCORCHED_HORN_TIP.getX() * side);
			// count 0 = the offsets are a direction, so the smoke rises gently
			spawn(wearer, Particle.SMOKE, headPoint(wearer, tip), 0, 0, 1, 0, 0.04, null);
		}
	}

	/**
	 * Underwater every 10 seconds nautilus sparks swirl in and sink into the chest aquamarine for a moment.
	 */
	private static void playAquaticTide(Player wearer, int now) {
		if (!wearer.isUnderWater() || !inWindow(wearer, now, TIDE_CYCLE_TICKS, TIDE_CALL_TICKS) || !roll(TIDE_PER_SECOND)) {
			return;
		}
		flyInto(wearer, Particle.NAUTILUS, bodyPoint(wearer, CHEST_GEM), 0.95);
	}

	/**
	 * A leaf drifts down from one of the Elven shoulders every second or so.
	 */
	private static void playElvenLeaves(Player wearer) {
		if (!roll(LEAVES_PER_SECOND)) {
			return;
		}
		ThreadLocalRandom random = ThreadLocalRandom.current();
		Vector shoulder = SHOULDER.clone().setX(SHOULDER.getX() * (random.nextBoolean() ? 1 : -1));
		spawn(wearer, Particle.TINTED_LEAVES, bodyPoint(wearer, shoulder), 1, 0.05, 0.05, 0.05, 0,
				LEAF_COLOURS[random.nextInt(LEAF_COLOURS.length)]);
	}

	/**
	 * Pink and gold sparkles drift around the Fae wearer, sized and placed by their scale so they hug a tiny fairy.
	 */
	private static void playFaeDust(Player wearer) {
		if (!roll(DUST_PER_SECOND)) {
			return;
		}
		ThreadLocalRandom random = ThreadLocalRandom.current();
		double scale = getScale(wearer);
		double angle = random.nextDouble(Math.PI * 2);
		double radius = 0.375 * scale * random.nextDouble(0.5, 1);
		Location at = wearer.getLocation().add(Math.cos(angle) * radius, random.nextDouble(0.25, 2.1) * scale, Math.sin(angle) * radius);
		if (random.nextInt(4) == 0) {
			spawn(wearer, Particle.WAX_ON, at, 1, 0, 0, 0, 0, null);
		} else {
			float size = (float) Math.max(0.2, Math.min(1.2, 0.6 * scale));
			spawn(wearer, Particle.DUST, at, 1, 0.02, 0.02, 0.02, 0,
					new Particle.DustOptions(FAE_DUST_COLOURS[random.nextInt(FAE_DUST_COLOURS.length)], size));
		}
	}

	/**
	 * Every 10 seconds, enchanting glyphs drift in from around the wearer and sink into the Soulbound chest crystal.
	 */
	private static void playSoulboundGlyphs(Player wearer, int now) {
		if (!inWindow(wearer, now, GLYPH_CYCLE_TICKS, GLYPH_DRIFT_TICKS) || !roll(GLYPHS_PER_SECOND)) {
			return;
		}
		flyInto(wearer, Particle.ENCHANT, bodyPoint(wearer, CHEST_GEM), 1.1);
	}

	/**
	 * While gliding, each set's own particles stream off the elytra's wing tips.
	 */
	private static void playGlideTrail(Player wearer, String set) {
		double scale = getScale(wearer);
		Location loc = wearer.getLocation();
		double yaw = Math.toRadians(loc.getYaw());
		Vector left = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
		Vector back = loc.getDirection().multiply(-0.4 * scale);
		for (int side : new int[] { 1, -1 }) {
			Location tip = loc.clone().add(0, 0.3 * scale, 0).add(left.clone().multiply(0.8 * scale * side)).add(back);
			switch (set) {
				case "scorched" -> spawn(wearer, Particle.SMALL_FLAME, tip, 1, 0.02, 0.02, 0.02, 0, null);
				case "echo" -> spawn(wearer, Particle.SCULK_CHARGE_POP, tip, 1, 0.02, 0.02, 0.02, 0, null);
				default -> spawn(wearer, Particle.DUST, tip, 1, 0.02, 0.02, 0.02, 0, new Particle.DustOptions(glideColour(set), 0.9f));
			}
		}
	}

	private static Color glideColour(String set) {
		return switch (set) {
			case "dwarven" -> Color.fromRGB(0xE8C45A);
			case "aquatic" -> Color.fromRGB(0x86D3DA);
			case "ardent" -> Color.fromRGB(0xD4DDF0);
			case "elven" -> Color.fromRGB(0x6AA83A);
			case "fae" -> Color.fromRGB(0xFFD1EF);
			case "soulbound" -> Color.fromRGB(0xB38EF3);
			default -> Color.WHITE;
		};
	}

	/**
	 * Echo "Listening" effect , exactly like a sculk sensor.
	 * Called by {@link EchoArmourUtils} for every vibration a wearer hears, at most one particle per wearer at a time.
	 *
	 * @param wearer The Echo wearer who heard it.
	 * @param origin Where the vibration came from.
	 */
	public static void playEchoListen(Player wearer, Location origin) {
		if (!canShowEffects(wearer)) {
			return;
		}
		int now = Bukkit.getCurrentTick();
		Integer last = lastListen.get(wearer.getUniqueId());
		if (last != null && now - last < LISTEN_COOLDOWN_TICKS) {
			return;
		}
		lastListen.put(wearer.getUniqueId(), now);
		int arrivalTicks = 4 + (int) (origin.distance(wearer.getEyeLocation()) * 2);
		Vibration vibration = new Vibration(new Vibration.Destination.EntityDestination(wearer), arrivalTicks);
		spawn(wearer, Particle.VIBRATION, origin, 1, 0, 0, 0, 0, vibration);
	}

	/**
	 * Ardent moonlight adding a light effect around the wearer.
	 */
	private static void updateMoonlights(int now) {
		for (UUID uuid : List.copyOf(moonlights.keySet())) {
			if (Bukkit.getPlayer(uuid) == null) {
				clearMoonlight(uuid);
			}
		}
		lastListen.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
		for (Player wearer : Bukkit.getOnlinePlayers()) {
			Location target = null;
			if (AranarthUtils.isWearingArmorType(wearer, "ardent") && canShowEffects(wearer)
					&& wearer.getEyeLocation().getBlock().getLightLevel() <= DARK_LIGHT_LEVEL) {
				target = getMoonlightSpot(wearer);
			}
			if (target == null) {
				clearMoonlight(wearer.getUniqueId());
				continue;
			}

			Moonlight light = moonlights.computeIfAbsent(wearer.getUniqueId(), k -> new Moonlight());
			boolean moved = light.at == null || !isSameBlock(light.at, target);
			if (moved && light.at != null) {
				restoreFor(light.shownTo, light.at);
				light.shownTo.clear();
			}
			light.at = target;

			Set<UUID> viewers = new HashSet<>();
			for (Player viewer : wearer.getWorld().getPlayers()) {
				if ((viewer.equals(wearer) || viewer.canSee(wearer)) && !hasEffectsDisabled(viewer)
						&& viewer.getLocation().distanceSquared(target) <= MOONLIGHT_RANGE * MOONLIGHT_RANGE) {
					viewers.add(viewer.getUniqueId());
				}
			}
			Set<UUID> leaving = new HashSet<>(light.shownTo);
			leaving.removeAll(viewers);
			restoreFor(leaving, target);

			// Resent now and then, since a chunk reload or block update on the client would wipe the fake block
			boolean resend = moved || now - light.sentAt >= MOONLIGHT_RESEND_TICKS;
			BlockData data = createMoonlight(target.getBlock());
			for (UUID viewerId : viewers) {
				Player viewer = Bukkit.getPlayer(viewerId);
				if (viewer != null && (resend || !light.shownTo.contains(viewerId))) {
					viewer.sendBlockChange(target, data);
				}
			}
			if (resend) {
				light.sentAt = now;
			}
			light.shownTo = viewers;
		}
	}

	/**
	 * Provides the block the moonlight goes in: the wearer's head, else their feet, if it is air or still water.
	 */
	private static Location getMoonlightSpot(Player wearer) {
		for (Block block : new Block[] { wearer.getEyeLocation().getBlock(), wearer.getLocation().getBlock() }) {
			if (block.getType().isAir() || isWaterSource(block)) {
				return block.getLocation();
			}
		}
		return null;
	}

	private static boolean isWaterSource(Block block) {
		return block.getType() == Material.WATER && block.getBlockData() instanceof Levelled levelled && levelled.getLevel() == 0;
	}

	private static BlockData createMoonlight(Block block) {
		BlockData data = Material.LIGHT.createBlockData();
		if (data instanceof Levelled levelled) {
			levelled.setLevel(MOONLIGHT_LEVEL);
		}
		if (data instanceof Waterlogged waterlogged) {
			waterlogged.setWaterlogged(isWaterSource(block));
		}
		return data;
	}

	/**
	 * Shows the given players the real block again where a moonlight was.
	 */
	private static void restoreFor(Set<UUID> viewers, Location at) {
		BlockData real = at.getBlock().getBlockData();
		for (UUID viewerId : viewers) {
			Player viewer = Bukkit.getPlayer(viewerId);
			if (viewer != null && viewer.getWorld().equals(at.getWorld())) {
				viewer.sendBlockChange(at, real);
			}
		}
	}

	private static void clearMoonlight(UUID wearer) {
		Moonlight light = moonlights.remove(wearer);
		if (light != null && light.at != null) {
			restoreFor(light.shownTo, light.at);
		}
	}

	/**
	 * Puts every real block back for everyone who was shown a moonlight, used when the plugin is disabled.
	 */
	public static void disable() {
		for (UUID uuid : List.copyOf(moonlights.keySet())) {
			clearMoonlight(uuid);
		}
	}

	/**
	 * Sends an Aranarthium armour particle to every player near the wearer.
	 *
	 * @param wearer   The player wearing the Aranarthium piece.
	 * @param particle The particle.
	 * @param at       Where to spawn it.
	 * @param count    The base number of particles, before scaling.
	 * @param offsetX  The X offset.
	 * @param offsetY  The Y offset.
	 * @param offsetZ  The Z offset.
	 * @param extra    The extra particle data, typically the speed.
	 * @param data     The particle data (e.g. dust options), or null.
	 */
	public static <T> void spawn(Player wearer, Particle particle, Location at, int count,
								 double offsetX, double offsetY, double offsetZ, double extra, T data) {
		// An invisible or spectating wearer must not be given away by their trail, so only they see it
		boolean selfOnly = wearer.getGameMode() == GameMode.SPECTATOR || wearer.hasPotionEffect(PotionEffectType.INVISIBILITY);
		for (Player viewer : wearer.getWorld().getPlayers()) {
			if (!viewer.equals(wearer) && (selfOnly || !viewer.canSee(wearer))) {
				continue;
			}
			if (viewer.getLocation().distanceSquared(at) > VIEW_RANGE * VIEW_RANGE) {
				continue;
			}
			AranarthPlayer viewerAranarthPlayer = AranarthUtils.getPlayer(viewer.getUniqueId());
			if (viewerAranarthPlayer != null && viewerAranarthPlayer.isAranarthiumParticlesDisabled()) {
				continue;
			}
			int percent = viewerAranarthPlayer == null ? 100 : viewerAranarthPlayer.getParticleNum();
			int amount = scaleCount(Math.max(count, 1), percent);
			if (amount == 0) {
				continue;
			}
			if (count == 0) {
				for (int i = 0; i < amount; i++) {
					viewer.spawnParticle(particle, at, 0, offsetX, offsetY, offsetZ, extra, data);
				}
			} else {
				viewer.spawnParticle(particle, at, amount, offsetX, offsetY, offsetZ, extra, data);
			}
		}
	}

	/**
	 * Spawns a particle that flies into the target from a random point around it.
	 */
	private static void flyInto(Player wearer, Particle particle, Location target, double radius) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		double scale = getScale(wearer);
		double angle = random.nextDouble(Math.PI * 2);
		double elevation = random.nextDouble(-0.6, 0.9);
		double r = radius * scale;
		Vector from = new Vector(Math.cos(angle) * Math.cos(elevation) * r, Math.sin(elevation) * r * 0.7,
				Math.sin(angle) * Math.cos(elevation) * r);
		spawn(wearer, particle, target, 0, from.getX(), from.getY(), from.getZ(), 1, null);
	}

	/**
	 * Scales a particle count by a /particles percentage, rounding the remainder randomly so small counts
	 * still thin out smoothly (e.g. 1 particle at 50% shows half the time).
	 */
	private static int scaleCount(int count, int percent) {
		if (percent == 100) {
			return count;
		}
		double scaled = count * (percent / 100.0);
		int whole = (int) scaled;
		return ThreadLocalRandom.current().nextDouble() < scaled - whole ? whole + 1 : whole;
	}

	/**
	 * Provides whether an effect that plays perSecond times a second should fire on this run.
	 */
	private static boolean roll(double perSecond) {
		return ThreadLocalRandom.current().nextDouble() < perSecond * PERIOD_TICKS / 20.0;
	}

	/**
	 * Provides whether the wearer is inside the active part of a repeating cycle, offset per wearer so a group of
	 * wearers doesn't play in perfect sync.
	 */
	private static boolean inWindow(Player wearer, int now, int cycleTicks, int activeTicks) {
		return Math.floorMod(now + wearer.getUniqueId().hashCode(), cycleTicks) < activeTicks;
	}

	/**
	 * Provides the world position of a point fixed to the wearer's head.
	 * @param wearer The player wearing the piece.
	 * @param local  The point relative to the neck pivot, in blocks at scale 1 (x = left, y = up, z = forward).
	 * @return The point in the world.
	 */
	public static Location headPoint(Player wearer, Vector local) {
		double scale = getScale(wearer);
		Location loc = wearer.getLocation();
		double pitch = Math.toRadians(loc.getPitch());
		double yaw = Math.toRadians(loc.getYaw());

		// Pitch first (looking down tips the head forward around the neck), then yaw
		double y = local.getY() * Math.cos(pitch) - local.getZ() * Math.sin(pitch);
		double z = local.getY() * Math.sin(pitch) + local.getZ() * Math.cos(pitch);
		double x = local.getX();
		Vector left = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
		Vector forward = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
		Vector offset = left.multiply(x).add(forward.multiply(z)).add(new Vector(0, y, 0)).multiply(scale);

		double neck = (NECK_HEIGHT - (wearer.isSneaking() ? SNEAK_DROP : 0)) * scale;
		return loc.add(0, neck, 0).add(offset);
	}

	/**
	 * Provides the world position of a point fixed to the wearer's body, following the body's turn (not the head's).
	 * @param wearer The player wearing the piece.
	 * @param local  The point relative to the feet, in blocks at scale 1 (x = left, y = up, z = forward).
	 * @return The point in the world.
	 */
	public static Location bodyPoint(Player wearer, Vector local) {
		double scale = getScale(wearer);
		double yaw = Math.toRadians(wearer.getBodyYaw());
		Vector left = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
		Vector forward = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
		double y = local.getY() - (wearer.isSneaking() ? SNEAK_DROP : 0);
		Vector offset = left.multiply(local.getX()).add(forward.multiply(local.getZ())).add(new Vector(0, y, 0)).multiply(scale);
		return wearer.getLocation().add(offset);
	}

	/**
	 * Provides whether others may see the wearer's armour effects at all.
	 */
	private static boolean canShowEffects(Player wearer) {
		if (wearer.getGameMode() == GameMode.SPECTATOR || wearer.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
			return false;
		}
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(wearer.getUniqueId());
		return aranarthPlayer == null || !aranarthPlayer.isVanished();
	}

	private static boolean hasEffectsDisabled(Player viewer) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(viewer.getUniqueId());
		return aranarthPlayer != null && aranarthPlayer.isAranarthiumParticlesDisabled();
	}

	private static boolean isSameBlock(Location a, Location b) {
		return a.getWorld().equals(b.getWorld()) && a.getBlockX() == b.getBlockX()
				&& a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
	}

	/**
	 * Provides which Aranarthium set a single armour piece belongs to.
	 *
	 * @param item The armour piece.
	 * @return The set id (e.g. "scorched"), or null if it isn't Aranarthium armour.
	 */
	public static String getPieceType(ItemStack item) {
		if (item == null || !item.hasItemMeta()) {
			return null;
		}
		ItemMeta meta = item.getItemMeta();
		return meta.getPersistentDataContainer().get(ARMOR_TYPE, PersistentDataType.STRING);
	}

	private static double getScale(Player player) {
		AttributeInstance scale = player.getAttribute(Attribute.SCALE);
		return scale != null ? scale.getValue() : 1.0;
	}
}
