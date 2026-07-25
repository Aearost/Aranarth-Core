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

    // 9 jobs in a 3x3 grid: slots 10, 12, 14, 19, 21, 23, 28, 30, 32
    public static final int[] JOB_SLOTS = {10, 12, 14, 19, 21, 23, 28, 30, 32};
    public static final JobType[] JOB_ORDER = {
        JobType.BUILDER, JobType.FARMER, JobType.MINER,
        JobType.EXCAVATOR, JobType.LUMBERJACK, JobType.SMITH,
        JobType.EXPLORER, JobType.ALCHEMIST, JobType.HUNTER
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

    private Inventory initializeGui() {
        Inventory inv = Bukkit.createInventory(player, 54, ChatUtils.translateToColor("&8&lJoin a Job"));

        ItemStack yellowPane = makePane(Material.YELLOW_STAINED_GLASS_PANE);
        ItemStack blackPane = makePane(Material.BLACK_STAINED_GLASS_PANE);

        // Fill border with panes
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, blackPane);
        }
        // Corner yellows
        inv.setItem(0, yellowPane);
        inv.setItem(8, yellowPane);
        inv.setItem(45, yellowPane);
        inv.setItem(53, yellowPane);

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
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatUtils.translateToColor("&7Back"));
        back.setItemMeta(backMeta);
        inv.setItem(49, back);

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

        meta.setDisplayName(ChatUtils.translateToColor("&6&l" + job.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7" + JobUtils.getJobDescription(job)));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Actions that earn money:"));

        for (String action : getJobActions(job)) {
            lore.add(ChatUtils.translateToColor("&8 - &f" + action));
        }

        lore.add("");

        if (alreadyJoined) {
            int level = jobData.getLevel(job);
            double currentXp = jobData.getCurrentXp(job);
            long required = JobUtils.getXpRequired(level);
            String xpStr = level >= 10 ? "Max Level" : (int) currentXp + " / " + required;
            lore.add(ChatUtils.translateToColor("&7Level: &f" + level));
            lore.add(ChatUtils.translateToColor("&7XP: &f" + xpStr));
            lore.add("");
        }

        lore.add(ChatUtils.translateToColor(statusColor + statusText));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> getJobActions(JobType job) {
        return switch (job) {
            case BUILDER -> List.of("Place buildable blocks &7($0.10)");
            case FARMER -> List.of("Harvest crops &7($0.05-$0.20)", "Collect honey &7($0.50)", "Kill passive mobs &7($0.10)");
            case MINER -> List.of("Mine stone/ores &7($0.02-$3.00)", "Harvest amethyst &7($0.10)");
            case EXCAVATOR -> List.of("Dig dirt/sand/gravel &7($0.02-$0.04)", "Brush artifacts &7($2.50)");
            case LUMBERJACK -> List.of("Chop logs &7($0.08)", "Craft wood items &7($0.02-$0.10)");
            case SMITH -> List.of("Craft tools/armor &7($0.35-$2.50)", "Smith netherite &7($5.00)");
            case EXPLORER -> List.of("Walk/ride &7($0.003-$0.004/block)", "Open natural chests &7($3-$8)");
            case ALCHEMIST -> List.of("Brew potions &7($0.80-$1.00)", "Enchant items &7($0.50)");
            case HUNTER -> List.of("Kill mobs &7($0.15-$15.00)", "Catch fish &7($0.20-$1.50)");
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
