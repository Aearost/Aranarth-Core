package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.JobUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiJobsJoin {

    public static final String TITLE = "Join a Job";

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
        Inventory inv = Bukkit.createInventory(player, 45, ChatUtils.translateToColor("&8&lJoin a Job"));

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
        backMeta.setDisplayName(ChatUtils.translateToColor("&7Back"));
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
            statusText = "Already Joined";
        } else if (slotsFull) {
            statusColor = "&c";
            statusText = "Job Slots Full";
        } else {
            statusColor = "&e";
            statusText = "Click to Join!";
        }

        meta.setDisplayName(ChatUtils.translateToColor(JobUtils.getJobColor(job) + job.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7" + JobUtils.getJobDescription(job)));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Actions that earn money:"));

        for (String action : getJobActions(job)) {
            lore.add(ChatUtils.translateToColor("&8 - &7" + action));
        }

        lore.add("");

        if (alreadyJoined) {
            int level = jobData.getLevel(job);
            double currentXp = jobData.getCurrentXp(job);
            long required = JobUtils.getXpRequired(level);
            String xpStr = level >= 10 ? "Max Level" : (int) currentXp + " / " + required;
            lore.add(ChatUtils.translateToColor("&7Level: &e" + level));
            lore.add(ChatUtils.translateToColor("&7XP: &e" + xpStr));
            lore.add("");
        }

        lore.add(ChatUtils.translateToColor(statusColor + statusText));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> getJobActions(JobType job) {
        return switch (job) {
            case BUILDER -> List.of("Place buildable blocks &8($1.00)");
            case FARMER -> List.of("Harvest crops &8($0.60-$2.40)", "Collect honey &8($9.60-$12.00)", "Kill passive mobs &8($1.20)");
            case MINER -> List.of("Mine stone &8($0.03-$0.07)", "Mine ores &8($1.90-$75.00)", "Harvest amethyst &8($4.80)");
            case EXCAVATOR -> List.of("Dig dirts, sands, etc &8($0.05-$0.10)", "Brush artifacts &8($25.00)");
            case LUMBERJACK -> List.of("Chop logs &8($1.45-$2.90)", "Craft/saw wood items &8($1.20-$6.00)");
            case EXPLORER -> List.of("Walk/ride &8($0.25-$0.50/block)", "Open natural chests &8($100-$250)");
            case ALCHEMIST -> List.of("Brew potions &8($19.20-$48.00)", "Enchant items &8($24.00)");
            case HUNTER -> List.of("Kill mobs &8($4.80-$250.00)", "Catch fish &8($4.80-$36.00)");
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
