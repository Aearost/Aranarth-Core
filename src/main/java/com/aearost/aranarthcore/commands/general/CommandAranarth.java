package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.gui.GuiAranarth;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import com.aearost.aranarthcore.utils.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

/**
 * Centralised guide command for Aranarth topics.
 */
public class CommandAranarth implements CommandExecutor {

    static final List<String> CALENDAR_MONTHS = List.of(
            "ignivor", "aquinvor", "ventivor", "florivor", "aestivor",
            "calorvor", "ardorvor", "solarvor", "follivor", "strigavor",
            "faunivor", "umbravor", "glacivor", "frigorvor", "obscurvor"
    );

    static final List<String> ARANARTHIUM_SECTIONS = List.of(
            "intro", "ingots", "elytra", "aquatic", "ardent",
            "dwarven", "elven", "fae", "scorched", "soulbound"
    );

    static final List<String> INCANTATION_SECTIONS = List.of(
            "overview", "applying", "beheading", "lifesteal",
            "plentiful", "magnetism", "resilience", "preservation"
    );

    private static final Set<String> ARANARTHIUM_SET = Set.copyOf(ARANARTHIUM_SECTIONS);
    private static final Set<String> INCANTATION_SET = Set.copyOf(INCANTATION_SECTIONS);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }

        if (args.length == 0) {
            sendMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "calendar" -> {
                if (args.length == 1) sendCalendarMenu(player);
                else sendCalendarChapter(player, args[1].toLowerCase());
            }
            case "aranarthium" -> {
                if (args.length == 1) sendAranarthiumMenu(player);
                else sendAranarthiumChapter(player, args[1].toLowerCase());
            }
            case "incantations" -> {
                if (args.length == 1) sendIncantationsMenu(player);
                else sendIncantationsChapter(player, args[1].toLowerCase());
            }
            case "rules" -> sendRules(player);
            default -> player.sendMessage(ChatUtils.chatMessage(
                    Lang.get("guide.invalid_topic", "topic", args[0])));
        }
        return true;
    }

    private void sendMainMenu(Player player) {
        new GuiAranarth(player).openGui();
    }

    private void sendCalendarMenu(Player player) {
        player.sendMessage(ChatUtils.translateToColor(Lang.get("calendar.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("  " + Lang.get("calendar.guide_select_month")));
        player.sendMessage(Component.empty());

        String[] months = CALENDAR_MONTHS.toArray(new String[0]);
        for (int i = 0; i < months.length; i += 3) {
            int end = Math.min(i + 3, months.length);
            String[][] rowData = new String[end - i][];
            for (int j = i; j < end; j++) {
                String month = months[j];
                rowData[j - i] = new String[]{
                        Lang.get("calendar." + month + "_name"),
                        "&7Click to learn about " + Lang.get("calendar." + month + "_name"),
                        "/aranarth calendar " + month
                };
            }
            player.sendMessage(Component.text("  ").append(buildRow(rowData)));
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("              ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
    }

    private void sendCalendarChapter(Player player, String month) {
        int monthNum = CALENDAR_MONTHS.indexOf(month) + 1;
        if (monthNum == 0) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("guide.invalid_chapter", "chapter", month)));
            return;
        }

        String name = Lang.get("calendar." + month + "_name");
        String subtitle = Lang.get("calendar." + month + "_subtitle");
        String desc = Lang.get("calendar." + month + "_desc");

        int prevIdx = (monthNum - 2 + CALENDAR_MONTHS.size()) % CALENDAR_MONTHS.size();
        int nextIdx = monthNum % CALENDAR_MONTHS.size();
        String prevMonth = CALENDAR_MONTHS.get(prevIdx);
        String nextMonth = CALENDAR_MONTHS.get(nextIdx);

        int days = DateUtils.getDaysInMonth(Month.valueOf(month.toUpperCase()));
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(ChatUtils.translateToColor("  " + name + " &8(&7" + monthNum + "&8) &7- " + subtitle + " &8| &7" + days + " days"));
        player.sendMessage(Component.empty());
        sendMultilineIndented(player, desc);
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.get("guide.nav_prev"), Lang.get("calendar." + prevMonth + "_name"), "/aranarth calendar " + prevMonth))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("calendar.guide_back"), "/aranarth calendar"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("guide.nav_next"), Lang.get("calendar." + nextMonth + "_name"), "/aranarth calendar " + nextMonth)));
    }

    private void sendAranarthiumMenu(Player player) {
        player.sendMessage(ChatUtils.translateToColor(Lang.get("aranarthium.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("  " + Lang.get("aranarthium.guide_select_section")));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("  ").append(buildRow(new String[][]{
                {Lang.get("aranarthium.section_intro_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium intro"},
                {Lang.get("aranarthium.section_ingots_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium ingots"},
                {Lang.get("aranarthium.section_elytra_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium elytra"}
        })));
        player.sendMessage(Component.text("  ").append(buildRow(new String[][]{
                {Lang.get("aranarthium.section_aquatic_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium aquatic"},
                {Lang.get("aranarthium.section_ardent_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium ardent"},
                {Lang.get("aranarthium.section_dwarven_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium dwarven"},
                {Lang.get("aranarthium.section_elven_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium elven"}
        })));
        player.sendMessage(Component.text("      ").append(buildRow(new String[][]{
                {Lang.get("aranarthium.section_fae_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium fae"},
                {Lang.get("aranarthium.section_scorched_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium scorched"},
                {Lang.get("aranarthium.section_soulbound_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium soulbound"}
        })));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("              ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
    }

    private void sendAranarthiumChapter(Player player, String section) {
        if (!ARANARTHIUM_SET.contains(section)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("guide.invalid_chapter", "chapter", section)));
            return;
        }

        int sectionIdx = ARANARTHIUM_SECTIONS.indexOf(section);
        int prevIdx = (sectionIdx - 1 + ARANARTHIUM_SECTIONS.size()) % ARANARTHIUM_SECTIONS.size();
        int nextIdx = (sectionIdx + 1) % ARANARTHIUM_SECTIONS.size();
        String prevSection = ARANARTHIUM_SECTIONS.get(prevIdx);
        String nextSection = ARANARTHIUM_SECTIONS.get(nextIdx);

        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        sendMultilineIndented(player, Lang.get("aranarthium.section_" + section + "_content"));
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.get("guide.nav_prev"), Lang.get("aranarthium.section_" + prevSection + "_label"), "/aranarth aranarthium " + prevSection))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("aranarthium.guide_back"), "/aranarth aranarthium"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("guide.nav_next"), Lang.get("aranarthium.section_" + nextSection + "_label"), "/aranarth aranarthium " + nextSection)));
    }

    private void sendIncantationsMenu(Player player) {
        player.sendMessage(ChatUtils.translateToColor(Lang.get("incantations.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("  " + Lang.get("incantations.guide_select_section")));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("        ").append(buildRow(new String[][]{
                {Lang.get("incantations.section_overview_label"), Lang.get("guide.click_to_view"), "/aranarth incantations overview"},
                {Lang.get("incantations.section_applying_label"), Lang.get("guide.click_to_view"), "/aranarth incantations applying"}
        })));
        player.sendMessage(Component.text("  ").append(buildRow(new String[][]{
                {Lang.get("incantations.section_beheading_label"), Lang.get("guide.click_to_view"), "/aranarth incantations beheading"},
                {Lang.get("incantations.section_lifesteal_label"), Lang.get("guide.click_to_view"), "/aranarth incantations lifesteal"},
                {Lang.get("incantations.section_plentiful_label"), Lang.get("guide.click_to_view"), "/aranarth incantations plentiful"}
        })));
        player.sendMessage(Component.text("  ").append(buildRow(new String[][]{
                {Lang.get("incantations.section_magnetism_label"), Lang.get("guide.click_to_view"), "/aranarth incantations magnetism"},
                {Lang.get("incantations.section_resilience_label"), Lang.get("guide.click_to_view"), "/aranarth incantations resilience"},
                {Lang.get("incantations.section_preservation_label"), Lang.get("guide.click_to_view"), "/aranarth incantations preservation"}
        })));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("              ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
    }

    private void sendIncantationsChapter(Player player, String section) {
        if (!INCANTATION_SET.contains(section)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("guide.invalid_chapter", "chapter", section)));
            return;
        }

        int sectionIdx = INCANTATION_SECTIONS.indexOf(section);
        int prevIdx = (sectionIdx - 1 + INCANTATION_SECTIONS.size()) % INCANTATION_SECTIONS.size();
        int nextIdx = (sectionIdx + 1) % INCANTATION_SECTIONS.size();
        String prevSection = INCANTATION_SECTIONS.get(prevIdx);
        String nextSection = INCANTATION_SECTIONS.get(nextIdx);

        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        if (section.equals("overview")) {
            sendMultilineIndented(player, Lang.get("incantations.section_overview_content1"));
            player.sendMessage(Component.empty());
            sendMultilineIndented(player, Lang.get("incantations.section_overview_content2"));
        } else {
            sendMultilineIndented(player, Lang.get("incantations.section_" + section + "_content"));
        }
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.get("guide.nav_prev"), Lang.get("incantations.section_" + prevSection + "_label"), "/aranarth incantations " + prevSection))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("incantations.guide_back"), "/aranarth incantations"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("guide.nav_next"), Lang.get("incantations.section_" + nextSection + "_label"), "/aranarth incantations " + nextSection)));
    }

    private void sendRules(Player player) {
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        for (int i = 1; i <= 10; i++) {
            sendMultilineIndented(player, Lang.get("rules.rule_" + i));
        }
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(Component.text("              ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
    }

    private Component buildRow(String[][] items) {
        var row = Component.text();
        for (int i = 0; i < items.length; i++) {
            row.append(buildBtn(items[i][0], items[i][1], items[i][2]));
            if (i < items.length - 1) {
                row.append(Component.text("   "));
            }
        }
        return row.build();
    }

    /**
     * Builds a single clickable button wrapped in dark gray brackets.
     */
    private Component buildBtn(String label, String hover, String command) {
        String display = "&8[" + label + "&8]";
        Component labelComp = LegacyComponentSerializer.legacySection().deserialize(
                ChatUtils.translateToColor(display));
        return ChatUtils.clickableCommand(labelComp, ChatUtils.translateToColor(hover), command, false);
    }

    /**
     * Builds a navigation back-link button.
     */
    private Component buildNavBtn(String label, String command) {
        return buildNavBtn(label, "&7Click to navigate", command);
    }

    private Component buildNavBtn(String label, String hover, String command) {
        Component labelComp = LegacyComponentSerializer.legacySection().deserialize(
                ChatUtils.translateToColor(label));
        return ChatUtils.clickableCommand(labelComp, ChatUtils.translateToColor(hover), command, false);
    }

    /**
     * Sends multi-line lang content split on newline characters.
     */
    private void sendMultiline(Player player, String content) {
        for (String line : content.split("\n")) {
            if (line.isEmpty()) {
                player.sendMessage(Component.empty());
            } else {
                player.sendMessage(ChatUtils.translateToColor(line));
            }
        }
    }

    /**
     * Same as sendMultiline but prepends 2 spaces to each non-empty line.
     */
    private void sendMultilineIndented(Player player, String content) {
        for (String line : content.split("\n")) {
            if (line.isEmpty()) {
                player.sendMessage(Component.empty());
            } else {
                player.sendMessage(ChatUtils.translateToColor("  " + line));
            }
        }
    }
}
