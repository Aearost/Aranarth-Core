package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.gui.GuiJobs;
import com.aearost.aranarthcore.gui.GuiJobsLeave;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class GuiJobsLeaveClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (e.getClickedInventory() == null) {
            return;
        }
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) {
            return;
        }

        int slot = e.getRawSlot();

        if (slot == 22) {
            player.closeInventory();
            new GuiJobs(player).openGui();
            return;
        }

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) {
            return;
        }
        JobData jobData = ap.getJobData();
        List<JobType> activeJobs = jobData.getActiveJobs();

        int[] slots = GuiJobsLeave.JOB_SLOTS;
        for (int i = 0; i < slots.length && i < activeJobs.size(); i++) {
            if (slots[i] == slot) {
                // Leaving is always allowed, and starts the cooldown before joining another job
                JobType job = activeJobs.get(i);
                jobData.removeJob(job);
                jobData.setLastLeftAt(System.currentTimeMillis());
                ap.setJobDataLoaded(true);
                AranarthUtils.setPlayer(player.getUniqueId(), ap);
                Bukkit.getLogger().info("[AC][Jobs] " + player.getName() + " left job " + job.name()
                        + " - active=" + jobData.getActiveJobs());
                PersistenceUtils.saveJobDataSync(player.getUniqueId());
                if (NetworkManager.isActive()) {
                    NetworkManager.getInstance().publishJobUpdate(player.getUniqueId());
                }

                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "jobs.left", "job", Lang.key(job.getNameKey()))));
                int jobsVol = AranarthUtils.getPlayer(player.getUniqueId()).getJobsSoundVolume();
                if (jobsVol > 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, jobsVol / 100f, 1.0f);
                }

                new GuiJobsLeave(player).populateInto(player.getOpenInventory().getTopInventory());
                return;
            }
        }
    }
}
