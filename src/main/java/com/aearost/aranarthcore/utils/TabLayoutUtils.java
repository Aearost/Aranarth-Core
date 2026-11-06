package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.TabGroup;
import com.aearost.aranarthcore.network.NetworkTabManager;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * Arranges the tab list into groups based on where each player is, with a divider above each group.
 */
public class TabLayoutUtils {

    // The client only uses two columns past 20 entries, so a two-column layout needs at least 11 rows
    private static final int MIN_COLUMN_ROWS = 11;
    private static final int MAX_ROWS = 20;
    private static final int MAX_COLUMNS = 4;
    // The fewest spaces of the line on each side of a divider's label
    private static final int MIN_LINE_SPACES = 3;
    private static final int SPACE_WIDTH = 4;

    private static final String GRAY_SKIN = "gray";
    private static final String GOLD_SKIN = "gold";

    // The skin of each divider and filler entry currently shown to each viewer, in order
    private static final Map<UUID, List<String>> shownLayoutSkins = new HashMap<>();
    private static String lastSkinConfig = "";

    /**
     * A player shown in the tab list.
     *
     * @param uuid     The player's UUID.
     * @param group    The group the player is shown in.
     * @param vanished Whether the player is vanished, so only shown to players who can see vanished players.
     */
    public record TabMember(UUID uuid, TabGroup group, boolean vanished) {
    }

    /**
     * An entry of the tab list - a player, a divider of the group, the notice under a group that nobody is in, or a blank filler.
     */
    private record Slot(UUID playerUuid, TabGroup dividerGroup, int dividerCount, boolean isEmptyNotice) {
    }

    /**
     * Arranges every viewer's tab list into the groups.
     *
     * @param members         The players shown in the tab list, already sorted by rank.
     * @param viewers         The players whose tab list is being updated.
     * @param getDisplayNames Provides the tab names that the viewer sees for each player, used to size the dividers.
     */
    public static void applyLayout(List<TabMember> members, Collection<? extends Player> viewers,
                                   Function<Player, Map<UUID, String>> getDisplayNames) {
        Map<String, String[]> skins = loadSkins();

        // Re-adds every divider and filler entry when a skin changes, so their heads update without rejoining
        StringBuilder skinConfig = new StringBuilder();
        for (String[] skin : skins.values()) {
            skinConfig.append(skin[0]).append(skin[1]);
        }
        if (!skinConfig.toString().equals(lastSkinConfig)) {
            lastSkinConfig = skinConfig.toString();
            for (List<String> shownSkins : shownLayoutSkins.values()) {
                Collections.fill(shownSkins, "");
            }
        }

        for (Player viewer : viewers) {
            boolean canSeeVanished = viewer.hasPermission("aranarth.vanish.see");

            // Splits the players this viewer can see into their groups, keeping the rank order within each group
            Map<TabGroup, List<UUID>> groups = new EnumMap<>(TabGroup.class);
            for (TabGroup group : TabGroup.values()) {
                groups.put(group, new ArrayList<>());
            }
            for (TabMember member : members) {
                if (member.vanished() && !canSeeVanished && !member.uuid().equals(viewer.getUniqueId())) {
                    continue;
                }
                groups.get(member.group()).add(member.uuid());
            }

            // Each group is a block starting with its divider, kept in order on its side
            List<List<Slot>> left = new ArrayList<>();
            List<List<Slot>> right = new ArrayList<>();
            int widestName = 0;
            Map<UUID, String> displayNames = getDisplayNames.apply(viewer);
            for (TabGroup group : TabGroup.values()) {
                List<UUID> groupMembers = groups.get(group);
                List<Slot> side = new ArrayList<>();
                (group.isRightSide() ? right : left).add(side);
                side.add(new Slot(null, group, groupMembers.size(), false));
                if (groupMembers.isEmpty()) {
                    side.add(new Slot(null, null, 0, true));
                }
                for (UUID uuid : groupMembers) {
                    side.add(new Slot(uuid, null, 0, false));
                    String displayName = displayNames.get(uuid);
                    if (displayName != null) {
                        widestName = Math.max(widestName, getTextWidth(displayName));
                    }
                }
            }

            sendLayout(viewer, arrange(left, right), widestName, skins);
        }
    }

    /**
     * Provides the signed texture value and signature of each head color used by the dividers and fillers.
     */
    private static Map<String, String[]> loadSkins() {
        Map<String, String[]> skins = new LinkedHashMap<>();
        String[] gray = loadSkin("players.tab-blank-skin");
        skins.put(GRAY_SKIN, gray);
        String[] gold = loadSkin("players.tab-gold-skin");
        skins.put(GOLD_SKIN, gold[0].isEmpty() ? gray : gold);
        return skins;
    }

    /**
     * Provides the texture value and signature at the input config path.
     * Read without a fallback value, so a key only merged into config.yml on this startup still falls back to the jar's default.
     */
    private static String[] loadSkin(String path) {
        String value = AranarthCore.getInstance().getConfig().getString(path + ".value");
        String signature = AranarthCore.getInstance().getConfig().getString(path + ".signature");
        return new String[]{value != null ? value : "", signature != null ? signature : ""};
    }

    /**
     * Provides the head color of a divider or filler entry.
     */
    private static String getSlotSkin(Slot slot) {
        if (slot.dividerGroup() != null && slot.dividerCount() > 0) {
            return GOLD_SKIN;
        }
        return GRAY_SKIN;
    }

    /**
     * Combines both sides into the final order of the tab list.
     */
    private static List<Slot> arrange(List<List<Slot>> left, List<List<Slot>> right) {
        for (int rows = MIN_COLUMN_ROWS; rows <= MAX_ROWS; rows++) {
            List<List<Slot>> leftColumns = toColumns(left, rows);
            List<List<Slot>> rightColumns = toColumns(right, rows);
            int columns = leftColumns.size() + rightColumns.size();
            // The client uses the fewest columns that keep each one at 20 rows or less,
            // so the entries must not fit into one column less than intended
            if (columns > MAX_COLUMNS || columns * rows <= MAX_ROWS * (columns - 1)) {
                continue;
            }
            List<Slot> slots = new ArrayList<>();
            for (List<Slot> column : leftColumns) {
                slots.addAll(column);
            }
            for (List<Slot> column : rightColumns) {
                slots.addAll(column);
            }
            return slots;
        }

        // Too many players to split into sides, so everyone is listed in order instead
        List<Slot> slots = new ArrayList<>();
        for (List<List<Slot>> side : List.of(left, right)) {
            for (List<Slot> group : side) {
                slots.addAll(group);
            }
        }
        return slots;
    }

    /**
     * Splits the groups of a side into columns of the given number of rows, each padded with blank rows.
     */
    private static List<List<Slot>> toColumns(List<List<Slot>> groups, int rows) {
        List<List<Slot>> columns = new ArrayList<>();
        List<Slot> column = new ArrayList<>();
        for (List<Slot> group : groups) {
            if (!column.isEmpty()) {
                if (column.size() + 1 + group.size() <= rows) {
                    column.add(new Slot(null, null, 0, false));
                } else {
                    columns.add(column);
                    column = new ArrayList<>();
                }
            }
            Slot divider = group.getFirst();
            for (Slot slot : group) {
                if (column.size() == rows) {
                    columns.add(column);
                    column = new ArrayList<>();
                    column.add(divider);
                }
                column.add(slot);
            }
        }
        if (!column.isEmpty()) {
            columns.add(column);
        }
        for (List<Slot> filled : columns) {
            while (filled.size() < rows) {
                filled.add(new Slot(null, null, 0, false));
            }
        }
        return columns;
    }

    /**
     * Provides the divider of a group, with its label centered between two lines that span the width of the column.
     *
     * @param viewer     The player who sees the divider.
     * @param group      The group of the divider.
     * @param count      The number of players in the group.
     * @param widestName The width in pixels of the widest player name in the tab list.
     */
    private static String buildDivider(Player viewer, TabGroup group, int count, int widestName) {
        String groupName = Lang.getFor(viewer, group.getLangKey());
        String langKey = count > 0 ? "tab.group_header" : "tab.group_header_empty";
        int labelWidth = getTextWidth(ChatUtils.translateToColor(Lang.getFor(viewer, langKey,
                "left", "", "group", groupName, "right", "")));
        // The leading space counts towards the width, so the divider still spans the widest name
        int width = Math.max(widestName - SPACE_WIDTH, labelWidth + MIN_LINE_SPACES * 2 * SPACE_WIDTH);
        int lineSpaces = (width - labelWidth) / SPACE_WIDTH;
        int leftSpaces = lineSpaces / 2;
        return " " + ChatUtils.translateToColor(Lang.getFor(viewer, langKey,
                "left", " ".repeat(leftSpaces), "group", groupName, "right", " ".repeat(lineSpaces - leftSpaces)));
    }

    /**
     * Sends the arranged tab list to the viewer, adding or removing divider and filler entries as needed.
     */
    private static void sendLayout(Player viewer, List<Slot> slots, int widestName, Map<String, String[]> skins) {
        // The first layout sent to a viewer also clears any entries left over from before they joined this server
        List<String> shownSkins = shownLayoutSkins.get(viewer.getUniqueId());
        int removeUpTo = shownSkins != null ? shownSkins.size() : MAX_ROWS * MAX_COLUMNS;
        List<String> newShownSkins = new ArrayList<>();

        // The dividers are all as wide as the widest of them, so they line up with each other
        int dividerWidth = widestName;
        for (Slot slot : slots) {
            if (slot.dividerGroup() != null) {
                dividerWidth = Math.max(dividerWidth, getTextWidth(buildDivider(viewer, slot.dividerGroup(), slot.dividerCount(), 0)));
            }
        }

        Map<UUID, Integer> orders = new HashMap<>();
        Map<UUID, String> layoutNames = new HashMap<>();
        Map<UUID, NetworkTabManager.LayoutEntry> newEntries = new LinkedHashMap<>();
        int layoutIndex = 0;
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            int order = slots.size() - 1 - i;
            if (slot.playerUuid() != null) {
                orders.put(slot.playerUuid(), order);
                continue;
            }
            UUID layoutUuid = getLayoutUuid(layoutIndex);
            // The head of an entry can only be changed by removing it and adding it again
            String skin = getSlotSkin(slot);
            if (shownSkins == null || layoutIndex >= shownSkins.size() || !shownSkins.get(layoutIndex).equals(skin)) {
                String[] texture = skins.get(skin);
                newEntries.put(layoutUuid, new NetworkTabManager.LayoutEntry("ac_tab_" + layoutIndex, texture[0], texture[1]));
            }
            newShownSkins.add(skin);
            orders.put(layoutUuid, order);
            String layoutName = "";
            if (slot.dividerGroup() != null) {
                layoutName = buildDivider(viewer, slot.dividerGroup(), slot.dividerCount(), dividerWidth);
            } else if (slot.isEmptyNotice()) {
                // Centered under the divider above it
                String notice = ChatUtils.translateToColor(Lang.getFor(viewer, "tab.group_empty"));
                int leftSpaces = Math.max(0, (dividerWidth - getTextWidth(notice)) / 2 / SPACE_WIDTH);
                layoutName = " ".repeat(leftSpaces) + notice;
            }
            layoutNames.put(layoutUuid, layoutName);
            layoutIndex++;
        }

        // The client ignores adding an entry it already has, so an entry whose head changes is removed before being added again
        List<UUID> removedEntries = new ArrayList<>();
        for (int i = 0; i < removeUpTo; i++) {
            UUID layoutUuid = getLayoutUuid(i);
            if (i >= layoutIndex || newEntries.containsKey(layoutUuid)) {
                removedEntries.add(layoutUuid);
            }
        }

        NetworkTabManager.removeEntries(viewer, removedEntries);
        NetworkTabManager.addLayoutEntries(viewer, newEntries);
        NetworkTabManager.sendDisplayNames(viewer, layoutNames);
        NetworkTabManager.sendListOrders(viewer, orders);
        shownLayoutSkins.put(viewer.getUniqueId(), newShownSkins);
    }

    /**
     * Provides the approximate width in pixels of the input text in the default Minecraft font.
     *
     * @param text The §-formatted text.
     * @return The width of the text in pixels.
     */
    public static int getTextWidth(String text) {
        int width = 0;
        boolean isBold = false;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            if (codePoint == '§' && i < text.length()) {
                char code = Character.toLowerCase(text.charAt(i));
                i++;
                if (code == 'l') {
                    isBold = true;
                } else if (code != 'k' && code != 'm' && code != 'n' && code != 'o') {
                    // Colors and resets remove the bold formatting
                    isBold = false;
                }
                continue;
            }
            width += getCharWidth(codePoint) + (isBold ? 1 : 0);
        }
        return width;
    }

    /**
     * Provides the width in pixels of a character in the default Minecraft font, including the 1 pixel gap after it.
     */
    private static int getCharWidth(int codePoint) {
        return switch (codePoint) {
            case '!', '\'', ',', '.', ':', ';', '|', 'i' -> 2;
            case '`', 'l' -> 3;
            case ' ', '"', '(', ')', '*', 'I', '[', ']', 't', '{', '}' -> 4;
            case '<', '>', 'f', 'k' -> 5;
            case '@', '~' -> 7;
            default -> codePoint <= 0xFF ? 6 : 9;
        };
    }

    /**
     * Provides the UUID of the divider or filler entry at the input index, which is the same for every viewer.
     */
    private static UUID getLayoutUuid(int index) {
        return UUID.nameUUIDFromBytes(("AranarthTabLayout:" + index).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Forgets the divider and filler entries shown to a player who left, as their client no longer has them.
     *
     * @param uuid The UUID of the player who left.
     */
    public static void clearViewer(UUID uuid) {
        shownLayoutSkins.remove(uuid);
    }
}
