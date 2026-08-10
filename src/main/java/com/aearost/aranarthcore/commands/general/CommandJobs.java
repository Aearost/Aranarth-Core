package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.event.player.GuiJobsLeaveClick;
import com.aearost.aranarthcore.event.player.GuiJobsStatsClick;
import com.aearost.aranarthcore.gui.GuiJobs;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.JobUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandJobs implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
            return true;
        }

        if (args.length == 0) {
            new GuiJobs(player).openGui();
            return true;
        }

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) return true;
        JobData jobData = ap.getJobData();

        String sub = args[0].toLowerCase();

        if (sub.equals("join")) {
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/jobs join <job>"));
                return true;
            }
            JobType job = parseJobType(args[1]);
            if (job == null) {
                player.sendMessage(ChatUtils.chatMessage("&cThe job &e" + args[1] + " &ccould not be found"));
                return true;
            }
            int maxJobs = JobUtils.getMaxJobs(ap.getRank());
            if (jobData.hasJob(job)) {
                player.sendMessage(ChatUtils.chatMessage("&cYou are already a &e" + job.getDisplayName()));
                return true;
            }
            if (jobData.getActiveJobs().size() >= maxJobs) {
                player.sendMessage(ChatUtils.chatMessage("&cYou are already at your maximum of &e" + maxJobs + " &7job" + (maxJobs == 1 ? "" : "s")));
                return true;
            }
            jobData.addJob(job);
            ap.setJobDataLoaded(true);
            AranarthUtils.setPlayer(player.getUniqueId(), ap);
            Bukkit.getLogger().info("[AC][Jobs] " + player.getName() + " joined job (cmd) " + job.name()
                    + " - active=" + jobData.getActiveJobs());
            PersistenceUtils.saveJobData(player.getUniqueId());
            String aOrAn = job == JobType.EXCAVATOR || job == JobType.EXPLORER || job == JobType.ALCHEMIST ? "an" : "a";
            player.sendMessage(ChatUtils.chatMessage("&7You have become " + aOrAn + " &e" + job.getDisplayName() + " &7job!"));
            return true;
        }

        if (sub.equals("quit") || sub.equals("leave")) {
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/jobs quit <job>"));
                return true;
            }
            JobType job = parseJobType(args[1]);
            if (job == null) {
                player.sendMessage(ChatUtils.chatMessage("&cThe job &e" + args[1] + " &ccould not be found"));
                return true;
            }
            if (!jobData.hasJob(job)) {
                player.sendMessage(ChatUtils.chatMessage("&cYou are not a &e" + job.getDisplayName()));
                return true;
            }
            if (GuiJobsLeaveClick.isOnCooldown(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage("&cYou must wait &e" + GuiJobsLeaveClick.getCooldownRemaining(player.getUniqueId()) + " &cbefore leaving another job"));
                return true;
            }
            jobData.removeJob(job);
            ap.setJobDataLoaded(true);
            AranarthUtils.setPlayer(player.getUniqueId(), ap);
            Bukkit.getLogger().info("[AC][Jobs] " + player.getName() + " left job (cmd) " + job.name()
                    + " - active=" + jobData.getActiveJobs());
            PersistenceUtils.saveJobData(player.getUniqueId());
            GuiJobsLeaveClick.applyCooldown(player.getUniqueId());
            String aOrAn = job == JobType.EXCAVATOR || job == JobType.EXPLORER || job == JobType.ALCHEMIST ? "an" : "a";
            player.sendMessage(ChatUtils.chatMessage("&7You are no longer " + aOrAn + " &e" + job.getDisplayName()));
            return true;
        }

        if (sub.equals("stats")) {
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/jobs stats <job>"));
                return true;
            }
            JobType job = parseJobType(args[1]);
            if (job == null) {
                player.sendMessage(ChatUtils.chatMessage("&cThe job &e" + args[1] + " &ccould not be found"));
                return true;
            }
            GuiJobsStatsClick.sendStatsToChat(player, job, jobData);
            return true;
        }

        player.sendMessage(ChatUtils.chatMessage("&&cInvalid syntax: &e/jobs [join|quit|stats] [name]"));
        return true;
    }

    private JobType parseJobType(String name) {
        for (JobType job : JobType.values()) {
            if (job.name().equalsIgnoreCase(name) || job.getDisplayName().equalsIgnoreCase(name)) {
                return job;
            }
        }
        return null;
    }
}
