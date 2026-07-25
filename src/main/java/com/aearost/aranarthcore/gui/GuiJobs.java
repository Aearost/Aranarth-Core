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

public class GuiJobs {

    public static final String TITLE = "Jobs";

    private final Player player;
    private final Inventory gui;

    public GuiJobs(Player player) {
        this.player = player;
        this.gui = initializeGui();
    }

    public void openGui() {
        player.openInventory(gui);
    }

    private Inventory initializeGui() {
        Inventory inv = Bukkit.createInventory(player, 45, ChatUtils.translateToColor("&8&lJobs"));

        ItemStack yellowPane = makePane(Material.YELLOW_STAINED_GLASS_PANE);
        ItemStack blackPane = makePane(Material.BLACK_STAINED_GLASS_PANE);

        // Border: Row 1 (0-8)
        inv.setItem(0, yellowPane);
        inv.setItem(1, yellowPane);
        inv.setItem(2, blackPane);
        inv.setItem(3, blackPane);
        inv.setItem(4, blackPane);
        inv.setItem(5, blackPane);
        inv.setItem(6, blackPane);
        inv.setItem(7, yellowPane);
        inv.setItem(8, yellowPane);
        // Row 2 (9-17)
        inv.setItem(9, blackPane);
        inv.setItem(10, yellowPane);
        inv.setItem(11, blackPane);
        inv.setItem(12, blackPane);
        inv.setItem(13, blackPane);
        inv.setItem(14, blackPane);
        inv.setItem(15, blackPane);
        inv.setItem(16, yellowPane);
        inv.setItem(17, blackPane);
        // Row 3 - buttons at 20, 22, 24
        inv.setItem(18, blackPane);
        inv.setItem(19, blackPane);
        // 20: Join
        inv.setItem(21, blackPane);
        // 22: Stats
        inv.setItem(23, blackPane);
        // 24: Leave
        inv.setItem(25, blackPane);
        inv.setItem(26, blackPane);
        // Row 4
        inv.setItem(27, blackPane);
        inv.setItem(28, yellowPane);
        inv.setItem(29, blackPane);
        inv.setItem(30, blackPane);
        inv.setItem(31, blackPane);
        inv.setItem(32, blackPane);
        inv.setItem(33, blackPane);
        inv.setItem(34, yellowPane);
        inv.setItem(35, blackPane);
        // Row 5
        inv.setItem(36, yellowPane);
        inv.setItem(37, yellowPane);
        inv.setItem(38, blackPane);
        inv.setItem(39, blackPane);
        inv.setItem(40, blackPane);
        inv.setItem(41, blackPane);
        inv.setItem(42, blackPane);
        inv.setItem(43, yellowPane);
        inv.setItem(44, yellowPane);

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        JobData jobData = ap.getJobData();
        int maxJobs = JobUtils.getMaxJobs(ap.getRank());
        int activeCount = jobData.getActiveJobs().size();

        // Join button
        inv.setItem(20, makeJoinButton(jobData, maxJobs, activeCount));
        // Stats button
        inv.setItem(22, makeStatsButton(jobData));
        // Leave button
        inv.setItem(24, makeLeaveButton(jobData));

        return inv;
    }

    private ItemStack makeJoinButton(JobData jobData, int maxJobs, int activeCount) {
        ItemStack item = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&a&lJoin a Job"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Active Jobs: &f" + activeCount + " &8/ &f" + maxJobs));
        lore.add("");
        if (jobData.getActiveJobs().isEmpty()) {
            lore.add(ChatUtils.translateToColor("&7You have no active jobs."));
        } else {
            for (JobType job : jobData.getActiveJobs()) {
                lore.add(ChatUtils.translateToColor("&6" + job.getDisplayName() + " &7- Level &f" + jobData.getLevel(job)));
            }
        }
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Click to browse available jobs."));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeStatsButton(JobData jobData) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&e&lStatistics"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7View XP and level stats for your jobs."));
        lore.add("");
        if (jobData.getActiveJobs().isEmpty()) {
            lore.add(ChatUtils.translateToColor("&7You have no active jobs."));
        } else {
            for (JobType job : jobData.getActiveJobs()) {
                int level = jobData.getLevel(job);
                double currentXp = jobData.getCurrentXp(job);
                long required = JobUtils.getXpRequired(level);
                String xpStr = level >= 10 ? "Max Level" : (int) currentXp + " / " + required;
                lore.add(ChatUtils.translateToColor("&6" + job.getDisplayName() + " &7Lvl &f" + level + " &8- &7" + xpStr));
            }
        }
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Click to view detailed statistics."));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeLeaveButton(JobData jobData) {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&c&lLeave a Job"));
        List<String> lore = new ArrayList<>();
        if (jobData.getActiveJobs().isEmpty()) {
            lore.add(ChatUtils.translateToColor("&7You have no active jobs to leave."));
        } else {
            lore.add(ChatUtils.translateToColor("&7Your current jobs:"));
            for (JobType job : jobData.getActiveJobs()) {
                lore.add(ChatUtils.translateToColor("&6  " + job.getDisplayName() + " &7- Level &f" + jobData.getLevel(job)));
            }
        }
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Click to leave a job."));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makePane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(" "));
        pane.setItemMeta(meta);
        return pane;
    }
}
