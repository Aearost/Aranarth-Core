package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.player.UserManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static com.aearost.aranarthcore.objects.CustomKeys.TABLE_ENCHANTED;

/**
 * Provides the shared logic for the Enchanting child skill, which is levelled by Alchemy and Repair.
 */
public class EnchantingUtils {

    public static final int MAX_LEVEL = 1000;
    private static final String CONFIG_PATH = "mcmmo.enchanting.";

    /**
     * The enchantments that can be over-leveled, both by Arcane Ascension and the Wandering Enchanter.
     */
    public static final Set<Enchantment> OVER_LEVELED_ENCHANTS = Set.of(
            Enchantment.SHARPNESS, Enchantment.EFFICIENCY, Enchantment.FORTUNE, Enchantment.PROTECTION,
            Enchantment.FEATHER_FALLING, Enchantment.LOOTING, Enchantment.FIRE_ASPECT, Enchantment.POWER,
            Enchantment.UNBREAKING, Enchantment.RESPIRATION, Enchantment.SWIFT_SNEAK, Enchantment.SOUL_SPEED,
            Enchantment.LUCK_OF_THE_SEA, Enchantment.LURE, Enchantment.FROST_WALKER, Enchantment.KNOCKBACK,
            Enchantment.PUNCH
    );

    private static final Random RANDOM = new Random();
    private static final Map<UUID, Long> readiedPlayers = new HashMap<>();
    private static final Map<UUID, Long> infiniteLibraryEnds = new HashMap<>();
    private static final Map<UUID, Location> infiniteLibraryTables = new HashMap<>();
    private static final Map<UUID, Long> infiniteLibraryDurations = new HashMap<>();

    /**
     * Provides the player's Enchanting level, being the average of their Alchemy and Repair levels.
     * @param player The player.
     * @return The Enchanting level, or 0 if their mcMMO data is not loaded.
     */
    public static int getLevel(Player player) {
        McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);
        if (mcMMOPlayer == null) {
            return 0;
        }
        return (mcMMOPlayer.getSkillLevel(PrimarySkillType.ALCHEMY) + mcMMOPlayer.getSkillLevel(PrimarySkillType.REPAIR)) / 2;
    }

    /**
     * Provides the highest level the input enchantment can reach.
     * @param enchantment The enchantment.
     * @return The over-leveled maximum.
     */
    public static int getOverLeveledMax(Enchantment enchantment) {
        return OVER_LEVELED_ENCHANTS.contains(enchantment) ? enchantment.getMaxLevel() + 2 : enchantment.getMaxLevel();
    }

    /**
     * Scales a value linearly from the unlock level to the max Enchanting level.
     * @param level The Enchanting level.
     * @param unlockLevel The level at which the minimum value applies.
     * @param min The value at the unlock level.
     * @param max The value at the max Enchanting level.
     * @return The scaled value.
     */
    public static double scale(int level, int unlockLevel, double min, double max) {
        if (level >= MAX_LEVEL || unlockLevel >= MAX_LEVEL) {
            return max;
        }
        if (level <= unlockLevel) {
            return min;
        }
        double progress = (double) (level - unlockLevel) / (MAX_LEVEL - unlockLevel);
        return min + (max - min) * progress;
    }

    /**
     * Provides the scaled value of a sub-skill's min/max config pair at the input level.
     * @param subSkill The sub-skill's config section, i.e "arcane-recall".
     * @param minKey The key of the minimum value.
     * @param maxKey The key of the maximum value.
     * @param level The Enchanting level.
     * @return The scaled value.
     */
    public static double getScaled(String subSkill, String minKey, String maxKey, int level) {
        return scale(level, getUnlockLevel(subSkill), getDouble(subSkill + "." + minKey), getDouble(subSkill + "." + maxKey));
    }

    public static int getUnlockLevel(String subSkill) {
        return getInt(subSkill + ".unlock-level");
    }

    public static boolean hasUnlocked(Player player, String subSkill) {
        return getLevel(player) >= getUnlockLevel(subSkill);
    }

    public static double getDouble(String path) {
        return AranarthCore.getInstance().getConfig().getDouble(CONFIG_PATH + path);
    }

    public static int getInt(String path) {
        return AranarthCore.getInstance().getConfig().getInt(CONFIG_PATH + path);
    }

    /**
     * Provides the portion of the EXP spent on a bottle that is stored in it.
     */
    public static double getBottledKnowledgeRatio(int level) {
        return getScaled("bottled-knowledge", "min-ratio", "max-ratio", level);
    }

    /**
     * Enchanting sub-skills only apply in the gameplay worlds, and never in creative mode.
     * @param player The player.
     * @return Whether the player can use the Enchanting sub-skills.
     */
    public static boolean canUseSkills(Player player) {
        return AranarthUtils.isSurvivalWorld(player.getWorld().getName())
                && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR;
    }

    /**
     * Provides the multiplier applied to the portion of EXP imbued into Essences by Distilled Wisdom.
     * @param player The player collecting the EXP.
     * @return The multiplier, being 1 if Distilled Wisdom does not apply.
     */
    public static double getDistilledWisdomEssenceMultiplier(Player player) {
        if (!canUseSkills(player)) {
            return 1;
        }
        int level = getLevel(player);
        if (level < getUnlockLevel("distilled-wisdom")) {
            return 1;
        }
        return getScaled("distilled-wisdom", "min-essence-multiplier", "max-essence-multiplier", level);
    }

    /**
     * Rounds the value up or down at random based on its fraction, so the average matches the exact value.
     * @param value The value.
     * @return The rounded value.
     */
    public static int randomRound(double value) {
        int rounded = (int) Math.floor(value);
        return RANDOM.nextDouble() < value - rounded ? rounded + 1 : rounded;
    }

    /**
     * Provides the levels consumed per curse when using Purification.
     */
    public static int getPurificationLevelsPerCurse(int level) {
        return (int) Math.round(scale(level, getUnlockLevel("purification"),
                getDouble("purification.max-levels-per-curse"), getDouble("purification.min-levels-per-curse")));
    }

    /**
     * Provides the duration of Infinite Library in milliseconds.
     */
    public static long getInfiniteLibraryDurationMillis(int level) {
        return (long) (getScaled("infinite-library", "min-duration-seconds", "max-duration-seconds", level) * 1000);
    }

    /**
     * Provides the EXP points held between the input level and the level the given number of levels below it.
     * @param level The current level.
     * @param levelsSpent The number of levels being removed.
     * @return The EXP points.
     */
    public static int getPointsOfLevels(int level, int levelsSpent) {
        return EssenceImbueUtils.getXpForLevel(level) - EssenceImbueUtils.getXpForLevel(Math.max(0, level - levelsSpent));
    }

    /**
     * Determines whether the item was enchanted at an enchanting table, or combined with one that was.
     * @param item The item.
     * @return Whether the item is table-enchanted.
     */
    public static boolean isTableEnchanted(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(TABLE_ENCHANTED, PersistentDataType.BYTE);
    }

    /**
     * Marks the item as having been enchanted at an enchanting table, so it cannot be ground for bonus EXP.
     * @param meta The item's meta.
     */
    public static void markTableEnchanted(ItemMeta meta) {
        meta.getPersistentDataContainer().set(TABLE_ENCHANTED, PersistentDataType.BYTE, (byte) 1);
    }

    /**
     * Readies the player's enchanting abilities until the ready time passes.
     * @param player The player.
     * @return The time at which the abilities are lowered.
     */
    public static long ready(Player player) {
        long expiry = System.currentTimeMillis() + getInt("infinite-library.ready-seconds") * 1000L;
        readiedPlayers.put(player.getUniqueId(), expiry);
        return expiry;
    }

    /**
     * Lowers the player's enchanting abilities if they are still readied from the same ready.
     * @param uuid The player's UUID.
     * @param expiry The time returned when the abilities were readied.
     * @return Whether the abilities were lowered.
     */
    public static boolean lowerIfUnused(UUID uuid, long expiry) {
        Long current = readiedPlayers.get(uuid);
        if (current != null && current == expiry) {
            readiedPlayers.remove(uuid);
            return true;
        }
        return false;
    }

    /**
     * Determines whether the player's enchanting abilities are readied, consuming the ready state if so.
     * @param player The player.
     * @return Whether the abilities were readied.
     */
    public static boolean consumeReady(Player player) {
        Long expiry = readiedPlayers.remove(player.getUniqueId());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public static boolean isReadied(Player player) {
        Long expiry = readiedPlayers.get(player.getUniqueId());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public static void clearReady(UUID uuid) {
        readiedPlayers.remove(uuid);
    }

    /**
     * Formats the enchantment for display, i.e "Sharpness".
     * @param enchantment The enchantment.
     * @return The formatted enchantment name.
     */
    public static String formatEnchantment(Enchantment enchantment) {
        return ChatUtils.getFormattedItemName(enchantment.getKey().getKey());
    }

    /**
     * Starts Infinite Library for the player at the input enchanting table.
     * @param player The player.
     * @param table The location of the enchanting table.
     * @param durationMillis The duration of the ability.
     */
    public static void startInfiniteLibrary(Player player, Location table, long durationMillis) {
        infiniteLibraryEnds.put(player.getUniqueId(), Long.MAX_VALUE);
        infiniteLibraryTables.put(player.getUniqueId(), table);
        infiniteLibraryDurations.put(player.getUniqueId(), durationMillis);
    }

    /**
     * Starts counting down the player's Infinite Library duration, if it has not started yet.
     * @param uuid The player's UUID.
     * @return The time at which the ability ends, or 0 if the countdown had already started.
     */
    public static long startInfiniteLibraryCountdown(UUID uuid) {
        Long duration = infiniteLibraryDurations.remove(uuid);
        if (duration == null) {
            return 0;
        }
        long end = System.currentTimeMillis() + duration;
        infiniteLibraryEnds.put(uuid, end);
        return end;
    }

    /**
     * Determines whether the player has Infinite Library active but has not rolled any offers with it yet.
     * @param uuid The player's UUID.
     * @return Whether the countdown is still waiting for the first roll.
     */
    public static boolean isInfiniteLibraryAwaitingRoll(UUID uuid) {
        return infiniteLibraryDurations.containsKey(uuid);
    }

    /**
     * Determines whether the player has Infinite Library active at the input enchanting table.
     * @param player The player.
     * @param table The location of the enchanting table being used.
     * @return Whether Infinite Library is active at that table.
     */
    public static boolean isInfiniteLibraryActive(Player player, Location table) {
        Long end = infiniteLibraryEnds.get(player.getUniqueId());
        if (end == null || end <= System.currentTimeMillis()) {
            return false;
        }
        Location activeTable = infiniteLibraryTables.get(player.getUniqueId());
        return activeTable != null && table != null && activeTable.getBlock().equals(table.getBlock());
    }

    public static boolean isInfiniteLibraryRunning(UUID uuid) {
        Long end = infiniteLibraryEnds.get(uuid);
        return end != null && end > System.currentTimeMillis();
    }

    public static void endInfiniteLibrary(UUID uuid) {
        infiniteLibraryEnds.remove(uuid);
        infiniteLibraryTables.remove(uuid);
        infiniteLibraryDurations.remove(uuid);
    }

    /**
     * Ends Infinite Library only if it is still the activation that ends at the input time.
     * @param uuid The player's UUID.
     * @param end The time returned when the countdown was started.
     * @return Whether the ability was ended.
     */
    public static boolean endInfiniteLibrary(UUID uuid, long end) {
        Long current = infiniteLibraryEnds.get(uuid);
        if (current == null || current != end) {
            return false;
        }
        endInfiniteLibrary(uuid);
        return true;
    }
}
