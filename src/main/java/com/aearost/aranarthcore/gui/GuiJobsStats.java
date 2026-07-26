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

public class GuiJobsStats {

    public static final String TITLE = "Job Statistics";

    private final Player player;
    private final Inventory gui;

    public GuiJobsStats(Player player) {
        this.player = player;
        this.gui = initializeGui();
    }

    public void openGui() {
        player.openInventory(gui);
    }

    private Inventory initializeGui() {
        Inventory inv = Bukkit.createInventory(player, 45, ChatUtils.translateToColor("&8&lJob Statistics"));

        ItemStack grayPane = makePane(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, grayPane);
        }

        for (int i = 36; i < 45; i++) {
            inv.setItem(i, grayPane);
        }

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        JobData jobData = ap.getJobData();

        // Show all 9 jobs, indicating which ones the player actively has
        for (int i = 0; i < GuiJobsJoin.JOB_ORDER.length; i++) {
            JobType job = GuiJobsJoin.JOB_ORDER[i];
            inv.setItem(GuiJobsJoin.JOB_SLOTS[i], makeStatItem(job, jobData));
        }

        // Back button
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatUtils.translateToColor("&7Back"));
        back.setItemMeta(backMeta);
        inv.setItem(40, back);

        return inv;
    }

    private ItemStack makeStatItem(JobType job, JobData jobData) {
        ItemStack item = new ItemStack(JobUtils.getJobIcon(job));
        ItemMeta meta = item.getItemMeta();

        boolean isActive = jobData.hasJob(job);
        int level = jobData.getLevel(job);
        double currentXp = jobData.getCurrentXp(job);
        long required = JobUtils.getXpRequired(level);
        String xpStr = level >= 10 ? "Max Level" : (int) currentXp + " &8/ &e" + required;

        if (isActive) {
            meta.setDisplayName(ChatUtils.translateToColor(JobUtils.getJobColor(job) + job.getDisplayName() + " &a(Active)"));
        } else {
            meta.setDisplayName(ChatUtils.translateToColor(JobUtils.getJobColor(job) + job.getDisplayName()));
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Level: &e" + level));
        lore.add(ChatUtils.translateToColor("&7XP: &e" + xpStr));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Click to view detailed stats in chat"));
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
