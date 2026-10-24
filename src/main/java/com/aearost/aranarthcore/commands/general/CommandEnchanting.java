package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.EnchantingUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.text.TextComponentFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Displays the Enchanting child skill in the same format as the mcMMO skill commands, along with its guide.
 */
public class CommandEnchanting implements CommandExecutor {

    private static final int GUIDE_SECTIONS = 5;
    private static final int LINES_PER_PAGE = 8;

    private final DecimalFormat percent = new DecimalFormat("##0.00%", DecimalFormatSymbols.getInstance(Locale.US));
    private final DecimalFormat decimal = new DecimalFormat("##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    /**
     * The sub-skills in display order, with their config section and mcMMO type tag.
     */
    private enum SubSkill {
        BOTTLED_KNOWLEDGE("bottled-knowledge", "bottled_knowledge", "JSON.Type.Active"),
        ARCANE_RECALL("arcane-recall", "arcane_recall", "JSON.Type.Passive"),
        ARCANE_ASCENSION("arcane-ascension", "arcane_ascension", "JSON.Type.Passive"),
        PURIFICATION("purification", "purification", "JSON.Type.Active"),
        DISTILLED_WISDOM("distilled-wisdom", "distilled_wisdom", "JSON.Type.Passive"),
        INFINITE_LIBRARY("infinite-library", "infinite_library", "JSON.Type.SuperAbility");

        private final String configKey;
        private final String langKey;
        private final String typeKey;

        SubSkill(String configKey, String langKey, String typeKey) {
            this.configKey = configKey;
            this.langKey = langKey;
            this.typeKey = typeKey;
        }

        private int getUnlockLevel() {
            return EnchantingUtils.getUnlockLevel(configKey);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
            return true;
        }

        if (args.length == 0) {
            McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);
            if (mcMMOPlayer == null) {
                player.sendMessage(LocaleLoader.getString("Profile.PendingLoad"));
                return true;
            }
            sendSkillCommand(player, mcMMOPlayer);
        } else if (args[0].equals("?")) {
            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(LocaleLoader.getString("Guides.Page.Invalid"));
                    return true;
                }
            }
            sendGuide(player, page);
        } else {
            player.sendMessage(LocaleLoader.getString("Guides.Usage", "enchanting"));
        }
        return true;
    }

    /**
     * Sends the skill overview, mirroring the layout of the mcMMO child skill commands.
     */
    private void sendSkillCommand(Player player, McMMOPlayer mcMMOPlayer) {
        int level = EnchantingUtils.getLevel(player);
        String skillName = Lang.getDefault("enchanting.skill_name");

        if (sendsBlankLines()) {
            player.sendMessage("");
            player.sendMessage("");
        }

        // Header
        player.sendMessage(LocaleLoader.getString("Skills.Overhaul.Header", skillName));
        player.sendMessage(LocaleLoader.getString("Commands.XPGain.Overhaul", LocaleLoader.getString("Commands.XPGain.Child")));
        String parents = LocaleLoader.getString("Effects.Child.ParentList", LocaleLoader.getString("Alchemy.SkillName"),
                mcMMOPlayer.getSkillLevel(PrimarySkillType.ALCHEMY))
                + ChatUtils.translateToColor("&7, ")
                + LocaleLoader.getString("Effects.Child.ParentList", LocaleLoader.getString("Repair.SkillName"),
                mcMMOPlayer.getSkillLevel(PrimarySkillType.REPAIR));
        player.sendMessage(LocaleLoader.getString("Effects.Child.Overhaul", level, parents));

        // Sub-skills, 3 per line
        player.sendMessage(LocaleLoader.getString("Skills.Overhaul.Header", LocaleLoader.getString("Effects.SubSkills.Overhaul")));
        Component atSign = legacy(LocaleLoader.getString("JSON.Hover.AtSymbolSkills"));
        SubSkill[] subSkills = SubSkill.values();
        for (int i = 0; i < subSkills.length; i += 3) {
            TextComponent.Builder line = Component.text();
            for (int j = i; j < Math.min(i + 3, subSkills.length); j++) {
                line.append(atSign).append(buildSubSkillButton(player, subSkills[j], level)).append(Component.space());
            }
            player.sendMessage(line.build());
        }

        // Stats
        List<String> stats = getStats(player, level);
        if (!stats.isEmpty()) {
            player.sendMessage(LocaleLoader.getString("Skills.Overhaul.Header", LocaleLoader.getString("Commands.Stats.Self.Overhaul")));
            for (String stat : stats) {
                player.sendMessage(stat);
            }
        }
        player.sendMessage(LocaleLoader.getString("Guides.Available", skillName, "enchanting"));

        try {
            if (mcMMO.p.getGeneralConfig().getUrlLinksEnabled()) {
                player.sendMessage(LocaleLoader.getString("Overhaul.mcMMO.Header"));
                TextComponentFactory.sendPlayerUrlHeader(player);
            }
        } catch (Throwable ignored) {
            // The mcMMO internals changed, so the links are skipped
        }
    }

    private boolean sendsBlankLines() {
        try {
            return mcMMO.p.getAdvancedConfig().doesSkillCommandSendBlankLines();
        } catch (Throwable ignored) {
            return true;
        }
    }

    /**
     * Builds the sub-skill name with its hover tooltip, matching the mcMMO sub-skill buttons.
     */
    private Component buildSubSkillButton(Player player, SubSkill subSkill, int level) {
        int unlockLevel = subSkill.getUnlockLevel();
        if (level < unlockLevel) {
            String mystery = LocaleLoader.getString("JSON.Hover.Mystery", String.valueOf(unlockLevel));
            Component hover = Component.text()
                    .append(legacy(mystery)).append(Component.newline())
                    .append(legacy(LocaleLoader.getString("JSON.Locked")))
                    .append(Component.newline()).append(Component.newline())
                    .append(legacy(LocaleLoader.getString("JSON.LevelRequirement")))
                    .append(Component.text(": " + unlockLevel))
                    .build();
            return legacy(mystery).hoverEvent(HoverEvent.showText(hover));
        }

        String name = Lang.getDefault("enchanting." + subSkill.langKey + "_name");
        // Every sub-skill scales up to the max level, at which point it is shown as fully ranked
        String nameKey = level >= EnchantingUtils.MAX_LEVEL ? "JSON.Hover.MaxRankSkillName" : "JSON.Hover.SkillName";
        Component hover = Component.text()
                .append(legacy(LocaleLoader.getString(nameKey, name))).append(Component.newline())
                .append(legacy(LocaleLoader.getString(subSkill.typeKey))).append(Component.newline())
                .append(Component.newline())
                .append(legacy(LocaleLoader.getString("JSON.DescriptionHeader"))).append(Component.newline())
                .append(legacy(ChatUtils.translateToColor(Lang.getDefault("enchanting." + subSkill.langKey + "_description"))))
                .build();
        return legacy(LocaleLoader.getString(nameKey, name)).hoverEvent(HoverEvent.showText(hover));
    }

    /**
     * Provides the stat lines of every unlocked sub-skill.
     */
    private List<String> getStats(Player player, int level) {
        List<String> stats = new ArrayList<>();
        if (level >= SubSkill.BOTTLED_KNOWLEDGE.getUnlockLevel()) {
            stats.add(stat(player, "stat_bottled_knowledge", percent.format(EnchantingUtils.getBottledKnowledgeRatio(level))));
        }

        if (level >= SubSkill.ARCANE_RECALL.getUnlockLevel()) {
            stats.add(stat(player, "stat_arcane_recall_chance",
                    percent.format(EnchantingUtils.getScaled("arcane-recall", "min-chance", "max-chance", level))));
            stats.add(stat(player, "stat_arcane_recall_refund",
                    percent.format(EnchantingUtils.getScaled("arcane-recall", "min-refund", "max-refund", level))));
        }
        if (level >= SubSkill.ARCANE_ASCENSION.getUnlockLevel()) {
            stats.add(stat(player, "stat_arcane_ascension_chance",
                    percent.format(EnchantingUtils.getScaled("arcane-ascension", "min-chance", "max-chance", level))));
        }
        if (level >= SubSkill.PURIFICATION.getUnlockLevel()) {
            stats.add(stat(player, "stat_purification_chance",
                    percent.format(EnchantingUtils.getScaled("purification", "min-chance", "max-chance", level))));
            stats.add(stat(player, "stat_purification_cost",
                    Lang.getDefault("enchanting.stat_purification_cost_value", "levels", EnchantingUtils.getPurificationLevelsPerCurse(level))));
        }
        if (level >= SubSkill.DISTILLED_WISDOM.getUnlockLevel()) {
            stats.add(stat(player, "stat_distilled_wisdom_multiplier",
                    decimal.format(EnchantingUtils.getScaled("distilled-wisdom", "min-multiplier", "max-multiplier", level)) + "x"));
            stats.add(stat(player, "stat_distilled_wisdom_essence",
                    decimal.format(EnchantingUtils.getScaled("distilled-wisdom", "min-essence-multiplier", "max-essence-multiplier", level)) + "x"));
        }
        if (level >= SubSkill.INFINITE_LIBRARY.getUnlockLevel()) {
            stats.add(stat(player, "stat_infinite_library_length",
                    decimal.format(EnchantingUtils.getInfiniteLibraryDurationMillis(level) / 1000.0) + "s"));
        }
        return stats;
    }

    private String stat(Player player, String key, String value) {
        return LocaleLoader.getString("Ability.Generic.Template", ChatUtils.translateToColor(Lang.getDefault("enchanting." + key)), value);
    }

    /**
     * Sends a page of the guide, mirroring the layout of the mcMMO skill guides.
     */
    private void sendGuide(Player player, int page) {
        List<String> guide = new ArrayList<>();
        for (int i = 0; i < GUIDE_SECTIONS; i++) {
            String section = Lang.getDefault("enchanting.guide_section_" + i,
                    "lapis", EnchantingUtils.getInt("infinite-library.lapis-per-roll"));
            List<String> lines = new ArrayList<>(Arrays.asList(section.split("\n")));
            while (lines.size() < LINES_PER_PAGE) {
                lines.add("");
            }
            guide.addAll(lines);
        }

        int totalPages = (int) Math.ceil(guide.size() / (double) LINES_PER_PAGE);
        if (page > totalPages || page <= 0) {
            player.sendMessage(LocaleLoader.getString("Guides.Page.OutOfRange", totalPages));
            return;
        }

        player.sendMessage(LocaleLoader.getString("Guides.Header", Lang.getDefault("enchanting.skill_name")));
        int start = LINES_PER_PAGE * (page - 1);
        for (int i = start; i < start + LINES_PER_PAGE; i++) {
            player.sendMessage(i < guide.size() ? ChatUtils.translateToColor(guide.get(i)) : "");
        }
        player.sendMessage(ChatUtils.translateToColor(Lang.getDefault("enchanting.guide_page", "page", page, "total", totalPages)));
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }
}
