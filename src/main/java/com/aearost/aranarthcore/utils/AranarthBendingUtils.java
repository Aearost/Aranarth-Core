package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AranarthBendingUtils {

    /**
     * Ordered block progression used by lava warm-up abilities (MagmaGlaives, Eruption).
     * Index 0 = first transition (Stone), index 3 = fully charged (Magma Block).
     */
    public static final Material[] LAVA_WARMUP_SEQUENCE = {
            Material.STONE,
            Material.GRANITE,
            Material.NETHERRACK,
            Material.MAGMA_BLOCK,
    };

    // -------------------------------------------------------------------------
    // Spiritual energy palette — shared across spiritual abilities
    // -------------------------------------------------------------------------

    /**
     * The four spirit colours used by AngeredSpirits and EnergyBurst, in index order:
     * 0 = Red (Weakness), 1 = Blue (Slowness), 2 = Purple (Blindness), 3 = Yellow (Nausea).
     */
    public static final Color[] SPIRIT_COLORS = {
            Color.fromRGB(220, 30,  30),   // Red    → Weakness
            Color.fromRGB(30,  100, 220),  // Blue   → Slowness
            Color.fromRGB(140, 30,  200),  // Purple → Blindness
            Color.fromRGB(220, 200, 30),   // Yellow → Nausea
    };

    /** Matching potion effects for each entry in {@link #SPIRIT_COLORS}. */
    public static final PotionEffectType[] SPIRIT_EFFECT_TYPES = {
            PotionEffectType.WEAKNESS,
            PotionEffectType.SLOWNESS,
            PotionEffectType.BLINDNESS,
            PotionEffectType.NAUSEA,
    };

    /** Duration (ticks) applied by all spiritual-ability curse effects. */
    public static final int SPIRIT_EFFECT_DURATION  = 100;
    /** Amplifier used for all spiritual-ability curse effects (0 = level I). */
    public static final int SPIRIT_EFFECT_AMPLIFIER = 0;

    // -------------------------------------------------------------------------
    // Bloodbending dust palette — shared across all bloodbending abilities
    // -------------------------------------------------------------------------

    /** Dark crimson dust used for sustained bloodbending particle effects. */
    public static final Particle.DustOptions BLOOD_DUST =
            new Particle.DustOptions(Color.fromRGB(170, 8, 8), 1.3f);
    /** Bright red dust used for bursts and impacts in bloodbending abilities. */
    public static final Particle.DustOptions BLOOD_DUST_BRIGHT =
            new Particle.DustOptions(Color.fromRGB(230, 35, 35), 0.9f);

    /** Bright blue water dust shared by healing waterbending abilities during their water phase. */
    public static final Particle.DustOptions WATER_DUST =
            new Particle.DustOptions(Color.fromRGB(40, 120, 255), 1.0f);
    /** Darker blue water dust shared by healing waterbending abilities during their water phase. */
    public static final Particle.DustOptions WATER_DUST_DARK =
            new Particle.DustOptions(Color.fromRGB(20, 75, 225), 0.8f);

    /** Warm yellow dust used for the Jolt orbital ready state and shot trail. */
    public static final Particle.DustOptions LIGHTNING_DUST =
            new Particle.DustOptions(Color.fromRGB(255, 240, 80), 0.8f);
    /** Pale yellow-white dust used for alternating orbital particles and trail bursts in Jolt. */
    public static final Particle.DustOptions LIGHTNING_DUST_BRIGHT =
            new Particle.DustOptions(Color.fromRGB(255, 255, 180), 1.0f);
    /** Light blue dust used for the Discharge bolt path. */
    public static final Particle.DustOptions LIGHTNING_DUST_BLUE =
            new Particle.DustOptions(Color.fromRGB(100, 200, 255), 1.0f);

    private static final Map<UUID, Long> ELECTROCUTED_ENTITIES = new HashMap<>();

    /** Hex colour used for the action bar electrocution message, matching LIGHTNING_DUST. */
    private static final TextColor ELECTROCUTION_COLOR = TextColor.fromHexString("#FFF050");

    /**
     * Electrocutes a living entity for the given duration, freezing their horizontal
     * movement each tick while allowing vertical movement and head rotation. If the
     * target is a player, they also receive a {@code * ELECTROCUTED *} action bar
     * message. Repeated calls refresh the duration.
     *
     * @param target     The entity to electrocute.
     * @param durationMs How long the electrocution lasts in milliseconds.
     */
    public static void applyElectrocution(LivingEntity target, long durationMs) {
        UUID uuid = target.getUniqueId();
        ELECTROCUTED_ENTITIES.put(uuid, System.currentTimeMillis() + durationMs);
        int stunTicks = (int) (durationMs / 50L);

        if (target instanceof Player p) {
            p.sendActionBar(Component.text("* ELECTROCUTED *").color(ELECTROCUTION_COLOR));
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= stunTicks || !ELECTROCUTED_ENTITIES.containsKey(uuid)) {
                    ELECTROCUTED_ENTITIES.remove(uuid);
                    cancel();
                    return;
                }
                if (target.isDead() || (target instanceof Player p && !p.isOnline())) {
                    ELECTROCUTED_ENTITIES.remove(uuid);
                    cancel();
                    return;
                }
                target.setVelocity(new Vector(0, target.getVelocity().getY(), 0));
                ticks++;
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0L, 1L);
    }

    /**
     * Returns whether the entity with the given UUID is currently electrocuted.
     *
     * @param uuid The UUID to check.
     * @return Whether the entity is electrocuted.
     */
    public static boolean isElectrocuted(UUID uuid) {
        Long expiry = ELECTROCUTED_ENTITIES.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            ELECTROCUTED_ENTITIES.remove(uuid);
            return false;
        }
        return true;
    }

    /**
     * Applies one randomly chosen spirit curse to the target entity using the
     * standard duration and amplifier shared across spiritual abilities.
     *
     * @param entity The entity to curse.
     */
    public static void applyRandomSpiritEffect(final LivingEntity entity) {
        final int idx = (int) (Math.random() * SPIRIT_EFFECT_TYPES.length);
        entity.addPotionEffect(new PotionEffect(
                SPIRIT_EFFECT_TYPES[idx],
                SPIRIT_EFFECT_DURATION,
                SPIRIT_EFFECT_AMPLIFIER,
                false, true, true));
    }

    // -------------------------------------------------------------------------
    // Sound-bending dust palette — shared across all sound abilities
    // -------------------------------------------------------------------------

    /** Turquoise ring colour used by DeafeningScream and Amplification. */
    public static final Color SOUND_COLOR_TURQUOISE = Color.fromRGB(72, 209, 204);
    /** Dark teal colour used by SonicBoom's charge particles. */
    public static final Color SOUND_COLOR_TEAL      = Color.fromRGB(0, 128, 128);
    /** Sky-blue colour used by SonicPulse's travelling rings. */
    public static final Color SOUND_COLOR_PULSE     = Color.fromRGB(100, 210, 255);

    public static final Particle.DustOptions SOUND_RING_DUST   = new Particle.DustOptions(SOUND_COLOR_TURQUOISE, 0.8f);
    public static final Particle.DustOptions SOUND_CHARGE_DUST = new Particle.DustOptions(SOUND_COLOR_TEAL,      0.5f);
    public static final Particle.DustOptions SOUND_PULSE_DUST  = new Particle.DustOptions(SOUND_COLOR_PULSE,     1.1f);

    // -------------------------------------------------------------------------
    // Sandbending dust palettes — shared across sand abilities
    // -------------------------------------------------------------------------

    private static final Particle.DustOptions[] YELLOW_SAND_PALETTE = {
            new Particle.DustOptions(Color.fromRGB(0xC2, 0xB2, 0x80), 1.2f),
            new Particle.DustOptions(Color.fromRGB(0xD2, 0xB4, 0x8C), 1.0f),
            new Particle.DustOptions(Color.fromRGB(0xE8, 0xD5, 0xA0), 0.8f),
            new Particle.DustOptions(Color.fromRGB(0xC1, 0x9A, 0x6B), 1.1f),
            new Particle.DustOptions(Color.fromRGB(0xA8, 0x96, 0x60), 0.9f),
    };
    private static final Particle.DustOptions[] RED_SAND_PALETTE = {
            new Particle.DustOptions(Color.fromRGB(0xC8, 0x5A, 0x32), 1.2f),
            new Particle.DustOptions(Color.fromRGB(0xD4, 0x6A, 0x3E), 1.0f),
            new Particle.DustOptions(Color.fromRGB(0xB8, 0x4A, 0x28), 1.1f),
            new Particle.DustOptions(Color.fromRGB(0xDC, 0x7A, 0x50), 0.9f),
            new Particle.DustOptions(Color.fromRGB(0xA0, 0x3C, 0x1E), 0.8f),
    };
    private static final Particle.DustOptions[] SOUL_SAND_PALETTE = {
            new Particle.DustOptions(Color.fromRGB(0x6B, 0x4A, 0x2E), 1.2f),
            new Particle.DustOptions(Color.fromRGB(0x7A, 0x56, 0x38), 1.0f),
            new Particle.DustOptions(Color.fromRGB(0x5C, 0x3E, 0x28), 1.1f),
            new Particle.DustOptions(Color.fromRGB(0x8A, 0x64, 0x46), 0.9f),
            new Particle.DustOptions(Color.fromRGB(0x4A, 0x32, 0x1E), 0.8f),
    };
    private static final Particle.DustOptions[] GRAVEL_PALETTE = {
            new Particle.DustOptions(Color.fromRGB(0x80, 0x80, 0x80), 1.2f),
            new Particle.DustOptions(Color.fromRGB(0x6E, 0x6E, 0x6E), 1.0f),
            new Particle.DustOptions(Color.fromRGB(0x96, 0x90, 0x8A), 1.1f),
            new Particle.DustOptions(Color.fromRGB(0x5A, 0x56, 0x52), 0.9f),
            new Particle.DustOptions(Color.fromRGB(0xAA, 0xA4, 0x9C), 0.8f),
    };

    /**
     * Returns the appropriate sand dust particle palette for the given block material.
     * Used by sandbending abilities (Sandstorm, SandWave) to colour particles by source type.
     */
    public static Particle.DustOptions[] pickSandDustPalette(Material material) {
        return switch (material) {
            case RED_SAND,
                 RED_SANDSTONE, RED_SANDSTONE_SLAB, RED_SANDSTONE_STAIRS, RED_SANDSTONE_WALL,
                 CHISELED_RED_SANDSTONE, CUT_RED_SANDSTONE, CUT_RED_SANDSTONE_SLAB,
                 SMOOTH_RED_SANDSTONE, SMOOTH_RED_SANDSTONE_STAIRS, SMOOTH_RED_SANDSTONE_SLAB -> RED_SAND_PALETTE;
            case SOUL_SAND, SOUL_SOIL -> SOUL_SAND_PALETTE;
            case GRAVEL, SUSPICIOUS_GRAVEL,
                 WHITE_CONCRETE_POWDER, ORANGE_CONCRETE_POWDER, MAGENTA_CONCRETE_POWDER,
                 LIGHT_BLUE_CONCRETE_POWDER, YELLOW_CONCRETE_POWDER, LIME_CONCRETE_POWDER,
                 PINK_CONCRETE_POWDER, GRAY_CONCRETE_POWDER, LIGHT_GRAY_CONCRETE_POWDER,
                 CYAN_CONCRETE_POWDER, PURPLE_CONCRETE_POWDER, BLUE_CONCRETE_POWDER,
                 BROWN_CONCRETE_POWDER, GREEN_CONCRETE_POWDER, RED_CONCRETE_POWDER,
                 BLACK_CONCRETE_POWDER -> GRAVEL_PALETTE;
            default -> YELLOW_SAND_PALETTE;
        };
    }

    /**
     * Returns true if the player's ability should be prevented near a Dominion where they
     * do not have the Build permission. Checks the 3x3 chunk grid centered on the player's
     * current chunk. The Build permission accounts for the player's effective rank in the
     * dominion (member rank or inter-dominion relation rank).
     *
     * @param player The player using the ability.
     * @return Whether the ability should be cancelled.
     */
    public static boolean preventAbilityNearDominion(Player player) {
        int chunkX = player.getLocation().getChunk().getX();
        int chunkZ = player.getLocation().getChunk().getZ();
        for (int x = chunkX - 1; x <= chunkX + 1; x++) {
            for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
                Chunk chunk = player.getWorld().getChunkAt(x, z);
                Dominion dominion = DominionUtils.getDominionOfChunk(chunk);
                if (dominion == null) {
                    continue;
                }
                if (!DominionUtils.hasPermission(player, dominion, DominionPermission.BUILD)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes an ability instance that fires as part of a combo's input sequence.
     * @param bPlayer The BendingPlayer whose cooldown should be cleared.
     * @param player The Player whose active ability instances are searched.
     * @param abilityName The exact ability name to remove (case-sensitive).
     */
    public static void suppressComboTrigger(BendingPlayer bPlayer, Player player, String abilityName) {
        final CoreAbility prototype = CoreAbility.getAbility(abilityName);
        if (prototype == null) return;
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            for (final CoreAbility ability : new ArrayList<>(CoreAbility.getAbilities(prototype.getClass()))) {
                if (ability.getPlayer().equals(player)) {
                    ability.remove();
                    bPlayer.removeCooldown(abilityName);
                }
            }
        });
    }

    private static final Set<Material> METAL_ARMOR_PIECES = EnumSet.of(
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
    );

    private static final Set<Material> METAL_INGOTS = EnumSet.of(
            Material.IRON_INGOT, Material.COPPER_INGOT, Material.GOLD_INGOT, Material.NETHERITE_INGOT
    );

    /**
     * Determines if the player is wearing at least 1 piece of metal armor.
     * @param player The player.
     * @return Whether the player is wearing at least 1 piece of metal armor.
     */
    public static boolean hasMetalArmor(final Player player) {
        int count = 0;
        for (final ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece != null && METAL_ARMOR_PIECES.contains(piece.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the player has at least 1 metal ingot in their inventory.
     * Accepted ingots: iron, copper, gold, netherite.
     * @param player The player.
     * @return Whether the player has at least 1 metal ingot.
     */
    public static boolean hasMetalIngot(final Player player) {
        for (final ItemStack item : player.getInventory().getContents()) {
            if (item != null && METAL_INGOTS.contains(item.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the player meets the metal requirement: wearing metal armor or carrying a metal ingot.
     * @param player The player.
     * @return Whether the player has metal armor or a metal ingot.
     */
    public static boolean hasMetalRequirement(final Player player) {
        return hasMetalArmor(player) || hasMetalIngot(player);
    }
}
