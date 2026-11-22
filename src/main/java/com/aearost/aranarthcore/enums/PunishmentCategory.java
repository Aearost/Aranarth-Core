package com.aearost.aranarthcore.enums;

import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.PunishmentUtils;

import java.util.List;

/**
 * The offence categories of the punishment system, each with the ladder of punishments that its
 * offences escalate through. Offences never expire, so every earlier offence in a category counts.
 */
public enum PunishmentCategory {

    CHAT(List.of(
            new Step(PunishmentUtils.WARN, "", false),
            new Step(PunishmentUtils.WARN, "", false),
            new Step(PunishmentUtils.MUTE, "15m", false),
            new Step(PunishmentUtils.MUTE, "1h", false),
            new Step(PunishmentUtils.MUTE, "1d", true),
            new Step(PunishmentUtils.MUTE, "1w", false),
            new Step(PunishmentUtils.MUTE, "30d", false),
            new Step(PunishmentUtils.MUTE, "-1", false))),
    MINOR_GLITCH(List.of(
            new Step(PunishmentUtils.WARN, "", true),
            new Step(PunishmentUtils.BAN, "1w", false),
            new Step(PunishmentUtils.BAN, "-1", false))),
    MAJOR_GLITCH(List.of(
            new Step(PunishmentUtils.BAN, "30d", false),
            new Step(PunishmentUtils.BAN, "-1", false))),
    HACKING(List.of(
            new Step(PunishmentUtils.BAN, "-1", false)));

    /**
     * A single rung of a category's ladder.
     *
     * @param type          The punishment type, being WARN, MUTE or BAN.
     * @param duration      The duration in the format the mute and ban commands take, or -1 when permanent.
     * @param notifyCouncil Whether the Council is alerted about the player when this step is issued.
     */
    public record Step(String type, String duration, boolean notifyCouncil) {

        /**
         * Provides the lang key suffix of the step's duration.
         */
        public String getLengthKey() {
            return switch (duration) {
                case "-1" -> "permanent";
                case "30d" -> "1mo";
                default -> duration;
            };
        }
    }

    private final List<Step> steps;

    PunishmentCategory(List<Step> steps) {
        this.steps = steps;
    }

    public List<Step> getSteps() {
        return steps;
    }

    /**
     * Provides the lowercase key used in commands and lang keys.
     */
    public String getKey() {
        return name().toLowerCase();
    }

    /**
     * Provides the category matching the key, or null if there is none.
     */
    public static PunishmentCategory fromKey(String key) {
        for (PunishmentCategory category : values()) {
            if (category.getKey().equalsIgnoreCase(key)) {
                return category;
            }
        }
        return null;
    }

    /**
     * Provides the category the punishment counts towards.
     *
     * @return The category, or null if the punishment is not an offence of any category.
     */
    public static PunishmentCategory of(Punishment punishment) {
        if (punishment.getCategory() != null) {
            return fromKey(punishment.getCategory());
        }
        return switch (punishment.getType()) {
            case PunishmentUtils.WARN, PunishmentUtils.MUTE -> CHAT;
            default -> null;
        };
    }

    /**
     * Provides how many offences of this category are in the punishment history.
     */
    public int countOffences(List<Punishment> punishments) {
        if (punishments == null) {
            return 0;
        }
        int count = 0;
        for (Punishment punishment : punishments) {
            if (of(punishment) == this) {
                count++;
            }
        }
        return count;
    }

    /**
     * Provides the index of the step that the player's next offence of this category falls on.
     */
    public int getNextStepIndex(List<Punishment> punishments) {
        return Math.min(countOffences(punishments), steps.size() - 1);
    }
}
