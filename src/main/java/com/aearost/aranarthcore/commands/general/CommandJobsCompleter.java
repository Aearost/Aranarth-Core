package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandJobsCompleter implements TabCompleter {

    private static final List<String> PLAYER_SUBS = List.of("join", "quit", "stats", "top", "who");
    private static final List<String> ADMIN_SUBS = List.of("xp", "remove", "reset", "add");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) {
            return completions;
        }

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) {
            return completions;
        }

        boolean isAdmin = ap.getCouncilRank() == 3;

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(PLAYER_SUBS);
            if (isAdmin) {
                subs.addAll(ADMIN_SUBS);
            }
            return filter(subs, args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            if (sub.equals("join")) {
                JobData jobData = ap.getJobData();
                for (JobType job : JobType.values()) {
                    if (!jobData.hasJob(job)) {
                        completions.add(job.getDisplayName().toLowerCase());
                    }
                }
                return filter(completions, args[1]);
            }
            if (sub.equals("quit")) {
                for (JobType job : ap.getJobData().getActiveJobs()) {
                    completions.add(job.getDisplayName().toLowerCase());
                }
                return filter(completions, args[1]);
            }
            if (sub.equals("stats") || sub.equals("top")) {
                for (JobType job : JobType.values()) {
                    completions.add(job.getDisplayName().toLowerCase());
                }
                return filter(completions, args[1]);
            }
            if (sub.equals("who")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                return filter(completions, args[1]);
            }
            if (isAdmin && (sub.equals("xp") || sub.equals("remove") || sub.equals("reset") || sub.equals("add"))) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                return filter(completions, args[1]);
            }
        }

        if (args.length == 3 && isAdmin) {
            if (sub.equals("xp") || sub.equals("reset")) {
                for (JobType job : JobType.values()) {
                    completions.add(job.getDisplayName().toLowerCase());
                }
                return filter(completions, args[2]);
            }
            if (sub.equals("remove")) {
                // Complete target's active jobs if available
                AranarthPlayer target = getTargetAp(args[1]);
                List<JobType> jobs = target != null ? target.getJobData().getActiveJobs() : List.of(JobType.values());
                for (JobType job : jobs) {
                    completions.add(job.getDisplayName().toLowerCase());
                }
                return filter(completions, args[2]);
            }
            if (sub.equals("add")) {
                // Complete target's inactive jobs if available
                AranarthPlayer target = getTargetAp(args[1]);
                for (JobType job : JobType.values()) {
                    if (target == null || !target.getJobData().hasJob(job)) {
                        completions.add(job.getDisplayName().toLowerCase());
                    }
                }
                return filter(completions, args[2]);
            }
        }

        return completions;
    }

    private AranarthPlayer getTargetAp(String username) {
        java.util.UUID uuid = AranarthUtils.getUUIDFromUsername(username);
        return uuid != null ? AranarthUtils.getPlayer(uuid) : null;
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String s : options) {
            if (s.toLowerCase().startsWith(lower)) {
                result.add(s);
            }
        }
        return result;
    }
}
