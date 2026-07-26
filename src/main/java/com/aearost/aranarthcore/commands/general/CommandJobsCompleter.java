package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandJobsCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) return completions;

        if (args.length == 1) {
            List<String> subs = List.of("join", "quit", "stats");
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
            if (ap == null) return completions;
            JobData jobData = ap.getJobData();

            if (sub.equals("join")) {
                for (JobType job : JobType.values()) {
                    if (!jobData.hasJob(job) && job.getDisplayName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(job.getDisplayName().toLowerCase());
                    }
                }
            } else if (sub.equals("quit")) {
                for (JobType job : jobData.getActiveJobs()) {
                    if (job.getDisplayName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(job.getDisplayName().toLowerCase());
                    }
                }
            } else if (sub.equals("stats")) {
                for (JobType job : JobType.values()) {
                    if (job.getDisplayName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(job.getDisplayName().toLowerCase());
                    }
                }
            }
        }

        return completions;
    }
}
