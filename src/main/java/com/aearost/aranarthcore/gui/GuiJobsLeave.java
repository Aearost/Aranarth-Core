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

public class GuiJobsLeave {

    public static final String TITLE = "Leave a Job";

    // Jobs centered in the middle row: slots 11-15
    public static final int[] JOB_SLOTS = {11, 12, 13, 14, 15};

    private final Player player;
    private final Inventory gui;

    public GuiJobsLeave(Player player) {
        this.player = player;
        this.gui = initializeGui();
    }

    public void openGui() {
        player.openInventory(gui);
    }

    private Inventory initializeGui() {
        Inventory inv = Bukkit.createInventory(player, 27, ChatUtils.translateToColor("&8&lLeave a Job"));

        ItemStack grayPane = makePane(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, grayPane);
        }

        for (int i = 18; i < 27; i++) {
            inv.setItem(i, grayPane);
        }

        // Back button
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatUtils.translateToColor("&7Back"));
        back.setItemMeta(backMeta);
        inv.setItem(22, back);

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        JobData jobData = ap.getJobData();
        List<JobType> activeJobs = jobData.getActiveJobs();

        for (int i = 0; i < activeJobs.size() && i < JOB_SLOTS.length; i++) {
            inv.setItem(JOB_SLOTS[i], makeLeaveItem(activeJobs.get(i), jobData));
        }

        return inv;
    }

    private ItemStack makeLeaveItem(JobType job, JobData jobData) {
        ItemStack item = new ItemStack(JobUtils.getJobIcon(job));
        ItemMeta meta = item.getItemMeta();

        int level = jobData.getLevel(job);
        double currentXp = jobData.getCurrentXp(job);
        long required = JobUtils.getXpRequired(level);
        String xpStr = level >= 10 ? "Max Level" : (int) currentXp + " / " + required;

        meta.setDisplayName(ChatUtils.translateToColor("&c&l" + job.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Level: &e" + level));
        lore.add(ChatUtils.translateToColor("&7XP: &e" + xpStr));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&cClick to leave this job"));
        lore.add(ChatUtils.translateToColor("&7Your level and XP will be saved"));
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
