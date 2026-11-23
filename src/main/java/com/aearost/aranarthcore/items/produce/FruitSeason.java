package com.aearost.aranarthcore.items.produce;

import com.aearost.aranarthcore.enums.Month;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * The season of a tree fruit.
 */
public enum FruitSeason {

    // One tier per month, in calendar order from Ignivór to Obscurvór
    SPRING(2, 3, 4, 5, 5, 4, 3, 2, 2, 1, 1, 1, 1, 1, 1),
    SUMMER(1, 1, 2, 3, 4, 5, 5, 5, 4, 3, 2, 1, 1, 1, 1),
    LATE_SUMMER(1, 1, 1, 2, 3, 4, 5, 5, 5, 3, 2, 1, 1, 1, 1),
    AUTUMN(1, 1, 1, 2, 2, 3, 3, 4, 5, 5, 5, 4, 2, 1, 1),
    LATE_AUTUMN(1, 1, 1, 1, 2, 2, 3, 3, 4, 5, 5, 5, 3, 2, 1),
    WINTER(4, 3, 2, 1, 1, 1, 1, 2, 2, 3, 3, 4, 5, 5, 5),
    TROPICAL(3, 3, 3, 4, 4, 5, 5, 5, 4, 4, 3, 3, 3, 3, 3);

    /** The share of a tree's fruit that grows in each tier, from 1 (dormant) to 5 (peak). */
    private static final double[] SHARE = {0, 0.15, 0.35, 0.55, 0.8, 1.0};
    /** How quickly picked fruit grows back in each tier, compared to a month of normal growth. */
    private static final double[] SPEED = {0, 0.5, 0.75, 1.0, 1.25, 1.5};

    private final int[] tiers;

    FruitSeason(int... tiers) {
        this.tiers = tiers;
    }

    /**
     * Provides the season of a tree fruit by its produce id, such as apple or lemon.
     */
    public static FruitSeason of(String fruitId) {
        return switch (fruitId) {
            case "cherry", "apricot", "lychee" -> SPRING;
            case "peach", "plum", "mango", "lime" -> SUMMER;
            case "fig" -> LATE_SUMMER;
            case "persimmon", "olive" -> LATE_AUTUMN;
            case "lemon", "orange" -> WINTER;
            case "banana", "coconut", "avocado" -> TROPICAL;
            // Apples, pears, pomegranates and dates
            default -> AUTUMN;
        };
    }

    /**
     * Provides the share of a tree's fruit that grows in the month, from 0.15 out of season to 1 at its peak.
     */
    public double getShare(Month month) {
        return SHARE[tiers[month.ordinal()]];
    }

    /**
     * Provides how quickly picked fruit grows back in the month, compared to a month of normal growth.
     */
    public double getSpeed(Month month) {
        return SPEED[tiers[month.ordinal()]];
    }

    /**
     * Provides the months in which the fruit is at its peak, in calendar order.
     */
    public List<Month> getPeakMonths() {
        List<Month> peak = new ArrayList<>();
        for (Month month : Month.values()) {
            if (tiers[month.ordinal()] == SHARE.length - 1) {
                peak.add(month);
            }
        }
        return peak;
    }

    /**
     * Provides the lang key of the season's name.
     */
    public String getNameKey() {
        return "farmingtrees.fruit_season_" + name().toLowerCase();
    }

    /**
     * Determines if a spot of fruit bears fruit in the month.
     */
    public boolean bears(Block block, Month month) {
        return rank(block) < getShare(month);
    }

    /**
     * Provides a fixed number from 0 up to 1 for the block, spread evenly across the fruit of a tree.
     */
    private static double rank(Block block) {
        long hash = block.getX() * 73428767L ^ block.getY() * 912931L ^ block.getZ() * 391939L;
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        return (hash >>> 11) / (double) (1L << 53);
    }
}
