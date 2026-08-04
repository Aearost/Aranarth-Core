package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.gui.GuiJobs;
import com.aearost.aranarthcore.gui.GuiJobsLeave;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiJobsLeaveClick {

    private static final long COOLDOWN_MS = 3_600_000L; // 1 hour
    private static final Map<UUID, Long> leaveCooldowns = new HashMap<>();

    public static boolean isOnCooldown(UUID uuid) {
        Long last = leaveCooldowns.get(uuid);
        if (last == null) {
            return false;
        }
        return (System.currentTimeMillis() - last) < COOLDOWN_MS;
    }

    public static String getCooldownRemaining(UUID uuid) {
        Long last = leaveCooldowns.get(uuid);
        if (last == null) {
            return "0m";
        }
        long remaining = COOLDOWN_MS - (System.currentTimeMillis() - last);
        if (remaining <= 0) {
            return "0m";
        }
        long hours = remaining / 3_600_000L;
        long minutes = (remaining % 3_600_000L) / 60_000L;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    public static void applyCooldown(UUID uuid) {
        leaveCooldowns.put(uuid, System.currentTimeMillis());
    }

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
                if (isOnCooldown(player.getUniqueId())) {
                    player.sendMessage(ChatUtils.chatMessage("&7You must wait &e" + getCooldownRemaining(player.getUniqueId()) + " &7before leaving another job"));
                    return;
                }

                JobType job = activeJobs.get(i);
                jobData.removeJob(job);
                ap.setJobDataLoaded(true);
                AranarthUtils.setPlayer(player.getUniqueId(), ap);
                Bukkit.getLogger().info("[AC][Jobs] " + player.getName() + " left job " + job.name()
                        + " — active=" + jobData.getActiveJobs());
                PersistenceUtils.saveJobDataSync(player.getUniqueId());
                if (NetworkManager.isActive()) {
                    NetworkManager.getInstance().publishJobUpdate(player.getUniqueId());
                }
                applyCooldown(player.getUniqueId());

                player.sendMessage(ChatUtils.chatMessage("&7You have left the &e" + job.getDisplayName() + " &7job &8— &7your progress has been saved"));
                int jobsVol = AranarthUtils.getPlayer(player.getUniqueId()).getJobsSoundVolume();
                if (jobsVol > 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, jobsVol / 100f, 1.0f);
                }

                player.closeInventory();
                new GuiJobsLeave(player).openGui();
                return;
            }
        }
    }
}
