package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.event.player.GuiJobsStatsClick;
import com.aearost.aranarthcore.gui.GuiJobs;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.JobUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandJobs implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage("&cThis command can only be run by a player."));
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
                player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &fUsage: /jobs join <job>"));
                return true;
            }
            JobType job = parseJobType(args[1]);
            if (job == null) {
                player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &cUnknown job: &f" + args[1]));
                return true;
            }
            int maxJobs = JobUtils.getMaxJobs(ap.getRank());
            if (jobData.hasJob(job)) {
                player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &fYou are already a &6" + job.getDisplayName() + "&f."));
                return true;
            }
            if (jobData.getActiveJobs().size() >= maxJobs) {
                player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &fYou have reached your maximum of &6" + maxJobs + " &fjob" + (maxJobs == 1 ? "" : "s") + "."));
                return true;
            }
            jobData.addJob(job);
            AranarthUtils.setPlayer(player.getUniqueId(), ap);
            PersistenceUtils.saveJobData(player.getUniqueId());
            player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &fYou have joined the &6" + job.getDisplayName() + " &fjob!"));
            return true;
        }

        if (sub.equals("quit") || sub.equals("leave")) {
            if (args.length < 2) {
                player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &fUsage: /jobs quit <job>"));
                return true;
            }
            JobType job = parseJobType(args[1]);
            if (job == null) {
                player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &cUnknown job: &f" + args[1]));
                return true;
            }
            if (!jobData.hasJob(job)) {
                player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &fYou are not a &6" + job.getDisplayName() + "&f."));
                return true;
            }
            jobData.removeJob(job);
            AranarthUtils.setPlayer(player.getUniqueId(), ap);
            PersistenceUtils.saveJobData(player.getUniqueId());
            player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &fYou have left the &6" + job.getDisplayName() + " &fjob. Your progress has been saved."));
            return true;
        }

        if (sub.equals("stats")) {
            if (args.length < 2) {
                player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &fUsage: /jobs stats <job>"));
                return true;
            }
            JobType job = parseJobType(args[1]);
            if (job == null) {
                player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &cUnknown job: &f" + args[1]));
                return true;
            }
            if (!jobData.hasJob(job)) {
                player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &fYou are not a &6" + job.getDisplayName() + "&f."));
                return true;
            }
            GuiJobsStatsClick.sendStatsToChat(player, job, jobData);
            return true;
        }

        player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &fUsage: /jobs [join|quit|stats] [name]"));
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
