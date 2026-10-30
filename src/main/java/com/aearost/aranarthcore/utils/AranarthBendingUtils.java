package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.Outpost;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.waterbending.util.WaterReturn;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AranarthBendingUtils {

    /**
     * Ordered block progression used by lava warm-up abilities.
     */
    public static final Material[] LAVA_WARMUP_SEQUENCE = {
            Material.STONE,
            Material.GRANITE,
            Material.NETHERRACK,
            Material.MAGMA_BLOCK,
    };

    /**
     * The four spirit colours used by spirit abilities.
     */
    public static final Color[] SPIRIT_COLORS = {
            Color.fromRGB(220, 30, 30),   // Red - Weakness
            Color.fromRGB(30, 100, 220),  // Blue - Slowness
            Color.fromRGB(140, 30, 200),  // Purple - Blindness
            Color.fromRGB(220, 200, 30),   // Yellow - Nausea
    };

    /**
     * Matching potion effects for each entry in the spirit colors.
     */
    public static final PotionEffectType[] SPIRIT_EFFECT_TYPES = {
            PotionEffectType.WEAKNESS,
            PotionEffectType.SLOWNESS,
            PotionEffectType.BLINDNESS,
            PotionEffectType.NAUSEA,
    };

    public static final int SPIRIT_EFFECT_DURATION = 100;
    public static final int SPIRIT_EFFECT_AMPLIFIER = 0;

    /**
     * Turquoise colour used by soundbending abilities.
     */
    public static final Color SOUND_COLOR_TURQUOISE = Color.fromRGB(72, 209, 204);
    /**
     * Dark teal colour used by soundbending abilities.
     */
    public static final Color SOUND_COLOR_TEAL = Color.fromRGB(0, 128, 128);
    /**
     * Sky-blue colour used by soundbending abilities.
     */
    public static final Color SOUND_COLOR_PULSE = Color.fromRGB(100, 210, 255);

    /**
     * Primary petal colour of every flower recognized by AranarthUtils.isFlower().
     */
    public static final Map<Material, Color> FLOWER_PETAL_COLORS = Map.ofEntries(
            Map.entry(Material.DANDELION, Color.fromRGB(0xF5D63A)),
            Map.entry(Material.POPPY, Color.fromRGB(0xED302C)),
            Map.entry(Material.BLUE_ORCHID, Color.fromRGB(0x2ABFFD)),
            Map.entry(Material.ALLIUM, Color.fromRGB(0xB878ED)),
            Map.entry(Material.AZURE_BLUET, Color.fromRGB(0xE6EBEC)),
            Map.entry(Material.RED_TULIP, Color.fromRGB(0xEB4A3F)),
            Map.entry(Material.ORANGE_TULIP, Color.fromRGB(0xF0862F)),
            Map.entry(Material.WHITE_TULIP, Color.fromRGB(0xF2F2F2)),
            Map.entry(Material.PINK_TULIP, Color.fromRGB(0xF4B8D9)),
            Map.entry(Material.OXEYE_DAISY, Color.fromRGB(0xF5F2E8)),
            Map.entry(Material.CORNFLOWER, Color.fromRGB(0x466AEB)),
            Map.entry(Material.LILY_OF_THE_VALLEY, Color.fromRGB(0xFFFFFF)),
            Map.entry(Material.TORCHFLOWER, Color.fromRGB(0xE8752C)),
            Map.entry(Material.PITCHER_PLANT, Color.fromRGB(0x6C8FD3)),
            Map.entry(Material.CACTUS_FLOWER, Color.fromRGB(0xF07EB4)),
            Map.entry(Material.OPEN_EYEBLOSSOM, Color.fromRGB(0xF59A3A)),
            Map.entry(Material.CLOSED_EYEBLOSSOM, Color.fromRGB(0x8A8A9C)),
            Map.entry(Material.WITHER_ROSE, Color.fromRGB(0x2B2B2B)),
            Map.entry(Material.PINK_PETALS, Color.fromRGB(0xF5A9C8)),
            Map.entry(Material.WILDFLOWERS, Color.fromRGB(0xF2D64B)),
            Map.entry(Material.SPORE_BLOSSOM, Color.fromRGB(0xE06FA8)),
            Map.entry(Material.SUNFLOWER, Color.fromRGB(0xFFCA2A)),
            Map.entry(Material.LILAC, Color.fromRGB(0xD29BE6)),
            Map.entry(Material.ROSE_BUSH, Color.fromRGB(0xD3262C)),
            Map.entry(Material.PEONY, Color.fromRGB(0xEBB5E5)),
            Map.entry(Material.CHORUS_FLOWER, Color.fromRGB(0x9D7FAE))
    );

    /**
     * Gets the primary petal colour of the given flower.
     *
     * @param type The flower material.
     * @return The petal colour, or white if the material is not a known flower.
     */
    public static Color getFlowerPetalColor(Material type) {
        return FLOWER_PETAL_COLORS.getOrDefault(type, Color.WHITE);
    }

    /**
     * Hex colour used for the action bar electrocution message, matching LIGHTNING_DUST.
     */
    private static final TextColor ELECTROCUTION_COLOR = TextColor.fromHexString("#FFF050");

    /**
     * Dark crimson dust used for bloodbending particle effects.
     */
    public static final Particle.DustOptions BLOOD_DUST =
            new Particle.DustOptions(Color.fromRGB(170, 8, 8), 1.3f);
    /**
     * Bright red dust used for bursts and impacts in bloodbending abilities.
     */
    public static final Particle.DustOptions BLOOD_DUST_BRIGHT =
            new Particle.DustOptions(Color.fromRGB(230, 35, 35), 0.9f);

    /**
     * Bright blue water dust shared by healing waterbending abilities during their water phase.
     */
    public static final Particle.DustOptions WATER_DUST =
            new Particle.DustOptions(Color.fromRGB(40, 120, 255), 1.0f);
    /**
     * Darker blue water dust shared by healing waterbending abilities during their water phase.
     */
    public static final Particle.DustOptions WATER_DUST_DARK =
            new Particle.DustOptions(Color.fromRGB(20, 75, 225), 0.8f);

    /**
     * Warm yellow dust used for lightning abilities.
     */
    public static final Particle.DustOptions LIGHTNING_DUST =
            new Particle.DustOptions(Color.fromRGB(255, 240, 80), 0.8f);
    /**
     * Pale yellow-white dust used for lightning abilities.
     */
    public static final Particle.DustOptions LIGHTNING_DUST_BRIGHT =
            new Particle.DustOptions(Color.fromRGB(255, 255, 180), 1.0f);
    /**
     * Light blue dust used for the lightning abilities.
     */
    public static final Particle.DustOptions LIGHTNING_DUST_BLUE =
            new Particle.DustOptions(Color.fromRGB(100, 200, 255), 1.0f);

    public static final Particle.DustOptions SOUND_RING_DUST = new Particle.DustOptions(SOUND_COLOR_TURQUOISE, 0.8f);
    public static final Particle.DustOptions SOUND_CHARGE_DUST = new Particle.DustOptions(SOUND_COLOR_TEAL, 0.5f);
    public static final Particle.DustOptions SOUND_PULSE_DUST = new Particle.DustOptions(SOUND_COLOR_PULSE, 1.1f);

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

    private static final Map<UUID, Long> ELECTROCUTED_ENTITIES = new HashMap<>();

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
     * Returns the appropriate sand dust particle palette for the given block material.
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
     * Applies electrocution effects to a living entity.
     *
     * @param target     The entity to electrocute.
     * @param durationMs How long the stun lasts in milliseconds, if it triggers.
     * @param chance     Probability [0.0–1.0] that the stun is applied.
     */
    public static void applyElectrocution(LivingEntity target, long durationMs, double chance) {
        if (!target.isDead()) {
            if (target instanceof Pig pig) {
                pig.getWorld().spawn(pig.getLocation(), PigZombie.class);
                pig.remove();
                return;
            } else if (target instanceof Villager villager) {
                villager.getWorld().spawn(villager.getLocation(), Witch.class);
                villager.remove();
                return;
            } else if (target instanceof Creeper creeper) {
                creeper.setPowered(true);
            }
        }

        // If it will not electrocute, skip rest of logic
        if (Math.random() >= chance) {
            return;
        }

        UUID uuid = target.getUniqueId();
        ELECTROCUTED_ENTITIES.put(uuid, System.currentTimeMillis() + durationMs);
        int stunTicks = (int) (durationMs / 50L);

        if (target instanceof Player p) {
            p.sendActionBar(Component.text(Lang.getFor(p, "bending.electrocuted")).color(ELECTROCUTION_COLOR));
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
        if (expiry == null) {
            return false;
        }
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

    /**
     * Returns true if bending should be blocked for the player at the given location.
     *
     * @param player   The player to check.
     * @param location The location whose chunk is checked.
     * @return Whether bending should be blocked at that location.
     */
    public static boolean isBendingBlockedAtLocation(Player player, Location location) {
        Chunk chunk = location.getChunk();
        Outpost outpost = OutpostUtils.getOutpostOfChunk(chunk);
        if (outpost != null) {
            return !outpost.isBendingEnabled();
        }
        Dominion dominion = DominionUtils.getDominionOfChunk(chunk);
        if (dominion == null) {
            return false;
        }
        return !dominion.isBendingEnabled();
    }

    /**
     * Returns true if the player may permanently change the block with an ability.
     *
     * @param player The player using the ability.
     * @param block  The block being changed.
     * @return Whether the block may be changed.
     */
    public static boolean canBendBlock(Player player, Block block) {
        if (AranarthUtils.getPlayer(player.getUniqueId()).isInAdminMode()) {
            return true;
        }
        Location location = block.getLocation();
        if (AranarthUtils.isSpawnLocation(location)) {
            return false;
        }
        if (block.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD)) {
            UUID owner = ShopIslandUtils.getIslandOwnerAtLocation(location);
            return owner != null && (owner.equals(player.getUniqueId())
                    || AranarthUtils.isShopCollaborator(owner, player.getUniqueId()));
        }
        if (PlotUtils.isPlotWorld(block.getWorld())) {
            return PlotUtils.canBuild(player, location);
        }
        Dominion dominion = DominionUtils.getDominionOfChunkAnywhere(block.getChunk());
        return dominion == null || DominionUtils.hasPermission(player, dominion, DominionPermission.BUILD);
    }

    /**
     * Deals ability damage that ignores armor and invulnerability frames without knocking the target
     * back. Unlike setting health directly, this fires the damage events, so protection, combat tagging
     * and kill attribution all apply.
     *
     * @param target  The entity being damaged.
     * @param damage  The amount of damage.
     * @param ability The ability dealing the damage.
     * @return Whether the damage landed, which is false when a protection listener cancelled it.
     */
    public static boolean damageIgnoringArmor(LivingEntity target, double damage, Ability ability) {
        double healthBefore = target.getHealth() + target.getAbsorptionAmount();
        Vector velocity = target.getVelocity();
        target.setNoDamageTicks(0);
        DamageHandler.damageEntity(target, damage, ability, true);
        target.setVelocity(velocity);
        return target.isDead() || target.getHealth() + target.getAbsorptionAmount() < healthBefore;
    }

    /**
     * Returns true if the player's ability should be prevented near a Dominion where they
     * do not have the Build permission.
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
                Dominion dominion = DominionUtils.getDominionOfChunkAnywhere(chunk);
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
     *
     * @param bPlayer     The BendingPlayer whose cooldown should be cleared.
     * @param player      The Player whose active ability instances are searched.
     * @param abilityName The exact ability name to remove (case-sensitive).
     */
    public static void suppressComboTrigger(BendingPlayer bPlayer, Player player, String abilityName) {
        final CoreAbility prototype = CoreAbility.getAbility(abilityName);
        if (prototype == null) {
            return;
        }
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            for (final CoreAbility ability : new ArrayList<>(CoreAbility.getAbilities(prototype.getClass()))) {
                if (ability.getPlayer().equals(player)) {
                    ability.remove();
                    bPlayer.removeCooldown(abilityName);
                }
            }
        });
    }

    /**
     * Determines if the player is wearing at least 1 piece of metal armor.
     *
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
     *
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
     *
     * @param player The player.
     * @return Whether the player has metal armor or a metal ingot.
     */
    public static boolean hasMetalRequirement(final Player player) {
        return hasMetalArmor(player) || hasMetalIngot(player);
    }

    /**
     * Refills one empty glass bottle in the player's inventory with water.
     */
    public static void fillWaterBottle(Player player) {
        PlayerInventory inventory = player.getInventory();
        int index = inventory.first(Material.GLASS_BOTTLE);
        if (index < 0) {
            return;
        }

        ItemStack item = inventory.getItem(index);
        ItemStack water = WaterReturn.waterBottleItem();
        if (item.getAmount() == 1) {
            inventory.setItem(index, water);
        } else {
            item.setAmount(item.getAmount() - 1);
            inventory.setItem(index, item);
            Map<Integer, ItemStack> leftover = inventory.addItem(water);
            leftover.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }

    /**
     * Returns true if the material is a valid plant-bending source block.
     * Mirrors the categories that ProjectKorra considers plant-bendable.
     */
    public static boolean isPlantSource(Material mat) {
        if (mat.name().endsWith("_LEAVES")) {
            return true;
        }
        return switch (mat) {
            case GRASS_BLOCK, SHORT_GRASS, TALL_GRASS, FERN, LARGE_FERN,
                 VINE, GLOW_LICHEN, LILY_PAD, SEAGRASS, TALL_SEAGRASS,
                 HANGING_ROOTS, BIG_DRIPLEAF, SMALL_DRIPLEAF -> true;
            default -> AranarthUtils.isFlower(mat);
        };
    }
}
