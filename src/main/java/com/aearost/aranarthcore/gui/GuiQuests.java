package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Quest;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.QuestUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * A 45-slot chest GUI displaying the player's current daily and weekly quests.
 */
public class GuiQuests {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getInstance();
    private static final ZoneId EST = ZoneId.of("America/New_York");

    static {
        MONEY_FORMAT.setGroupingUsed(true);
    }

    private final Player player;
    private final Inventory gui;
    private final int rank;

    public GuiQuests(Player player) {
        this.player = player;
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        this.rank = aranarthPlayer.getRank();
        this.gui = initializeGui();
    }

    public void openGui() {
        player.openInventory(gui);
        // Async timezone lookup — update info item (slot 22) once resolved
        AranarthUtils.getPlayerTimezone(player, zoneId ->
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () ->
                gui.setItem(22, makeInfoItem(rank, zoneId))
            )
        );
    }

    private Inventory initializeGui() {
        Inventory inv = Bukkit.createInventory(player, 45, ChatUtils.translateToColor("&8&lYour Quests"));

        UUID uuid = player.getUniqueId();

        ItemStack yellowPane = makePane(Material.YELLOW_STAINED_GLASS_PANE, " ");
        ItemStack blackPane = makePane(Material.BLACK_STAINED_GLASS_PANE, " ");

        // Row 1 (0-8)
        inv.setItem(0, yellowPane);
        inv.setItem(1, yellowPane);
        inv.setItem(2, blackPane);
        inv.setItem(3, blackPane);
        // slot 4: daily quest 0
        inv.setItem(5, blackPane);
        inv.setItem(6, blackPane);
        inv.setItem(7, yellowPane);
        inv.setItem(8, yellowPane);
        // Row 2 (9-17)
        inv.setItem(9, blackPane);
        inv.setItem(10, yellowPane);
        // slot 11: daily quest 1
        inv.setItem(12, blackPane);
        inv.setItem(13, blackPane);
        inv.setItem(14, blackPane);
        // slot 15: daily quest 2
        inv.setItem(16, yellowPane);
        inv.setItem(17, blackPane);
        // Row 3 (18-26)
        inv.setItem(18, blackPane);
        inv.setItem(19, yellowPane);
        inv.setItem(20, blackPane);
        inv.setItem(21, blackPane);
        // slot 22: info book
        inv.setItem(23, blackPane);
        inv.setItem(24, blackPane);
        inv.setItem(25, yellowPane);
        inv.setItem(26, blackPane);
        // Row 4 (27-35)
        inv.setItem(27, blackPane);
        inv.setItem(28, yellowPane);
        // slot 29: weekly quest 0
        inv.setItem(30, blackPane);
        inv.setItem(31, blackPane);
        inv.setItem(32, blackPane);
        // slot 33: weekly quest 1
        inv.setItem(34, yellowPane);
        inv.setItem(35, blackPane);
        // Row 5 (36-44)
        inv.setItem(36, yellowPane);
        inv.setItem(37, yellowPane);
        inv.setItem(38, blackPane);
        inv.setItem(39, blackPane);
        // slot 40: weekly quest 2
        inv.setItem(41, blackPane);
        inv.setItem(42, blackPane);
        inv.setItem(43, yellowPane);
        inv.setItem(44, yellowPane);

        // Daily quest items at 4, 11, 15
        List<Quest> dailyQuests = QuestUtils.getActiveDailyQuests(uuid, rank);
        int[] dailySlots = {4, 11, 15};
        for (int i = 0; i < 3; i++) {
            if (i < dailyQuests.size()) {
                int progress = QuestUtils.getDailyProgress(uuid, i);
                boolean completed = QuestUtils.isDailyCompleted(uuid, i);
                boolean claimed = QuestUtils.isDailyClaimed(uuid, i);
                inv.setItem(dailySlots[i], makeQuestItem(dailyQuests.get(i), progress, completed, claimed));
            } else {
                inv.setItem(dailySlots[i], makeNoQuestItem());
            }
        }

        // Weekly quest items at 29, 33, 40
        List<Quest> weeklyQuests = QuestUtils.getActiveWeeklyQuests(uuid, rank);
        int[] weeklySlots = {29, 33, 40};
        for (int i = 0; i < 3; i++) {
            if (i < weeklyQuests.size()) {
                int progress = QuestUtils.getWeeklyProgress(uuid, i);
                boolean completed = QuestUtils.isWeeklyCompleted(uuid, i);
                boolean claimed = QuestUtils.isWeeklyClaimed(uuid, i);
                inv.setItem(weeklySlots[i], makeQuestItem(weeklyQuests.get(i), progress, completed, claimed));
            } else {
                inv.setItem(weeklySlots[i], makeNoQuestItem());
            }
        }

        // Info book at slot 22 with EST fallback (updated async via openGui)
        inv.setItem(22, makeInfoItem(rank, EST));

        return inv;
    }

    public static ItemStack makeQuestItem(Quest quest, int progress, boolean claimable, boolean completed) {
        Material mat;
        String statusColor;
        String statusText;

        if (completed) {
            mat = Material.GRAY_CONCRETE;
            statusColor = "&7";
            statusText = "Completed";
        } else if (claimable) {
            mat = Material.LIME_CONCRETE;
            statusColor = "&a";
            statusText = "Click to Claim!";
        } else if (progress > 0) {
            mat = Material.YELLOW_CONCRETE;
            statusColor = "&e";
            statusText = "In Progress";
        } else {
            mat = Material.RED_CONCRETE;
            statusColor = "&c";
            statusText = "Not Started";
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&f&l" + quest.getDisplayName()));

        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&8" + quest.getQuestType().name().charAt(0)
                + quest.getQuestType().name().substring(1).toLowerCase() + " Quest"));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Progress: &f" + progress + " &8/ &f" + quest.getRequired()));
        if (quest.hasItemReward()) {
            lore.add(ChatUtils.translateToColor("&7Reward: &f" + QuestUtils.getItemRewardDisplayName(quest.getItemReward())));
        } else {
            lore.add(ChatUtils.translateToColor("&7Reward: &6$" + MONEY_FORMAT.format(quest.getReward())));
        }
        lore.add("");
        lore.add(ChatUtils.translateToColor(statusColor + statusText));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makePane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(name));
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack makeNoQuestItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&cNo Quest Available"));
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack makeInfoItem(int rank, ZoneId zone) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&e&lQuest Info"));

        String nextDailyStr = formatNextDailyReset(zone);
        String nextWeeklyStr = formatNextWeeklyReset(zone);
        String zoneName = zone.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Your Rank: &f" + (rank + 1)));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&6Next daily reset:"));
        lore.add(ChatUtils.translateToColor("&f  " + nextDailyStr + " &7(" + zoneName + ")"));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&bNext weekly reset:"));
        lore.add(ChatUtils.translateToColor("&f  " + nextWeeklyStr + " &7(" + zoneName + ")"));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Click a &alime &7quest to claim its reward."));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Formats the next daily reset time (3 AM EST) in the given timezone.
     */
    private String formatNextDailyReset(ZoneId playerZone) {
        LocalDateTime nowEst = LocalDateTime.now(EST);
        LocalDate nextDate;
        if (nowEst.getHour() < 3) {
            nextDate = nowEst.toLocalDate();
        } else {
            nextDate = nowEst.toLocalDate().plusDays(1);
        }
        ZonedDateTime nextReset = nextDate.atTime(3, 0).atZone(EST).withZoneSameInstant(playerZone);
        return formatZonedDateTime(nextReset);
    }

    /**
     * Formats the next weekly reset time (Sunday 3 AM EST) in the given timezone.
     */
    private String formatNextWeeklyReset(ZoneId playerZone) {
        LocalDateTime nowEst = LocalDateTime.now(EST);
        int daysUntilSunday = (7 - nowEst.getDayOfWeek().getValue()) % 7;
        // If today is Sunday but past 3 AM, move to next Sunday
        if (daysUntilSunday == 0 && nowEst.getHour() >= 3) {
            daysUntilSunday = 7;
        }
        LocalDate nextSunday = nowEst.toLocalDate().plusDays(daysUntilSunday);
        ZonedDateTime nextReset = nextSunday.atTime(3, 0).atZone(EST).withZoneSameInstant(playerZone);
        return formatZonedDateTime(nextReset);
    }

    private String formatZonedDateTime(ZonedDateTime dt) {
        String month = getMonthName(dt.getMonthValue());
        int day = dt.getDayOfMonth();
        int hour = dt.getHour();
        int minute = dt.getMinute();
        String amPm = hour < 12 ? "AM" : "PM";
        int hour12 = hour % 12;
        if (hour12 == 0) hour12 = 12;
        String minuteStr = minute < 10 ? "0" + minute : String.valueOf(minute);
        return month + " " + day + " at " + hour12 + ":" + minuteStr + " " + amPm;
    }

    private String getMonthName(int month) {
        return switch (month) {
            case 1 -> "Jan"; case 2 -> "Feb"; case 3 -> "Mar"; case 4 -> "Apr";
            case 5 -> "May"; case 6 -> "Jun"; case 7 -> "Jul"; case 8 -> "Aug";
            case 9 -> "Sep"; case 10 -> "Oct"; case 11 -> "Nov"; case 12 -> "Dec";
            default -> "???";
        };
    }
}
