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
        Inventory inv = Bukkit.createInventory(player, 27, ChatUtils.translateToColor("&8&lJobs"));

        ItemStack grayPane = makePane(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, grayPane);
        }

        for (int i = 18; i < 27; i++) {
            inv.setItem(i, grayPane);
        }

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        JobData jobData = ap.getJobData();
        int maxJobs = JobUtils.getMaxJobs(ap.getRank());
        int activeCount = jobData.getActiveJobs().size();

        inv.setItem(11, makeJoinButton(jobData, maxJobs, activeCount));
        inv.setItem(13, makeStatsButton(jobData));
        inv.setItem(15, makeLeaveButton(jobData));

        return inv;
    }

    private ItemStack makeJoinButton(JobData jobData, int maxJobs, int activeCount) {
        ItemStack item = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&a&lJoin a Job"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Active Jobs: &e" + activeCount + " &8/ &e" + maxJobs));
        if (!jobData.getActiveJobs().isEmpty()) {
            lore.add("");
            for (JobType job : jobData.getActiveJobs()) {
                lore.add(ChatUtils.translateToColor("&e" + job.getDisplayName() + " &7- Level &e" + jobData.getLevel(job)));
            }
        }
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Click to browse available jobs"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeStatsButton(JobData jobData) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&e&lStatistics"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7View XP and level stats for your jobs"));
        if (!jobData.getActiveJobs().isEmpty()) {
            lore.add("");
            for (JobType job : jobData.getActiveJobs()) {
                int level = jobData.getLevel(job);
                double currentXp = jobData.getCurrentXp(job);
                long required = JobUtils.getXpRequired(level);
                String xpStr = level >= 10 ? "Max Level" : (int) currentXp + " / " + required;
                lore.add(ChatUtils.translateToColor("&e" + job.getDisplayName() + " &7Lvl &e" + level + " &8- &7" + xpStr));
            }
        }
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Click to view detailed statistics"));
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
            lore.add(ChatUtils.translateToColor("&7You do not have any job to leave"));
        } else {
            lore.add(ChatUtils.translateToColor("&7Your current jobs:"));
            for (JobType job : jobData.getActiveJobs()) {
                lore.add(ChatUtils.translateToColor("&e  " + job.getDisplayName() + " &7- Level &e" + jobData.getLevel(job)));
            }
            lore.add("");
            lore.add(ChatUtils.translateToColor("&7Click to leave a job"));
        }
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
