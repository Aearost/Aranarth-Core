package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;

public class AranarthBendingUtils {

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
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            for (final CoreAbility ability : new ArrayList<>(CoreAbility.getAbilities(player))) {
                if (ability.getName().equals(abilityName)) {
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
}
