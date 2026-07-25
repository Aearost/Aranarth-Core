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
        Inventory inv = Bukkit.createInventory(player, 54, ChatUtils.translateToColor("&8&lLeave a Job"));

        ItemStack yellowPane = makePane(Material.YELLOW_STAINED_GLASS_PANE);
        ItemStack blackPane = makePane(Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, blackPane);
        }
        inv.setItem(0, yellowPane);
        inv.setItem(8, yellowPane);
        inv.setItem(45, yellowPane);
        inv.setItem(53, yellowPane);

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        JobData jobData = ap.getJobData();
        List<JobType> activeJobs = jobData.getActiveJobs();

        int[] slots = {10, 12, 14, 19, 21, 23, 28, 30, 32};
        for (int i = 0; i < activeJobs.size() && i < slots.length; i++) {
            inv.setItem(slots[i], makeLeaveItem(activeJobs.get(i), jobData));
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatUtils.translateToColor("&7Back"));
        back.setItemMeta(backMeta);
        inv.setItem(49, back);

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
        lore.add(ChatUtils.translateToColor("&7Level: &f" + level));
        lore.add(ChatUtils.translateToColor("&7XP: &f" + xpStr));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&cClick to leave this job."));
        lore.add(ChatUtils.translateToColor("&7Your level and XP will be saved."));
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
