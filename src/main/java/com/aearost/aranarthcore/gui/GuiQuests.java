package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.enums.QuestType;
import com.aearost.aranarthcore.objects.*;
import com.aearost.aranarthcore.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
 * A 45-slot chest GUI displaying the player's current daily and weekly quests, and their full-board bonuses.
 */
public class GuiQuests {

    public static final String TITLE_KEY = "gui.quests.title";
    public static final int[] DAILY_SLOTS = {4, 11, 15};
    public static final int[] WEEKLY_SLOTS = {29, 33, 40};
    public static final int DAILY_BONUS_SLOT = 13;
    public static final int WEEKLY_BONUS_SLOT = 31;
    public static final int INFO_SLOT = 22;

    private static final ZoneId EST = ZoneId.of("America/New_York");

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
        // Async timezone lookup - update info item once resolved
        AranarthUtils.getPlayerTimezone(player, zoneId ->
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () ->
                gui.setItem(INFO_SLOT, makeInfoItem(player, rank, zoneId != null ? zoneId : EST))
            )
        );
    }

    private Inventory initializeGui() {
        Inventory inv = GuiHolder.create(player, 45, Lang.getFor(player, TITLE_KEY));

        ItemStack yellowPane = makePane(Material.YELLOW_STAINED_GLASS_PANE);
        ItemStack blackPane = makePane(Material.BLACK_STAINED_GLASS_PANE);
        int[] yellow = {0, 1, 7, 8, 10, 16, 19, 25, 28, 34, 36, 37, 43, 44};
        for (int slot = 0; slot < 45; slot++) {
            inv.setItem(slot, blackPane);
        }
        for (int slot : yellow) {
            inv.setItem(slot, yellowPane);
        }

        refresh(player, inv);
        // Info book with EST fallback (updated async via openGui)
        inv.setItem(INFO_SLOT, makeInfoItem(player, rank, EST));
        return inv;
    }

    /**
     * Updates every quest and bonus item in place, without moving the player's cursor.
     */
    public static void refresh(Player player, Inventory inv) {
        UUID uuid = player.getUniqueId();
        PlayerQuestBoard board = QuestUtils.getBoard(uuid);
        for (QuestType type : QuestType.values()) {
            int[] slots = type == QuestType.DAILY ? DAILY_SLOTS : WEEKLY_SLOTS;
            List<Quest> quests = board.getQuests(type);
            for (int i = 0; i < 3; i++) {
                inv.setItem(slots[i], i < quests.size()
                        ? makeQuestItem(player, board, quests.get(i))
                        : makeNoQuestItem(player));
            }
            inv.setItem(type == QuestType.DAILY ? DAILY_BONUS_SLOT : WEEKLY_BONUS_SLOT, makeBonusItem(player, board, type));
        }
    }

    public static ItemStack makeQuestItem(Player player, PlayerQuestBoard board, Quest quest) {
        Material mat;
        String statusText;
        if (quest.isClaimed()) {
            mat = Material.GRAY_CONCRETE;
            statusText = Lang.getFor(player, "gui.quests.completed");
        } else if (quest.isCompleted()) {
            mat = Material.LIME_CONCRETE;
            statusText = Lang.getFor(player, "gui.quests.click_claim");
        } else if (quest.hasStarted()) {
            mat = Material.YELLOW_CONCRETE;
            statusText = Lang.getFor(player, "gui.quests.in_progress");
        } else {
            mat = Material.RED_CONCRETE;
            statusText = Lang.getFor(player, "gui.quests.not_started");
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&f&l" + QuestUtils.getDisplayName(player, quest)));

        List<String> lore = new ArrayList<>();
        lore.add(getJobLine(player, quest));
        QuestDefinition def = quest.isContract() ? null : QuestConfig.getDefinition(quest.getId());
        if (def != null && def.getLevel() > 1) {
            lore.add(Lang.getFor(player, "gui.quests.level", "level", def.getLevel()));
        }
        String typeName = Lang.getFor(player, quest.getType() == QuestType.DAILY ? "quest.type_daily" : "quest.type_weekly");
        lore.add(Lang.getFor(player, "gui.quests.type", "type", typeName));
        lore.add("");

        if (quest.isContract()) {
            QuestContract contract = QuestConfig.getContract(quest.getId());
            for (int part = 0; part < quest.getPartCount() && contract != null; part++) {
                String partName = QuestUtils.getPartName(player, contract.getParts().get(part).questId(),
                        quest.getVariant(part), quest.getRequired(part));
                lore.add(Lang.getFor(player, "gui.quests.part_progress", "name", partName,
                        "progress", QuestUtils.formatNumber(quest.getProgress(part)),
                        "required", QuestUtils.formatNumber(quest.getRequired(part))));
            }
        } else {
            lore.add(Lang.getFor(player, "gui.quests.progress",
                    "progress", QuestUtils.formatNumber(quest.getProgress()),
                    "required", QuestUtils.formatNumber(quest.getRequired())));
        }
        if (quest.hasItemReward()) {
            lore.add(Lang.getFor(player, "gui.quests.reward_item", "item", QuestRewardUtils.getDisplayName(quest.getItemReward())));
        } else {
            lore.add(Lang.getFor(player, "gui.quests.reward_money", "money", QuestUtils.formatNumber(quest.getReward())));
        }
        addJobRewardLines(player, quest, lore);
        lore.add("");
        lore.add(statusText);
        if (!quest.isCompleted()) {
            lore.add(board.isRerollUsed(quest.getType())
                    ? Lang.getFor(player, "gui.quests.reroll_used")
                    : Lang.getFor(player, "gui.quests.reroll", "amount", QuestUtils.formatNumber(QuestUtils.getRerollCost(quest))));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * @return The line naming the job a quest belongs to, in the job's colour.
     */
    private static String getJobLine(Player player, Quest quest) {
        List<JobType> jobs = QuestUtils.getQuestJobs(quest);
        if (jobs.isEmpty()) {
            return Lang.getFor(player, "gui.quests.job_general");
        }
        List<String> names = new ArrayList<>();
        for (JobType job : jobs) {
            names.add(JobUtils.getJobColor(job) + Lang.getFor(player, job.getNameKey()));
        }
        return Lang.getFor(player, quest.isContract() ? "gui.quests.job_contract" : "gui.quests.job",
                "job", String.join(ChatUtils.translateToColor(" &7+ "), names));
    }

    /**
     * Shows the job XP a quest gives in each job the player holds, or the level 10 bonus.
     */
    private static void addJobRewardLines(Player player, Quest quest, List<String> lore) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer == null || aranarthPlayer.getJobData() == null) {
            return;
        }
        JobData jobData = aranarthPlayer.getJobData();
        double percent = quest.getType() == QuestType.DAILY ? QuestConfig.getDailyXpPercent() : QuestConfig.getWeeklyXpPercent();
        String percentText = percent == Math.floor(percent) ? String.valueOf((int) percent) : String.valueOf(percent);
        for (JobType job : QuestUtils.getQuestJobs(quest)) {
            if (!jobData.hasJob(job)) {
                continue;
            }
            if (jobData.getLevel(job) >= 10) {
                int bonus = quest.getType() == QuestType.DAILY
                        ? QuestConfig.getMasterDailyBonus(quest.getRank())
                        : QuestConfig.getMasterWeeklyBonus(quest.getRank());
                lore.add(Lang.getFor(player, "gui.quests.master_reward", "amount", QuestUtils.formatNumber(bonus),
                        "job", Lang.getFor(player, job.getNameKey())));
            } else {
                lore.add(Lang.getFor(player, "gui.quests.xp_reward", "percent", percentText,
                        "job", Lang.getFor(player, job.getNameKey())));
            }
        }
    }

    private static ItemStack makeBonusItem(Player player, PlayerQuestBoard board, QuestType type) {
        boolean isClaimed = board.isBonusClaimed(type);
        boolean isReady = board.isAllClaimed(type) && !isClaimed;
        ItemStack item = new ItemStack(isClaimed ? Material.MINECART : isReady ? Material.ENDER_CHEST : Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, type == QuestType.DAILY ? "gui.quests.bonus_daily" : "gui.quests.bonus_weekly"));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.getFor(player, type == QuestType.DAILY ? "gui.quests.bonus_options_daily" : "gui.quests.bonus_options_weekly",
                "percent", (int) QuestConfig.getBoardBonusPercent()));
        lore.add("");
        lore.add(Lang.getFor(player, isClaimed ? "gui.quests.bonus_claimed" : isReady ? "gui.quests.bonus_ready" : "gui.quests.bonus_locked"));
        meta.setLore(lore);
        if (isReady) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makePane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(" "));
        pane.setItemMeta(meta);
        return pane;
    }

    private static ItemStack makeNoQuestItem(Player player) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.quests.no_quest"));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack makeInfoItem(Player player, int rank, ZoneId zone) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.quests.info"));

        String zoneName = zone.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

        List<String> lore = new ArrayList<>();
        lore.add(Lang.getFor(player, "gui.quests.your_rank", "rank", String.valueOf(rank + 1)));
        lore.add("");
        lore.add(Lang.getFor(player, "gui.quests.daily_reset"));
        lore.add(Lang.getFor(player, "gui.quests.reset_time", "time", formatNextDailyReset(player, zone), "zone", zoneName));
        lore.add("");
        lore.add(Lang.getFor(player, "gui.quests.weekly_reset"));
        lore.add(Lang.getFor(player, "gui.quests.reset_time", "time", formatNextWeeklyReset(player, zone), "zone", zoneName));
        lore.add("");
        lore.add(Lang.getFor(player, "gui.quests.claim_hint"));
        lore.add(Lang.getFor(player, "gui.quests.reroll_hint"));
        lore.add(Lang.getFor(player, "gui.quests.bonus_hint"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Formats the next daily reset time (3 AM EST) in the given timezone.
     */
    private static String formatNextDailyReset(Player player, ZoneId playerZone) {
        LocalDateTime nowEst = LocalDateTime.now(EST);
        LocalDate nextDate = nowEst.getHour() < 3 ? nowEst.toLocalDate() : nowEst.toLocalDate().plusDays(1);
        ZonedDateTime nextReset = nextDate.atTime(3, 0).atZone(EST).withZoneSameInstant(playerZone);
        return formatZonedDateTime(player, nextReset);
    }

    /**
     * Formats the next weekly reset time (Sunday 3 AM EST) in the given timezone.
     */
    private static String formatNextWeeklyReset(Player player, ZoneId playerZone) {
        LocalDateTime nowEst = LocalDateTime.now(EST);
        int daysUntilSunday = (7 - nowEst.getDayOfWeek().getValue()) % 7;
        // If today is Sunday but past 3 AM, move to next Sunday
        if (daysUntilSunday == 0 && nowEst.getHour() >= 3) {
            daysUntilSunday = 7;
        }
        LocalDate nextSunday = nowEst.toLocalDate().plusDays(daysUntilSunday);
        ZonedDateTime nextReset = nextSunday.atTime(3, 0).atZone(EST).withZoneSameInstant(playerZone);
        return formatZonedDateTime(player, nextReset);
    }

    private static String formatZonedDateTime(Player player, ZonedDateTime dt) {
        String month = getMonthName(player, dt.getMonthValue());
        int hour = dt.getHour();
        int minute = dt.getMinute();
        String amPm = hour < 12 ? "AM" : "PM";
        int hour12 = hour % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        String minuteStr = minute < 10 ? "0" + minute : String.valueOf(minute);
        return Lang.getFor(player, "quest.reset_time", "month", month, "day", dt.getDayOfMonth(), "time", hour12 + ":" + minuteStr + " " + amPm);
    }

    private static String getMonthName(Player player, int month) {
        return switch (month) {
            case 1 -> Lang.getFor(player, "date.month_short_jan"); case 2 -> Lang.getFor(player, "date.month_short_feb");
            case 3 -> Lang.getFor(player, "date.month_short_mar"); case 4 -> Lang.getFor(player, "date.month_short_apr");
            case 5 -> Lang.getFor(player, "date.month_short_may"); case 6 -> Lang.getFor(player, "date.month_short_jun");
            case 7 -> Lang.getFor(player, "date.month_short_jul"); case 8 -> Lang.getFor(player, "date.month_short_aug");
            case 9 -> Lang.getFor(player, "date.month_short_sep"); case 10 -> Lang.getFor(player, "date.month_short_oct");
            case 11 -> Lang.getFor(player, "date.month_short_nov"); case 12 -> Lang.getFor(player, "date.month_short_dec");
            default -> "???";
        };
    }
}
