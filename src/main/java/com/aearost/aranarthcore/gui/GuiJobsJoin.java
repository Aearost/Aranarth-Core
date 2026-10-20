package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.JobUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiJobsJoin {

    public static final String TITLE_KEY = "gui.jobsjoin.title";

    public static final int[] JOB_SLOTS = {11, 13, 15, 20, 22, 24, 29, 31};
    public static final JobType[] JOB_ORDER = {
        JobType.BUILDER, JobType.MINER, JobType.EXCAVATOR,
        JobType.LUMBERJACK, JobType.FARMER, JobType.HUNTER,
        JobType.ALCHEMIST, JobType.EXPLORER
    };

    private final Player player;
    private final Inventory gui;

    public GuiJobsJoin(Player player) {
        this.player = player;
        this.gui = initializeGui();
    }

    public void openGui() {
        player.openInventory(gui);
    }

    /**
     * Updates an already-open join-jobs inventory in-place without closing it.
     */
    public void populateInto(org.bukkit.inventory.Inventory inv) {
        for (int i = 0; i < gui.getSize(); i++) {
            inv.setItem(i, gui.getItem(i));
        }
    }

    private Inventory initializeGui() {
        Inventory inv = GuiHolder.create(player, 45, Lang.getFor(player, TITLE_KEY));

        ItemStack grayPane = makePane(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, grayPane);
        }

        for (int i = 36; i < 45; i++) {
            inv.setItem(i, grayPane);
        }

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        JobData jobData = ap.getJobData();
        int maxJobs = JobUtils.getMaxJobs(ap.getRank());
        int activeCount = jobData.getActiveJobs().size();
        boolean slotsFull = activeCount >= maxJobs;

        for (int i = 0; i < JOB_ORDER.length; i++) {
            JobType job = JOB_ORDER[i];
            inv.setItem(JOB_SLOTS[i], makeJobItem(job, jobData, slotsFull, ap.getRank()));
        }

        // Back button
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(Lang.getFor(player, "gui.back"));
        back.setItemMeta(backMeta);
        inv.setItem(40, back);

        return inv;
    }

    private ItemStack makeJobItem(JobType job, JobData jobData, boolean slotsFull, int rank) {
        ItemStack item = new ItemStack(JobUtils.getJobIcon(job));
        ItemMeta meta = item.getItemMeta();

        boolean alreadyJoined = jobData.hasJob(job);
        String statusColor;
        String statusText;

        if (alreadyJoined) {
            statusColor = "&a";
            statusText = Lang.getFor(player, "gui.jobsjoin.already_joined");
        } else if (slotsFull) {
            statusColor = "&c";
            statusText = Lang.getFor(player, "gui.jobsjoin.slots_full");
        } else {
            statusColor = "&e";
            statusText = Lang.getFor(player, "gui.jobsjoin.click_join");
        }

        meta.setDisplayName(ChatUtils.translateToColor(JobUtils.getJobColor(job) + Lang.getFor(player, job.getNameKey())));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.getFor(player, "jobs.desc_" + job.name().toLowerCase()));
        lore.add("");
        lore.add(Lang.getFor(player, "gui.jobsjoin.actions_lore"));

        for (String action : getJobActions(job)) {
            lore.add(ChatUtils.translateToColor("&8 - &7" + action));
        }

        lore.add("");

        if (alreadyJoined) {
            int level = jobData.getLevel(job);
            double currentXp = jobData.getCurrentXp(job);
            long required = JobUtils.getXpRequired(level);
            String xpStr = level >= 10 ? Lang.getFor(player, "jobs.max_level") : (int) currentXp + " / " + required;
            lore.add(Lang.getFor(player, "gui.jobsjoin.level", "level", String.valueOf(level)));
            lore.add(Lang.getFor(player, "gui.jobsjoin.xp", "xp", xpStr));
            lore.add("");
        }

        lore.add(ChatUtils.translateToColor(statusColor + statusText));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> getJobActions(JobType job) {
        return switch (job) {
            case BUILDER -> List.of(Lang.getFor(player, "jobs.action_builder"));
            case FARMER -> List.of(Lang.getFor(player, "jobs.action_farmer_crops"), Lang.getFor(player, "jobs.action_farmer_honey"), Lang.getFor(player, "jobs.action_farmer_mobs"));
            case MINER -> List.of(Lang.getFor(player, "jobs.action_miner_stone"), Lang.getFor(player, "jobs.action_miner_ores"), Lang.getFor(player, "jobs.action_miner_amethyst"));
            case EXCAVATOR -> List.of(Lang.getFor(player, "jobs.action_excavator_dig"), Lang.getFor(player, "jobs.action_excavator_brush"));
            case LUMBERJACK -> List.of(Lang.getFor(player, "jobs.action_lumberjack_chop"), Lang.getFor(player, "jobs.action_lumberjack_craft"));
            case EXPLORER -> List.of(Lang.getFor(player, "jobs.action_explorer_travel"), Lang.getFor(player, "jobs.action_explorer_chests"));
            case ALCHEMIST -> List.of(Lang.getFor(player, "jobs.action_alchemist_brew"), Lang.getFor(player, "jobs.action_alchemist_enchant"));
            case HUNTER -> List.of(Lang.getFor(player, "jobs.action_hunter_kill"), Lang.getFor(player, "jobs.action_hunter_fish"));
        };
    }

    private ItemStack makePane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(" "));
        pane.setItemMeta(meta);
        return pane;
    }
}
