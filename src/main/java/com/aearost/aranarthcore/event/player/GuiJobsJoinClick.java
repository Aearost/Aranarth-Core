package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.gui.GuiJobs;
import com.aearost.aranarthcore.gui.GuiJobsJoin;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.JobUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiJobsJoinClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        int slot = e.getRawSlot();

        // Back button
        if (slot == 40) {
            player.closeInventory();
            new GuiJobs(player).openGui();
            return;
        }

        // Find which job was clicked
        JobType job = getJobForSlot(slot);
        if (job == null) return;

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) return;
        JobData jobData = ap.getJobData();
        int maxJobs = JobUtils.getMaxJobs(ap.getRank());

        if (jobData.hasJob(job)) {
            player.sendMessage(ChatUtils.chatMessage("&7You are already a &e" + job.getDisplayName()));
            return;
        }

        if (jobData.getActiveJobs().size() >= maxJobs) {
            player.sendMessage(ChatUtils.chatMessage("&7You have reached your maximum of &e" + maxJobs + " &7job" + (maxJobs == 1 ? "" : "s")));
            return;
        }

        jobData.addJob(job);
        AranarthUtils.setPlayer(player.getUniqueId(), ap);
        PersistenceUtils.saveJobData(player.getUniqueId());

        player.sendMessage(ChatUtils.chatMessage("&7You have joined the &e" + job.getDisplayName() + " &7job!"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        player.closeInventory();
        new GuiJobsJoin(player).openGui();
    }

    private JobType getJobForSlot(int slot) {
        int[] slots = GuiJobsJoin.JOB_SLOTS;
        JobType[] jobs = GuiJobsJoin.JOB_ORDER;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) return jobs[i];
        }
        return null;
    }
}
