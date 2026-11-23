package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.abilities.airbending.soundbending.SoundAbility;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.gui.GuiAranarth;
import com.aearost.aranarthcore.gui.GuiRecipes;
import com.aearost.aranarthcore.gui.GuiTrees;
import com.aearost.aranarthcore.items.produce.FruitSapling;
import com.aearost.aranarthcore.items.produce.FruitSeason;
import com.aearost.aranarthcore.trees.CustomTrees;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CustomTreeUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import com.aearost.aranarthcore.utils.FireParticleRegistry;
import com.aearost.aranarthcore.utils.FruitUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.projectkorra.projectkorra.Element;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            "aquatic", "ardent", "dwarven", "echo",
            "elven", "fae", "scorched", "soulbound"
    );

    static final List<String> ESSENCE_SECTIONS = List.of(
            "overview", "applying", "imbuing",
            "beheading", "lifesteal", "plentiful",
            "magnetism", "resilience", "preservation"
    );

    static final List<String> CALENDAR_EVENTS = List.of(
            "solaris", "lunaris", "comet", "convergence", "bluemoon", "meteorite"
    );

    static final List<String> DOMINION_SECTIONS = List.of(
            "intro", "claiming", "food", "members", "flags", "regions",
            "resources", "outposts", "defenders", "relationships", "conquering", "levels", "commands"
    );

    static final List<String> BENDING_SECTIONS = List.of(
            "intro", "elements", "subelements", "firetypes", "combat", "mounts", "avatar"
    );

    static final List<String> RANKS_SECTIONS = List.of(
            "intro", "tiers", "pronouns", "mcmmo", "abilities", "unlocks"
    );

    static final List<String> ECONOMY_SECTIONS = List.of(
            "intro", "jobs", "shopoverview", "shopcreation", "shopusage", "quests", "shopcommands",
            "trading"
    );

    static final List<String> PERKS_SECTIONS = List.of(
            "intro", "blacklist", "tables",
            "colors", "compressor", "shulkerassist",
            "invisiblearmor", "inventoryassist", "randomizer"
    );

    static final List<String> MECHANICS_SECTIONS = List.of(
            "sawmill", "fletchingtable", "specialarrows",
            "quiver", "potions", "dragonsbreath",
            "autolock", "bonemealwood", "cauldronbottles",
            "goathorns", "navigation", "voting",
            "voicechat", "language", "toggles",
            "sounds", "heads", "brewbook"
    );

    static final List<String> FARMING_SECTIONS = List.of(
            "intro", "bigtrees", "treetypes",
            "raretrees", "fruittrees", "crops",
            "growing", "harvesting", "seasons",
            "foraging", "autoplant", "croptrample"
    );

    /**
     * A row of the Tree Types chapter.
     * @param key The lang key of the row's name, after "trees.sapling_".
     * @param saplings The saplings whose trees are listed in the row.
     */
    private record TreeRow(String key, Material... saplings) {}

    private static final List<TreeRow> TREE_ROWS = List.of(
            new TreeRow("oak", Material.OAK_SAPLING),
            new TreeRow("birch", Material.BIRCH_SAPLING),
            new TreeRow("poplar", Material.POPLAR_SAPLING),
            new TreeRow("spruce", Material.SPRUCE_SAPLING),
            new TreeRow("jungle", Material.JUNGLE_SAPLING),
            new TreeRow("acacia", Material.ACACIA_SAPLING),
            new TreeRow("dark_oak", Material.DARK_OAK_SAPLING),
            new TreeRow("pale_oak", Material.PALE_OAK_SAPLING),
            new TreeRow("cherry", Material.CHERRY_SAPLING),
            new TreeRow("mangrove", Material.MANGROVE_PROPAGULE),
            new TreeRow("azalea", Material.AZALEA, Material.FLOWERING_AZALEA),
            new TreeRow("crimson", Material.CRIMSON_FUNGUS),
            new TreeRow("warped", Material.WARPED_FUNGUS),
            new TreeRow("brown_mushroom", Material.BROWN_MUSHROOM),
            new TreeRow("red_mushroom", Material.RED_MUSHROOM)
    );

    static final List<String> CREATIVE_SECTIONS = List.of(
            "overview", "claiming", "merging",
            "members", "finding", "worldedit", "items"
    );

    private static final Set<String> CALENDAR_EVENT_SET = Set.copyOf(CALENDAR_EVENTS);

    // Pixel width that the guide's menu rows are centred within
    private static final int MENU_WIDTH_PX = 236;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
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
            case "rules" -> sendRules(player);
                case "aranarthium", "essences", "dominions", "bending", "ranks", "economy", "perks", "mechanics",
                 "creative", "farmingtrees" -> {
                String topic = args[0].toLowerCase();
                if (args.length == 1) sendSectionMenu(player, topic);
                else sendSectionChapter(player, topic, args[1].toLowerCase());
            }
            // An optional item name filters the guide, the same as searching with the spyglass
            case "recipes" -> GuiRecipes.open(player,
                    args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null);
            default -> player.sendMessage(ChatUtils.chatMessage(
                    Lang.getFor(player, "guide.invalid_topic", "topic", args[0])));
        }
        return true;
    }

    private void sendMainMenu(Player player) {
        new GuiAranarth(player).openGui();
    }

    private void sendCalendarMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.getFor(player, "calendar.guide_title")));
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.getFor(player, "calendar.guide_select_month")));
        player.sendMessage(Component.empty());

        String[] months = CALENDAR_MONTHS.toArray(new String[0]);
        for (int i = 0; i < months.length; i += 3) {
            int end = Math.min(i + 3, months.length);
            String[][] rowData = new String[end - i][];
            for (int j = i; j < end; j++) {
                String month = months[j];
                rowData[j - i] = new String[]{
                        Lang.getFor(player, "calendar." + month + "_name"),
                        Lang.getFor(player, "calendar.click_to_learn", "month", Lang.getFor(player, "calendar." + month + "_name")),
                        "/aranarth calendar " + month
                };
            }
            player.sendMessage(Component.text("       ").append(buildRow(rowData)));
        }

        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("       " + Lang.getFor(player, "calendar.events_header")));
        player.sendMessage(Component.text("          ").append(buildRow(new String[][]{
                {Lang.getFor(player, "calendar.solaris_name"), Lang.getFor(player, "guide.click_to_view"), "/aranarth calendar events solaris"},
                {Lang.getFor(player, "calendar.lunaris_name"), Lang.getFor(player, "guide.click_to_view"), "/aranarth calendar events lunaris"},
                {Lang.getFor(player, "calendar.comet_name"), Lang.getFor(player, "guide.click_to_view"), "/aranarth calendar events comet"}
        })));
        player.sendMessage(Component.text("                   ").append(buildRow(new String[][]{
                {Lang.getFor(player, "calendar.convergence_name"), Lang.getFor(player, "guide.click_to_view"), "/aranarth calendar events convergence"}
        })));
        player.sendMessage(Component.text("        ").append(buildRow(new String[][]{
                {Lang.getFor(player, "calendar.bluemoon_name"), Lang.getFor(player, "guide.click_to_view"), "/aranarth calendar events bluemoon"},
                {Lang.getFor(player, "calendar.meteorite_name"), Lang.getFor(player, "guide.click_to_view"), "/aranarth calendar events meteorite"}
        })));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("                       ").append(buildNavBtn(player, Lang.getFor(player, "guide.back_to_menu"), "/aranarth")));
    }

    private void sendCalendarChapter(Player player, String month) {
        int monthNum = CALENDAR_MONTHS.indexOf(month) + 1;
        if (monthNum == 0) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "guide.invalid_chapter", "chapter", month)));
            return;
        }

        String name = Lang.getFor(player, "calendar." + month + "_name");
        String subtitle = Lang.getFor(player, "calendar." + month + "_subtitle");
        String desc = Lang.getFor(player, "calendar." + month + "_desc");

        int prevIdx = (monthNum - 2 + CALENDAR_MONTHS.size()) % CALENDAR_MONTHS.size();
        int nextIdx = monthNum % CALENDAR_MONTHS.size();
        String prevMonth = CALENDAR_MONTHS.get(prevIdx);
        String nextMonth = CALENDAR_MONTHS.get(nextIdx);

        int days = DateUtils.getDaysInMonth(Month.valueOf(month.toUpperCase()));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "guide.divider")));
        player.sendMessage(ChatUtils.translateToColor("  " + name + " &8(&7" + monthNum + "&8) &7- " + subtitle + " &8| &7" + days + " days"));
        player.sendMessage(Component.empty());
        sendMultilineIndented(player, desc);
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.getFor(player, "guide.nav_prev"), Lang.getFor(player, "calendar." + prevMonth + "_name"), "/aranarth calendar " + prevMonth))
                .append(Component.text("   "))
                .append(buildNavBtn(player, Lang.getFor(player, "calendar.guide_back"), "/aranarth calendar"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.getFor(player, "guide.nav_next"), Lang.getFor(player, "calendar." + nextMonth + "_name"), "/aranarth calendar " + nextMonth)));
    }

    private void sendCalendarEventsMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.getFor(player, "calendar.events_title")));
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.getFor(player, "calendar.events_select")));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("          ").append(buildRow(new String[][]{
                {Lang.getFor(player, "calendar.solaris_name"), Lang.getFor(player, "guide.click_to_view"), "/aranarth calendar events solaris"},
                {Lang.getFor(player, "calendar.lunaris_name"), Lang.getFor(player, "guide.click_to_view"), "/aranarth calendar events lunaris"},
                {Lang.getFor(player, "calendar.comet_name"), Lang.getFor(player, "guide.click_to_view"), "/aranarth calendar events comet"}
        })));
        player.sendMessage(Component.text("                   ").append(buildRow(new String[][]{
                {Lang.getFor(player, "calendar.convergence_name"), Lang.getFor(player, "guide.click_to_view"), "/aranarth calendar events convergence"}
        })));
        player.sendMessage(Component.text("        ").append(buildRow(new String[][]{
                {Lang.getFor(player, "calendar.bluemoon_name"), Lang.getFor(player, "guide.click_to_view"), "/aranarth calendar events bluemoon"},
                {Lang.getFor(player, "calendar.meteorite_name"), Lang.getFor(player, "guide.click_to_view"), "/aranarth calendar events meteorite"}
        })));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("                       ").append(buildNavBtn(player, Lang.getFor(player, "calendar.guide_back"), "/aranarth calendar")));
    }

    private void sendCalendarEventChapter(Player player, String event) {
        if (!CALENDAR_EVENT_SET.contains(event)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "guide.invalid_chapter", "chapter", event)));
            return;
        }

        int eventIdx = CALENDAR_EVENTS.indexOf(event);
        int prevIdx = (eventIdx - 1 + CALENDAR_EVENTS.size()) % CALENDAR_EVENTS.size();
        int nextIdx = (eventIdx + 1) % CALENDAR_EVENTS.size();
        String prevEvent = CALENDAR_EVENTS.get(prevIdx);
        String nextEvent = CALENDAR_EVENTS.get(nextIdx);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "guide.divider")));
        sendMultilineIndented(player, Lang.getFor(player, "calendar." + event + "_desc"));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.getFor(player, "guide.nav_prev"), Lang.getFor(player, "calendar." + prevEvent + "_name"), "/aranarth calendar events " + prevEvent))
                .append(Component.text("   "))
                .append(buildNavBtn(player, Lang.getFor(player, "calendar.events_back"), "/aranarth calendar events"))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.getFor(player, "guide.nav_next"), Lang.getFor(player, "calendar." + nextEvent + "_name"), "/aranarth calendar events " + nextEvent)));
    }

    private void sendRules(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "rules.guide_title")));
        for (int i = 1; i <= 10; i++) {
            player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "rules.rule_" + i)));
        }
        player.sendMessage(Component.text("                    ").append(buildNavBtn(player, Lang.getFor(player, "guide.back_to_menu"), "/aranarth")));
    }

    /**
     * Provides the chapters of a guide topic, in the order they are navigated.
     */
    static List<String> getSections(String topic) {
        return switch (topic) {
            case "aranarthium" -> ARANARTHIUM_SECTIONS;
            case "essences" -> ESSENCE_SECTIONS;
            case "dominions" -> DOMINION_SECTIONS;
            case "bending" -> BENDING_SECTIONS;
            case "ranks" -> RANKS_SECTIONS;
            case "economy" -> ECONOMY_SECTIONS;
            case "perks" -> PERKS_SECTIONS;
            case "mechanics" -> MECHANICS_SECTIONS;
            case "creative" -> CREATIVE_SECTIONS;
            case "farmingtrees" -> FARMING_SECTIONS;
            default -> List.of();
        };
    }

    /**
     * Provides how many chapter buttons go on each row of a topic's menu.
     */
    private static int[] getMenuRows(String topic) {
        return switch (topic) {
            case "aranarthium" -> new int[]{3, 4, 4};
            case "dominions" -> new int[]{3, 3, 3, 2, 2};
            case "bending" -> new int[]{3, 3, 1};
            case "ranks" -> new int[]{3, 3};
            case "creative" -> new int[]{2, 3, 2};
            case "economy" -> new int[]{2, 2, 2, 2};
            case "mechanics" -> new int[]{3, 3, 3, 3, 3, 3};
            case "farmingtrees" -> new int[]{3, 3, 3, 3};
            default -> new int[]{3, 3, 3};
        };
    }

    private void sendSectionMenu(Player player, String topic) {
        List<String> sections = getSections(topic);
        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        String title = Lang.getFor(player, topic + ".guide_title");
        // A title with no spaces of its own in front, often a long one, is centred by its width instead
        if (ChatUtils.stripColorFormatting(title).startsWith(" ")) {
            player.sendMessage(ChatUtils.translateToColor("           " + title));
        } else {
            int spaces = Math.max(0, (MENU_WIDTH_PX - getTitleWidth(title)) / 2 / 4);
            player.sendMessage(ChatUtils.translateToColor(" ".repeat(spaces) + title));
        }
        player.sendMessage(ChatUtils.translateToColor("           " + Lang.getFor(player, topic + ".guide_select_section")));
        player.sendMessage(Component.empty());

        int index = 0;
        for (int rowSize : getMenuRows(topic)) {
            String[][] rowData = new String[rowSize][];
            for (int i = 0; i < rowSize; i++) {
                String section = sections.get(index++);
                rowData[i] = new String[]{
                        Lang.getFor(player, topic + ".section_" + section + "_label"),
                        Lang.getFor(player, "guide.click_to_view"),
                        "/aranarth " + topic + " " + section
                };
            }
            player.sendMessage(centre(buildRow(rowData), getRowWidth(rowData)));
        }

        player.sendMessage(Component.empty());
        String back = Lang.getFor(player, "guide.back_to_menu");
        player.sendMessage(centre(buildNavBtn(player, back, "/aranarth"), getPixelWidth(back)));
    }

    private void sendSectionChapter(Player player, String topic, String section) {
        List<String> sections = getSections(topic);
        if (!sections.contains(section)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "guide.invalid_chapter", "chapter", section)));
            return;
        }

        int sectionIdx = sections.indexOf(section);
        String prevSection = sections.get((sectionIdx - 1 + sections.size()) % sections.size());
        String nextSection = sections.get((sectionIdx + 1) % sections.size());

        player.sendMessage(Component.empty());
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "guide.divider")));
        if (topic.equals("essences") && (section.equals("overview") || section.equals("imbuing"))) {
            sendMultilineIndented(player, Lang.getFor(player, "essences.section_" + section + "_content1"));
            player.sendMessage(Component.empty());
            sendMultilineIndented(player, Lang.getFor(player, "essences.section_" + section + "_content2"));
        } else if (topic.equals("farmingtrees") && section.equals("treetypes")) {
            sendMultilineIndented(player, Lang.getFor(player, "farmingtrees.section_treetypes_content"));
            player.sendMessage(Component.empty());
            sendTreeSizes(player);
        } else if (topic.equals("farmingtrees") && section.equals("raretrees")) {
            sendMultilineIndented(player, Lang.getFor(player, "farmingtrees.section_raretrees_content"));
            player.sendMessage(Component.empty());
            sendRareTrees(player);
        } else if (topic.equals("farmingtrees") && section.equals("fruittrees")) {
            sendMultilineIndented(player, Lang.getFor(player, "farmingtrees.section_fruittrees_content"));
            player.sendMessage(Component.empty());
            sendFruitSaplings(player);
        } else if (topic.equals("bending") || topic.equals("ranks")) {
            sendMultilineIndented(player, Lang.getFor(player, topic + ".section_" + section + "_content", getElementColors()));
        } else {
            sendMultilineIndented(player, Lang.getFor(player, topic + ".section_" + section + "_content"));
        }
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "guide.divider")));
        player.sendMessage(Component.text("  ")
                .append(buildNavBtn(Lang.getFor(player, "guide.nav_prev"), Lang.getFor(player, topic + ".section_" + prevSection + "_label"), "/aranarth " + topic + " " + prevSection))
                .append(Component.text("   "))
                .append(buildNavBtn(player, Lang.getFor(player, topic + ".guide_back"), "/aranarth " + topic))
                .append(Component.text("   "))
                .append(buildNavBtn(Lang.getFor(player, "guide.nav_next"), Lang.getFor(player, topic + ".section_" + nextSection + "_label"), "/aranarth " + topic + " " + nextSection)));
    }

    /** The widest a row of sapling buttons may be before the next row starts, in pixels of the chat font. */
    private static final int SAPLING_ROW_WIDTH = 236;

    /** Reads the hex colours of the saplings' names, which the plain legacy serializer would drop. */
    private static final LegacyComponentSerializer HEX_SERIALIZER = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR).hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    /**
     * Sends a button for each type of sapling, which lists every tree it grows into when hovered over.
     */
    private void sendTreeSizes(Player player) {
        Map<String, List<CustomTrees.Variant>> bySapling = new LinkedHashMap<>();
        for (TreeRow row : TREE_ROWS) {
            List<CustomTrees.Variant> variants = new ArrayList<>();
            for (Material sapling : row.saplings()) {
                variants.addAll(CustomTrees.Variant.forSapling(sapling));
            }
            if (!variants.isEmpty()) {
                bySapling.put(row.key(), variants);
            }
        }
        sendSaplingButtons(player, bySapling);
    }

    /**
     * Sends a button for each type of sapling that grows a rare tree, which lists its rare trees and where they grow.
     */
    private void sendRareTrees(Player player) {
        Map<String, List<CustomTrees.Variant>> bySapling = new LinkedHashMap<>();
        for (CustomTrees.Variant variant : GuiTrees.rare()) {
            bySapling.computeIfAbsent(GuiTrees.saplingKey(variant.getSapling()), key -> new ArrayList<>()).add(variant);
        }
        sendSaplingButtons(player, bySapling);
    }

    /**
     * Sends a button for each type of sapling that crafts a fruit sapling, which lists the fruits it is crafted with.
     */
    private void sendFruitSaplings(Player player) {
        Map<String, List<CustomTrees.Variant>> bySapling = new LinkedHashMap<>();
        for (CustomTrees.Variant variant : FruitSapling.getVariants()) {
            bySapling.computeIfAbsent(GuiTrees.saplingKey(variant.getSapling()), key -> new ArrayList<>()).add(variant);
        }
        sendSaplingButtons(player, bySapling, this::getFruitSaplingHover);
    }

    /**
     * Sends the saplings as centred rows of [Sapling] buttons in the colour of their wood, listing their trees.
     * @param bySapling The trees of each sapling, by the sapling's lang key after "trees.sapling_".
     */
    private void sendSaplingButtons(Player player, Map<String, List<CustomTrees.Variant>> bySapling) {
        sendSaplingButtons(player, bySapling, this::getSaplingHover);
    }

    /** Builds the hover text of a sapling button from the player, the sapling's name and its trees. */
    @FunctionalInterface
    private interface SaplingHover {
        String build(Player player, String sapling, List<CustomTrees.Variant> variants);
    }

    /**
     * Sends the saplings as centred rows of [Sapling] buttons in the colour of their wood.
     * @param bySapling The trees of each sapling, by the sapling's lang key after "trees.sapling_".
     * @param hoverText The hover text of each button.
     */
    private void sendSaplingButtons(Player player, Map<String, List<CustomTrees.Variant>> bySapling, SaplingHover hoverText) {
        int gap = getPixelWidth("   ");
        var row = Component.text();
        int width = 0;
        for (Map.Entry<String, List<CustomTrees.Variant>> entry : bySapling.entrySet()) {
            String sapling = Lang.getFor(player, "trees.sapling_" + entry.getKey());
            String label = "&8[" + sapling + "&8]";
            int labelWidth = getPixelWidth(label);
            if (width > 0 && width + gap + labelWidth > SAPLING_ROW_WIDTH) {
                player.sendMessage(centre(row.build(), width));
                row = Component.text();
                width = 0;
            }
            if (width > 0) {
                row.append(Component.text("   "));
                width += gap;
            }
            row.append(HEX_SERIALIZER.deserialize(ChatUtils.translateToColor(label))
                    .hoverEvent(HoverEvent.showText(HEX_SERIALIZER.deserialize(
                            ChatUtils.translateToColor(hoverText.build(player, sapling, entry.getValue()))))));
            width += labelWidth;
        }
        if (width > 0) {
            player.sendMessage(centre(row.build(), width));
        }
    }

    /**
     * Provides the hover text of a sapling button: each of its trees from the smallest square to the largest,
     * with where it grows if it is a rare tree.
     */
    private String getSaplingHover(Player player, String sapling, List<CustomTrees.Variant> variants) {
        List<CustomTrees.Variant> sorted = new ArrayList<>(variants);
        sorted.sort(Comparator.comparingInt(variant -> Math.max(1, variant.getSize())));
        // The sapling's name in bold, keeping its own colour
        StringBuilder hover = new StringBuilder(sapling.replaceFirst("^(&#[0-9A-Fa-f]{6}|&[0-9a-f])", "$1&l"));
        for (CustomTrees.Variant variant : sorted) {
            int size = Math.max(1, variant.getSize());
            String key = size >= CustomTreeUtils.LARGE_TREE_SIZE
                    ? "farmingtrees.trees_hover_entry_duke" : "farmingtrees.trees_hover_entry";
            hover.append("\n").append(Lang.getFor(player, key,
                    "name", Lang.getFor(player, "trees.variant_" + variant.name().toLowerCase()),
                    "size", size + "x" + size));
            if (variant.isRare()) {
                hover.append("\n  ").append(Lang.getFor(player, "gui.trees.where",
                        "condition", Lang.getFor(player, "trees.condition_" + variant.name().toLowerCase())));
            }
        }
        return hover.toString();
    }

    /**
     * Provides the hover text of a fruit sapling button.
     */
    private String getFruitSaplingHover(Player player, String sapling, List<CustomTrees.Variant> variants) {
        StringBuilder hover = new StringBuilder(sapling.replaceFirst("^(&#[0-9A-Fa-f]{6}|&[0-9a-f])", "$1&l"));
        Month month = AranarthUtils.getMonth();
        for (CustomTrees.Variant variant : variants) {
            String fruitId = FruitSapling.getFruitId(variant);
            FruitSeason season = FruitSeason.of(fruitId);
            hover.append("\n").append(Lang.getFor(player, "farmingtrees.fruit_hover_entry",
                    "fruit", Lang.getFor(player, "produce." + fruitId),
                    "season", Lang.getFor(player, season.getNameKey())));
            List<Month> peak = season.getPeakMonths();
            hover.append("\n  ").append(Lang.getFor(player, "farmingtrees.fruit_hover_peak",
                    "from", Lang.getFor(player, "calendar." + peak.getFirst().name().toLowerCase() + "_name"),
                    "to", Lang.getFor(player, "calendar." + peak.getLast().name().toLowerCase() + "_name")));
            long minutes = FruitUtils.getRegrowTime(season, month) / 60000;
            hover.append("\n  ").append(Lang.getFor(player, "farmingtrees.fruit_hover_now",
                    "percent", String.valueOf(Math.round(season.getShare(month) * 100)),
                    "time", Lang.getFor(player, "farmingtrees.fruit_hover_time",
                            "hours", String.valueOf(minutes / 60), "minutes", String.valueOf(minutes % 60))));
            if (variant.isRare()) {
                hover.append("\n  ").append(Lang.getFor(player, "gui.trees.where",
                        "condition", Lang.getFor(player, FruitSapling.getConditionKey(variant))));
            }
        }
        return hover.toString();
    }

    /**
     * Provides the approximate width of a title in Minecraft's default font, where each bold character is 1 pixel wider.
     */
    private int getTitleWidth(String title) {
        int width = 0;
        boolean bold = false;
        for (String part : title.split("(?=&)")) {
            String text = part;
            if (part.matches("(?s)&[0-9a-fk-or].*")) {
                char code = Character.toLowerCase(part.charAt(1));
                if (code == 'l') {
                    bold = true;
                } else if (code != 'k' && code != 'm' && code != 'n' && code != 'o') {
                    bold = false;
                }
                text = part.substring(2);
            }
            width += getPixelWidth(text) + (bold ? text.length() : 0);
        }
        return width;
    }

    /**
     * Provides the placeholders that colour element and sub-element names in the Bending and Ranks guides,
     * so they always match the colours that ProjectKorra uses.
     */
    private static Object[] getElementColors() {
        return new Object[]{
                "air", Element.AIR.getColor(), "water", Element.WATER.getColor(),
                "earth", Element.EARTH.getColor(), "fire", Element.FIRE.getColor(),
                "chi", Element.CHI.getColor(), "avatar", Element.AVATAR.getColor(),
                "sound", getColor(SoundAbility.SOUND), "spiritual", Element.SPIRITUAL.getColor(),
                "blood", Element.BLOOD.getColor(), "plant", Element.PLANT.getColor(),
                "metal", Element.METAL.getColor(), "sand", Element.SAND.getColor(), "lava", Element.LAVA.getColor(),
                "lightning", Element.LIGHTNING.getColor(), "combustion", Element.COMBUSTION.getColor(),
                "bluefire", Element.BLUE_FIRE.getColor(), "whitefire", getColor(FireParticleRegistry.WHITE_FIRE_SUB),
                "prismaticfire", getColor(FireParticleRegistry.PRISMATIC_FIRE_SUB)
        };
    }

    /**
     * Provides the colour of a sub-element that AranarthCore registers itself, which is unset if bending failed to load.
     */
    private static String getColor(Element element) {
        return element == null ? "" : element.getColor().toString();
    }

    /**
     * Pads the row with leading spaces so that it sits in the middle of the menu.
     */
    private Component centre(Component row, int widthPx) {
        int spaces = Math.max(0, (MENU_WIDTH_PX - widthPx) / 2 / 4);
        return Component.text(" ".repeat(spaces)).append(row);
    }

    /**
     * Provides the width of a row of bracketed buttons, including the gaps between them.
     */
    private int getRowWidth(String[][] items) {
        int width = (items.length - 1) * 3 * 4;
        for (String[] item : items) {
            width += getPixelWidth("[" + item[0] + "]");
        }
        return width;
    }

    /**
     * Provides the approximate width of the text in Minecraft's default font, ignoring colour codes.
     */
    private int getPixelWidth(String text) {
        int width = 0;
        for (char c : ChatUtils.stripColorFormatting(text).toCharArray()) {
            width += switch (c) {
                case 'i', '!', ',', '.', ':', ';', '|', '\'' -> 2;
                case 'l', '`' -> 3;
                case ' ', 'I', 't', '[', ']' -> 4;
                case 'f', 'k', '<', '>', '(', ')', '*', '"', '{', '}' -> 5;
                case '@', '~' -> 7;
                default -> 6;
            };
        }
        return width;
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
    private Component buildNavBtn(Player player, String label, String command) {
        return buildNavBtn(label, Lang.getFor(player, "guide.click_to_navigate"), command);
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
