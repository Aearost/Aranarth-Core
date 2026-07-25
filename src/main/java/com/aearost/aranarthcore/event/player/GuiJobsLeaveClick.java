package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.gui.GuiJobs;
import com.aearost.aranarthcore.gui.GuiJobsLeave;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class GuiJobsLeaveClick {

    private static final int[] JOB_SLOTS = {10, 12, 14, 19, 21, 23, 28, 30, 32};

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        int slot = e.getRawSlot();

        if (slot == 49) {
            player.closeInventory();
            new GuiJobs(player).openGui();
            return;
        }

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) return;
        JobData jobData = ap.getJobData();
        List<JobType> activeJobs = jobData.getActiveJobs();

        for (int i = 0; i < JOB_SLOTS.length && i < activeJobs.size(); i++) {
            if (JOB_SLOTS[i] == slot) {
                JobType job = activeJobs.get(i);
                jobData.removeJob(job);
                AranarthUtils.setPlayer(player.getUniqueId(), ap);
                PersistenceUtils.saveJobData(player.getUniqueId());

                player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &fYou have left the &6" + job.getDisplayName() + " &fjob. Your progress has been saved."));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);

                player.closeInventory();
                new GuiJobsLeave(player).openGui();
                return;
            }
        }
    }
}
