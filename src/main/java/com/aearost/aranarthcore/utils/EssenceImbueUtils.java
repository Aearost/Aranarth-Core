package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.aearost.aranarthcore.objects.CustomKeys.ESSENCE_IMBUED_XP;

/**
 * Provides the shared logic for imbuing blank Essences with XP and crafting essences from them.
 */
public class EssenceImbueUtils {

    public static final int REQUIRED_RANK = 6;
    public static final double MACE_DURABILITY_COST = 0.25; // As percent
    public static final int MAX_IMBUED_LEVELS = 250;

    /**
     * The tiers of essences that can be crafted, in ascending order of required XP.
     */
    public enum Tier {
        RARE("essence.tier_rare", 10),
        EPIC("essence.tier_epic", 30),
        GODLY("essence.tier_godly", 100);

        private final String langKey;
        private final int percent;

        Tier(String langKey, int percent) {
            this.langKey = langKey;
            this.percent = percent;
        }

        public String getLangKey() {
            return langKey;
        }

        /**
         * @return The percentage of the maximum imbued XP required for this tier.
         */
        public int getPercent() {
            return percent;
        }

        /**
         * @return The total XP points required to be imbued for this tier.
         */
        public int getRequiredXp() {
            return (int) Math.ceil(getMaxImbuedXp() * percent / 100.0);
        }
    }

    /**
     * Provides the tier of the input essence type, based on the crate it is obtained from.
     * @param essenceType The essence type, i.e "essence_beheading".
     * @return The tier, or null if the essence type cannot be crafted.
     */
    public static Tier getTier(String essenceType) {
        if (essenceType == null) {
            return null;
        }
        return switch (essenceType) {
            case "essence_beheading" -> Tier.RARE;
            case "essence_lifesteal", "essence_magnetism" -> Tier.EPIC;
            case "essence_resilience", "essence_preservation", "essence_plentiful" -> Tier.GODLY;
            default -> null;
        };
    }

    /**
     * Provides the total XP points needed to reach the input level from level 0.
     * @param level The level.
     * @return The total XP points.
     */
    public static int getXpForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    /**
     * @return The maximum XP that can be imbued into an Essence.
     */
    public static int getMaxImbuedXp() {
        return getXpForLevel(MAX_IMBUED_LEVELS);
    }

    /**
     * Determines if the input item is a blank Essence.
     * @param item The item.
     * @return Whether the item is a blank Essence.
     */
    public static boolean isBlankEssence(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(ESSENCE_IMBUED_XP, PersistentDataType.INTEGER);
    }

    /**
     * Provides the XP imbued into the blank Essence.
     * @param item The blank Essence.
     * @return The imbued XP, or 0 if the item is not a blank Essence.
     */
    public static int getImbuedXp(ItemStack item) {
        if (!isBlankEssence(item)) {
            return 0;
        }
        Integer xp = item.getItemMeta().getPersistentDataContainer().get(ESSENCE_IMBUED_XP, PersistentDataType.INTEGER);
        return xp == null ? 0 : xp;
    }

    /**
     * Updates the XP imbued into the blank Essence, along with its lore.
     * @param item The blank Essence.
     * @param xp The new imbued XP.
     */
    public static void setImbuedXp(ItemStack item, int xp) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ESSENCE_IMBUED_XP, PersistentDataType.INTEGER, xp);
        meta.setLore(buildLore(xp));
        item.setItemMeta(meta);
    }

    /**
     * Provides the percentage of the maximum imbued XP, formatted for display.
     * @param xp The XP.
     * @return The formatted percentage, i.e "17.73".
     */
    public static String formatPercent(int xp) {
        // Rounds down so a percentage is never displayed before it has been reached
        double percent = Math.min(100.0, Math.floor(xp * 10000.0 / getMaxImbuedXp()) / 100.0);
        return String.format(Locale.US, "%.2f", percent);
    }

    /**
     * Builds the lore of a blank Essence with the input imbued XP.
     * @param xp The imbued XP.
     * @return The lore.
     */
    public static List<String> buildLore(int xp) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Imbued XP: &d" + formatPercent(xp) + "%"));

        int barLength = 30;
        int filled = (int) Math.floor(barLength * Math.min(1.0, (double) xp / getMaxImbuedXp()));
        lore.add(ChatUtils.translateToColor("&8[&d" + "|".repeat(filled) + "&7" + "|".repeat(barLength - filled) + "&8]"));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7&oHalf of your earned XP is imbued"));
        lore.add(ChatUtils.translateToColor("&7&owhile held in your inventory"));
        return lore;
    }

    /**
     * Determines if the player is of a high enough rank to imbue Essences and craft essences from them.
     * @param player The player.
     * @return Whether the player meets the rank requirement.
     */
    public static boolean hasRequiredRank(Player player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        return aranarthPlayer != null && aranarthPlayer.getRank() >= REQUIRED_RANK;
    }

    /**
     * Provides the lang key suffix of the rank required to imbue Essences, in the player's pronouns.
     * @param player The player.
     * @return The lang key suffix of the required rank.
     */
    public static String getRequiredRankId(Player player) {
        return AranarthUtils.getPlayer(player.getUniqueId()).getPronouns().getRankId(REQUIRED_RANK);
    }
}
