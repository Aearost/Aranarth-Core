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
            "intro", "ingots", "elytra",
            "aquatic", "ardent", "dwarven",
            "elven", "fae", "scorched", "soulbound"
    );

    static final List<String> ESSENCE_SECTIONS = List.of(
            "overview", "applying", "beheading",
            "lifesteal", "plentiful", "magnetism",
            "resilience", "preservation"
    );

    static final List<String> CALENDAR_EVENTS = List.of(
            "solaris", "lunaris", "comet", "convergence", "bluemoon", "meteorite"
    );

    static final List<String> DOMINION_SECTIONS = List.of(
            "intro", "claiming", "food", "members", "flags", "plots",
            "resources", "outposts", "defenders", "relationships", "conquering", "levels"
    );

    static final List<String> BENDING_SECTIONS = List.of(
            "intro", "elements", "ranklocked", "subelements", "restrictions", "combat", "mounts", "avatar"
    );

    static final List<String> RANKS_SECTIONS = List.of(
            "intro", "tiers", "pronouns", "mcmmo", "abilities", "unlocks"
    );

    static final List<String> ECONOMY_SECTIONS = List.of(
            "intro", "jobs", "shopoverview", "shopcreation", "shopusage", "quests", "shopcommands", "shoplisting",
            "trading", "marketprice"
    );

    static final List<String> PERKS_SECTIONS = List.of(
            "intro", "blacklist", "tables",
            "cosmetics", "hexcolors", "compressor",
            "shulkerassist", "invisiblearmor",
            "inventoryassist", "randomizer"
    );

    static final List<String> MECHANICS_SECTIONS = List.of(
            "fletchingtable", "specialarrows", "quiver",
            "dragonsbreath", "autoplant", "croptrample",
            "chestsort", "autolock", "bonemealwood",
            "cauldronbottles", "goathorns", "homepads"
    );

    private static final Set<String> ARANARTHIUM_SET = Set.copyOf(ARANARTHIUM_SECTIONS);
    private static final Set<String> ESSENCE_SET = Set.copyOf(ESSENCE_SECTIONS);
    private static final Set<String> CALENDAR_EVENT_SET = Set.copyOf(CALENDAR_EVENTS);
    private static final Set<String> DOMINION_SET = Set.copyOf(DOMINION_SECTIONS);
    private static final Set<String> BENDING_SET = Set.copyOf(BENDING_SECTIONS);
    private static final Set<String> RANKS_SET = Set.copyOf(RANKS_SECTIONS);
    private static final Set<String> ECONOMY_SET = Set.copyOf(ECONOMY_SECTIONS);
    private static final Set<String> PERKS_SET = Set.copyOf(PERKS_SECTIONS);
    private static final Set<String> MECHANICS_SET = Set.copyOf(MECHANICS_SECTIONS);

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
                if (args.length == 1) {
                    sendCalendarMenu(player);
                } else if (args[1].equalsIgnoreCase("events")) {
                    if (args.length == 2) sendCalendarEventsMenu(player);
                    else sendCalendarEventChapter(player, args[2].toLowerCase());
                } else {
                    sendCalendarChapter(player, args[1].toLowerCase());
                }
            }
            case "aranarthium" -> {
                if (args.length == 1) sendAranarthiumMenu(player);
                else sendAranarthiumChapter(player, args[1].toLowerCase());
            }
            case "essences" -> {
                if (args.length == 1) sendEssencesMenu(player);
                else sendEssencesChapter(player, args[1].toLowerCase());
            }
            case "rules" -> sendRules(player);
            case "dominions" -> {
                if (args.length == 1) sendDominionsMenu(player);
                else sendDominionsChapter(player, args[1].toLowerCase());
            }
            case "bending" -> {
                if (args.length == 1) sendBendingMenu(player);
                else sendBendingChapter(player, args[1].toLowerCase());
            }
            case "ranks" -> {
                if (args.length == 1) sendRanksMenu(player);
                else sendRanksChapter(player, args[1].toLowerCase());
            }
            case "economy" -> {
                if (args.length == 1) sendEconomyMenu(player);
                else sendEconomyChapter(player, args[1].toLowerCase());
            }
            case "perks" -> {
                if (args.length == 1) sendPerksMenu(player);
                else sendPerksChapter(player, args[1].toLowerCase());
            }
            case "mechanics" -> {
                if (args.length == 1) sendMechanicsMenu(player);
                else sendMechanicsChapter(player, args[1].toLowerCase());
            }
            case "recipes" -> player.sendMessage(ChatUtils.chatMessage(Lang.get("recipes.coming_soon")));
            default -> player.sendMessage(ChatUtils.chatMessage(
                    Lang.get("guide.invalid_topic", "topic", args[0])));
        }
        return true;
    }

    private void sendMainMenu(Player player) {
        new GuiAranarth(player).openGui();
    }

    private void sendCalendarMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("calendar.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("calendar.guide_select_month")));
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
            player.sendMessage(Component.text("       ").append(buildRow(rowData)));
        }

        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("       " + Lang.get("calendar.events_header")));
        player.sendMessage(Component.text("          ").append(buildRow(new String[][]{
                {Lang.get("calendar.solaris_name"), Lang.get("guide.click_to_view"), "/aranarth calendar events solaris"},
                {Lang.get("calendar.lunaris_name"), Lang.get("guide.click_to_view"), "/aranarth calendar events lunaris"},
                {Lang.get("calendar.comet_name"), Lang.get("guide.click_to_view"), "/aranarth calendar events comet"}
        })));
        player.sendMessage(Component.text("                   ").append(buildRow(new String[][]{
                {Lang.get("calendar.convergence_name"), Lang.get("guide.click_to_view"), "/aranarth calendar events convergence"}
        })));
        player.sendMessage(Component.text("        ").append(buildRow(new String[][]{
                {Lang.get("calendar.bluemoon_name"), Lang.get("guide.click_to_view"), "/aranarth calendar events bluemoon"},
                {Lang.get("calendar.meteorite_name"), Lang.get("guide.click_to_view"), "/aranarth calendar events meteorite"}
        })));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("                       ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
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
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
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

    private void sendCalendarEventsMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("calendar.events_title")));
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("calendar.events_select")));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("          ").append(buildRow(new String[][]{
                {Lang.get("calendar.solaris_name"), Lang.get("guide.click_to_view"), "/aranarth calendar events solaris"},
                {Lang.get("calendar.lunaris_name"), Lang.get("guide.click_to_view"), "/aranarth calendar events lunaris"},
                {Lang.get("calendar.comet_name"), Lang.get("guide.click_to_view"), "/aranarth calendar events comet"}
        })));
        player.sendMessage(Component.text("                   ").append(buildRow(new String[][]{
                {Lang.get("calendar.convergence_name"), Lang.get("guide.click_to_view"), "/aranarth calendar events convergence"}
        })));
        player.sendMessage(Component.text("        ").append(buildRow(new String[][]{
                {Lang.get("calendar.bluemoon_name"), Lang.get("guide.click_to_view"), "/aranarth calendar events bluemoon"},
                {Lang.get("calendar.meteorite_name"), Lang.get("guide.click_to_view"), "/aranarth calendar events meteorite"}
        })));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("                       ").append(buildNavBtn(Lang.get("calendar.guide_back"), "/aranarth calendar")));
    }

    private void sendCalendarEventChapter(Player player, String event) {
        if (!CALENDAR_EVENT_SET.contains(event)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("guide.invalid_chapter", "chapter", event)));
            return;
        }

        int eventIdx = CALENDAR_EVENTS.indexOf(event);
        int prevIdx = (eventIdx - 1 + CALENDAR_EVENTS.size()) % CALENDAR_EVENTS.size();
        int nextIdx = (eventIdx + 1) % CALENDAR_EVENTS.size();
        String prevEvent = CALENDAR_EVENTS.get(prevIdx);
        String nextEvent = CALENDAR_EVENTS.get(nextIdx);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        sendMultilineIndented(player, Lang.get("calendar." + event + "_desc"));
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.get("guide.nav_prev"), Lang.get("calendar." + prevEvent + "_name"), "/aranarth calendar events " + prevEvent))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("calendar.events_back"), "/aranarth calendar events"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("guide.nav_next"), Lang.get("calendar." + nextEvent + "_name"), "/aranarth calendar events " + nextEvent)));
    }

    private void sendAranarthiumMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("aranarthium.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("aranarthium.guide_select_section")));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("   ").append(buildRow(new String[][]{
                {Lang.get("aranarthium.section_intro_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium intro"},
                {Lang.get("aranarthium.section_ingots_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium ingots"},
                {Lang.get("aranarthium.section_elytra_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium elytra"}
        })));
        player.sendMessage(Component.text("           ").append(buildRow(new String[][]{
                {Lang.get("aranarthium.section_aquatic_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium aquatic"},
                {Lang.get("aranarthium.section_ardent_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium ardent"},
                {Lang.get("aranarthium.section_dwarven_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium dwarven"}
        })));
        player.sendMessage(Component.text("    ").append(buildRow(new String[][]{
                {Lang.get("aranarthium.section_elven_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium elven"},
                {Lang.get("aranarthium.section_fae_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium fae"},
                {Lang.get("aranarthium.section_scorched_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium scorched"},
                {Lang.get("aranarthium.section_soulbound_label"), Lang.get("guide.click_to_view"), "/aranarth aranarthium soulbound"}
        })));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("                      ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
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

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
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

    private void sendEssencesMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("essences.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("essences.guide_select_section")));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("       ").append(buildRow(new String[][]{
                {Lang.get("essences.section_overview_label"), Lang.get("guide.click_to_view"), "/aranarth essences overview"},
                {Lang.get("essences.section_applying_label"), Lang.get("guide.click_to_view"), "/aranarth essences applying"},
                {Lang.get("essences.section_beheading_label"), Lang.get("guide.click_to_view"), "/aranarth essences beheading"}
        })));
        player.sendMessage(Component.text("       ").append(buildRow(new String[][]{
                {Lang.get("essences.section_lifesteal_label"), Lang.get("guide.click_to_view"), "/aranarth essences lifesteal"},
                {Lang.get("essences.section_plentiful_label"), Lang.get("guide.click_to_view"), "/aranarth essences plentiful"},
                {Lang.get("essences.section_magnetism_label"), Lang.get("guide.click_to_view"), "/aranarth essences magnetism"}
        })));
        player.sendMessage(Component.text("             ").append(buildRow(new String[][]{
                {Lang.get("essences.section_resilience_label"), Lang.get("guide.click_to_view"), "/aranarth essences resilience"},
                {Lang.get("essences.section_preservation_label"), Lang.get("guide.click_to_view"), "/aranarth essences preservation"}
        })));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("                     ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
    }

    private void sendEssencesChapter(Player player, String section) {
        if (!ESSENCE_SET.contains(section)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("guide.invalid_chapter", "chapter", section)));
            return;
        }

        int sectionIdx = ESSENCE_SECTIONS.indexOf(section);
        int prevIdx = (sectionIdx - 1 + ESSENCE_SECTIONS.size()) % ESSENCE_SECTIONS.size();
        int nextIdx = (sectionIdx + 1) % ESSENCE_SECTIONS.size();
        String prevSection = ESSENCE_SECTIONS.get(prevIdx);
        String nextSection = ESSENCE_SECTIONS.get(nextIdx);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        if (section.equals("overview")) {
            sendMultilineIndented(player, Lang.get("essences.section_overview_content1"));
            player.sendMessage(Component.empty());
            sendMultilineIndented(player, Lang.get("essences.section_overview_content2"));
        } else {
            sendMultilineIndented(player, Lang.get("essences.section_" + section + "_content"));
        }
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.get("guide.nav_prev"), Lang.get("essences.section_" + prevSection + "_label"), "/aranarth essences " + prevSection))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("essences.guide_back"), "/aranarth essences"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("guide.nav_next"), Lang.get("essences.section_" + nextSection + "_label"), "/aranarth essences " + nextSection)));
    }

    private void sendRules(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.get("rules.guide_title")));
        for (int i = 1; i <= 10; i++) {
            player.sendMessage(ChatUtils.translateToColor(Lang.get("rules.rule_" + i)));
        }
        player.sendMessage(Component.text("                    ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
    }

    private void sendDominionsMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("dominions.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("dominions.guide_select_section")));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("     ").append(buildRow(new String[][]{
                {Lang.get("dominions.section_intro_label"), Lang.get("guide.click_to_view"), "/aranarth dominions intro"},
                {Lang.get("dominions.section_claiming_label"), Lang.get("guide.click_to_view"), "/aranarth dominions claiming"},
                {Lang.get("dominions.section_food_label"), Lang.get("guide.click_to_view"), "/aranarth dominions food"}
        })));
        player.sendMessage(Component.text("            ").append(buildRow(new String[][]{
                {Lang.get("dominions.section_members_label"), Lang.get("guide.click_to_view"), "/aranarth dominions members"},
                {Lang.get("dominions.section_flags_label"), Lang.get("guide.click_to_view"), "/aranarth dominions flags"},
                {Lang.get("dominions.section_plots_label"), Lang.get("guide.click_to_view"), "/aranarth dominions plots"}
        })));
        player.sendMessage(Component.text("    ").append(buildRow(new String[][]{
                {Lang.get("dominions.section_resources_label"), Lang.get("guide.click_to_view"), "/aranarth dominions resources"},
                {Lang.get("dominions.section_outposts_label"), Lang.get("guide.click_to_view"), "/aranarth dominions outposts"},
                {Lang.get("dominions.section_defenders_label"), Lang.get("guide.click_to_view"), "/aranarth dominions defenders"}
        })));
        player.sendMessage(Component.text("    ").append(buildRow(new String[][]{
                {Lang.get("dominions.section_relationships_label"), Lang.get("guide.click_to_view"), "/aranarth dominions relationships"},
                {Lang.get("dominions.section_conquering_label"), Lang.get("guide.click_to_view"), "/aranarth dominions conquering"},
                {Lang.get("dominions.section_levels_label"), Lang.get("guide.click_to_view"), "/aranarth dominions levels"}
        })));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("                    ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
    }

    private void sendDominionsChapter(Player player, String section) {
        if (!DOMINION_SET.contains(section)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("guide.invalid_chapter", "chapter", section)));
            return;
        }

        int sectionIdx = DOMINION_SECTIONS.indexOf(section);
        int prevIdx = (sectionIdx - 1 + DOMINION_SECTIONS.size()) % DOMINION_SECTIONS.size();
        int nextIdx = (sectionIdx + 1) % DOMINION_SECTIONS.size();
        String prevSection = DOMINION_SECTIONS.get(prevIdx);
        String nextSection = DOMINION_SECTIONS.get(nextIdx);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        sendMultilineIndented(player, Lang.get("dominions.section_" + section + "_content"));
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.get("guide.nav_prev"), Lang.get("dominions.section_" + prevSection + "_label"), "/aranarth dominions " + prevSection))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("dominions.guide_back"), "/aranarth dominions"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("guide.nav_next"), Lang.get("dominions.section_" + nextSection + "_label"), "/aranarth dominions " + nextSection)));
    }

    private void sendBendingMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("bending.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("bending.guide_select_section")));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("    ").append(buildRow(new String[][]{
                {Lang.get("bending.section_intro_label"), Lang.get("guide.click_to_view"), "/aranarth bending intro"},
                {Lang.get("bending.section_elements_label"), Lang.get("guide.click_to_view"), "/aranarth bending elements"},
                {Lang.get("bending.section_ranklocked_label"), Lang.get("guide.click_to_view"), "/aranarth bending ranklocked"}
        })));
        player.sendMessage(Component.text("  ").append(buildRow(new String[][]{
                {Lang.get("bending.section_subelements_label"), Lang.get("guide.click_to_view"), "/aranarth bending subelements"},
                {Lang.get("bending.section_restrictions_label"), Lang.get("guide.click_to_view"), "/aranarth bending restrictions"},
                {Lang.get("bending.section_combat_label"), Lang.get("guide.click_to_view"), "/aranarth bending combat"}
        })));
        player.sendMessage(Component.text("                  ").append(buildRow(new String[][]{
                {Lang.get("bending.section_mounts_label"), Lang.get("guide.click_to_view"), "/aranarth bending mounts"},
                {Lang.get("bending.section_avatar_label"), Lang.get("guide.click_to_view"), "/aranarth bending avatar"}
        })));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("                   ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
    }

    private void sendBendingChapter(Player player, String section) {
        if (!BENDING_SET.contains(section)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("guide.invalid_chapter", "chapter", section)));
            return;
        }

        int sectionIdx = BENDING_SECTIONS.indexOf(section);
        int prevIdx = (sectionIdx - 1 + BENDING_SECTIONS.size()) % BENDING_SECTIONS.size();
        int nextIdx = (sectionIdx + 1) % BENDING_SECTIONS.size();
        String prevSection = BENDING_SECTIONS.get(prevIdx);
        String nextSection = BENDING_SECTIONS.get(nextIdx);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        sendMultilineIndented(player, Lang.get("bending.section_" + section + "_content"));
        if (section.equals("restrictions")) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("  ").append(buildBtn(
                    Lang.get("dominions.section_flags_label"),
                    "&7View Dominion Flags guide",
                    "/aranarth dominions flags"
            )));
        }
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.get("guide.nav_prev"), Lang.get("bending.section_" + prevSection + "_label"), "/aranarth bending " + prevSection))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("bending.guide_back"), "/aranarth bending"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("guide.nav_next"), Lang.get("bending.section_" + nextSection + "_label"), "/aranarth bending " + nextSection)));
    }

    private void sendRanksMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("ranks.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("ranks.guide_select_section")));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("       ").append(buildRow(new String[][]{
                {Lang.get("ranks.section_intro_label"), Lang.get("guide.click_to_view"), "/aranarth ranks intro"},
                {Lang.get("ranks.section_tiers_label"), Lang.get("guide.click_to_view"), "/aranarth ranks tiers"},
                {Lang.get("ranks.section_pronouns_label"), Lang.get("guide.click_to_view"), "/aranarth ranks pronouns"}
        })));
        player.sendMessage(Component.text("        ").append(buildRow(new String[][]{
                {Lang.get("ranks.section_mcmmo_label"), Lang.get("guide.click_to_view"), "/aranarth ranks mcmmo"},
                {Lang.get("ranks.section_abilities_label"), Lang.get("guide.click_to_view"), "/aranarth ranks abilities"},
                {Lang.get("ranks.section_unlocks_label"), Lang.get("guide.click_to_view"), "/aranarth ranks unlocks"}
        })));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("                  ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
    }

    private void sendRanksChapter(Player player, String section) {
        if (!RANKS_SET.contains(section)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("guide.invalid_chapter", "chapter", section)));
            return;
        }

        int sectionIdx = RANKS_SECTIONS.indexOf(section);
        int prevIdx = (sectionIdx - 1 + RANKS_SECTIONS.size()) % RANKS_SECTIONS.size();
        int nextIdx = (sectionIdx + 1) % RANKS_SECTIONS.size();
        String prevSection = RANKS_SECTIONS.get(prevIdx);
        String nextSection = RANKS_SECTIONS.get(nextIdx);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        sendMultilineIndented(player, Lang.get("ranks.section_" + section + "_content"));
        if (section.equals("unlocks")) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("  ").append(buildBtn(
                    Lang.get("perks.guide_title_short"),
                    "&7View Perks guide",
                    "/aranarth perks"
            )));
        }
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.get("guide.nav_prev"), Lang.get("ranks.section_" + prevSection + "_label"), "/aranarth ranks " + prevSection))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("ranks.guide_back"), "/aranarth ranks"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("guide.nav_next"), Lang.get("ranks.section_" + nextSection + "_label"), "/aranarth ranks " + nextSection)));
    }

    private void sendEconomyMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("economy.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("economy.guide_select_section")));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("                  ").append(buildRow(new String[][]{
                {Lang.get("economy.section_intro_label"), Lang.get("guide.click_to_view"), "/aranarth economy intro"},
                {Lang.get("economy.section_jobs_label"), Lang.get("guide.click_to_view"), "/aranarth economy jobs"}
        })));
        player.sendMessage(Component.text("        ").append(buildRow(new String[][]{
                {Lang.get("economy.section_shopoverview_label"), Lang.get("guide.click_to_view"), "/aranarth economy shopoverview"},
                {Lang.get("economy.section_shopcreation_label"), Lang.get("guide.click_to_view"), "/aranarth economy shopcreation"}
        })));
        player.sendMessage(Component.text("            ").append(buildRow(new String[][]{
                {Lang.get("economy.section_shopusage_label"), Lang.get("guide.click_to_view"), "/aranarth economy shopusage"},
                {Lang.get("economy.section_quests_label"), Lang.get("guide.click_to_view"), "/aranarth economy quests"}
        })));
        player.sendMessage(Component.text("        ").append(buildRow(new String[][]{
                {Lang.get("economy.section_shopcommands_label"), Lang.get("guide.click_to_view"), "/aranarth economy shopcommands"},
                {Lang.get("economy.section_shoplisting_label"), Lang.get("guide.click_to_view"), "/aranarth economy shoplisting"}
        })));
        player.sendMessage(Component.text("              ").append(buildRow(new String[][]{
                {Lang.get("economy.section_trading_label"), Lang.get("guide.click_to_view"), "/aranarth economy trading"},
                {Lang.get("economy.section_marketprice_label"), Lang.get("guide.click_to_view"), "/aranarth economy marketprice"}
        })));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("                    ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
    }

    private void sendEconomyChapter(Player player, String section) {
        if (!ECONOMY_SET.contains(section)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("guide.invalid_chapter", "chapter", section)));
            return;
        }

        int sectionIdx = ECONOMY_SECTIONS.indexOf(section);
        int prevIdx = (sectionIdx - 1 + ECONOMY_SECTIONS.size()) % ECONOMY_SECTIONS.size();
        int nextIdx = (sectionIdx + 1) % ECONOMY_SECTIONS.size();
        String prevSection = ECONOMY_SECTIONS.get(prevIdx);
        String nextSection = ECONOMY_SECTIONS.get(nextIdx);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        sendMultilineIndented(player, Lang.get("economy.section_" + section + "_content"));
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.get("guide.nav_prev"), Lang.get("economy.section_" + prevSection + "_label"), "/aranarth economy " + prevSection))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("economy.guide_back"), "/aranarth economy"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("guide.nav_next"), Lang.get("economy.section_" + nextSection + "_label"), "/aranarth economy " + nextSection)));
    }

    private void sendPerksMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("perks.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("perks.guide_select_section")));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("       ").append(buildRow(new String[][]{
                {Lang.get("perks.section_intro_label"), Lang.get("guide.click_to_view"), "/aranarth perks intro"},
                {Lang.get("perks.section_blacklist_label"), Lang.get("guide.click_to_view"), "/aranarth perks blacklist"},
                {Lang.get("perks.section_tables_label"), Lang.get("guide.click_to_view"), "/aranarth perks tables"}
        })));
        player.sendMessage(Component.text(" ").append(buildRow(new String[][]{
                {Lang.get("perks.section_cosmetics_label"), Lang.get("guide.click_to_view"), "/aranarth perks cosmetics"},
                {Lang.get("perks.section_hexcolors_label"), Lang.get("guide.click_to_view"), "/aranarth perks hexcolors"},
                {Lang.get("perks.section_compressor_label"), Lang.get("guide.click_to_view"), "/aranarth perks compressor"}
        })));
        player.sendMessage(Component.text("      ").append(buildRow(new String[][]{
                {Lang.get("perks.section_shulkerassist_label"), Lang.get("guide.click_to_view"), "/aranarth perks shulkerassist"},
                {Lang.get("perks.section_invisiblearmor_label"), Lang.get("guide.click_to_view"), "/aranarth perks invisiblearmor"}
        })));
        player.sendMessage(Component.text("       ").append(buildRow(new String[][]{
                {Lang.get("perks.section_inventoryassist_label"), Lang.get("guide.click_to_view"), "/aranarth perks inventoryassist"},
                {Lang.get("perks.section_randomizer_label"), Lang.get("guide.click_to_view"), "/aranarth perks randomizer"}
        })));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("                  ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
    }

    private void sendPerksChapter(Player player, String section) {
        if (!PERKS_SET.contains(section)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("guide.invalid_chapter", "chapter", section)));
            return;
        }

        int sectionIdx = PERKS_SECTIONS.indexOf(section);
        int prevIdx = (sectionIdx - 1 + PERKS_SECTIONS.size()) % PERKS_SECTIONS.size();
        int nextIdx = (sectionIdx + 1) % PERKS_SECTIONS.size();
        String prevSection = PERKS_SECTIONS.get(prevIdx);
        String nextSection = PERKS_SECTIONS.get(nextIdx);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        sendMultilineIndented(player, Lang.get("perks.section_" + section + "_content"));
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.get("guide.nav_prev"), Lang.get("perks.section_" + prevSection + "_label"), "/aranarth perks " + prevSection))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("perks.guide_back"), "/aranarth perks"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("guide.nav_next"), Lang.get("perks.section_" + nextSection + "_label"), "/aranarth perks " + nextSection)));
    }

    private void sendMechanicsMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("mechanics.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.get("mechanics.guide_select_section")));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("  ").append(buildRow(new String[][]{
                {Lang.get("mechanics.section_fletchingtable_label"), Lang.get("guide.click_to_view"), "/aranarth mechanics fletchingtable"},
                {Lang.get("mechanics.section_specialarrows_label"), Lang.get("guide.click_to_view"), "/aranarth mechanics specialarrows"},
                {Lang.get("mechanics.section_quiver_label"), Lang.get("guide.click_to_view"), "/aranarth mechanics quiver"}
        })));
        player.sendMessage(Component.text("  ").append(buildRow(new String[][]{
                {Lang.get("mechanics.section_dragonsbreath_label"), Lang.get("guide.click_to_view"), "/aranarth mechanics dragonsbreath"},
                {Lang.get("mechanics.section_autoplant_label"), Lang.get("guide.click_to_view"), "/aranarth mechanics autoplant"},
                {Lang.get("mechanics.section_croptrample_label"), Lang.get("guide.click_to_view"), "/aranarth mechanics croptrample"}
        })));
        player.sendMessage(Component.text(" ").append(buildRow(new String[][]{
                {Lang.get("mechanics.section_chestsort_label"), Lang.get("guide.click_to_view"), "/aranarth mechanics chestsort"},
                {Lang.get("mechanics.section_autolock_label"), Lang.get("guide.click_to_view"), "/aranarth mechanics autolock"},
                {Lang.get("mechanics.section_bonemealwood_label"), Lang.get("guide.click_to_view"), "/aranarth mechanics bonemealwood"}
        })));
        player.sendMessage(Component.text("     ").append(buildRow(new String[][]{
                {Lang.get("mechanics.section_cauldronbottles_label"), Lang.get("guide.click_to_view"), "/aranarth mechanics cauldronbottles"},
                {Lang.get("mechanics.section_goathorns_label"), Lang.get("guide.click_to_view"), "/aranarth mechanics goathorns"},
                {Lang.get("mechanics.section_homepads_label"), Lang.get("guide.click_to_view"), "/aranarth mechanics homepads"}
        })));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("                     ").append(buildNavBtn(Lang.get("guide.back_to_menu"), "/aranarth")));
    }

    private void sendMechanicsChapter(Player player, String section) {
        if (!MECHANICS_SET.contains(section)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("guide.invalid_chapter", "chapter", section)));
            return;
        }

        int sectionIdx = MECHANICS_SECTIONS.indexOf(section);
        int prevIdx = (sectionIdx - 1 + MECHANICS_SECTIONS.size()) % MECHANICS_SECTIONS.size();
        int nextIdx = (sectionIdx + 1) % MECHANICS_SECTIONS.size();
        String prevSection = MECHANICS_SECTIONS.get(prevIdx);
        String nextSection = MECHANICS_SECTIONS.get(nextIdx);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        sendMultilineIndented(player, Lang.get("mechanics.section_" + section + "_content"));
        player.sendMessage(ChatUtils.translateToColor(Lang.get("guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.get("guide.nav_prev"), Lang.get("mechanics.section_" + prevSection + "_label"), "/aranarth mechanics " + prevSection))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("mechanics.guide_back"), "/aranarth mechanics"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.get("guide.nav_next"), Lang.get("mechanics.section_" + nextSection + "_label"), "/aranarth mechanics " + nextSection)));
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
