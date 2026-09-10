package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.database.DatabaseManager;
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        if (ap == null) {
            return true;
        }
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

        if (sub.equals("top")) {
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/jobs top <job>"));
                return true;
            }
            JobType job = parseJobType(args[1]);
            if (job == null) {
                player.sendMessage(ChatUtils.chatMessage("&cThe job &e" + args[1] + " &ccould not be found"));
                return true;
            }
            handleTop(player, job);
            return true;
        }

        if (sub.equals("who")) {
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/jobs who <player>"));
                return true;
            }
            handleWho(player, args[1]);
            return true;
        }

        // Admin commands - council rank 3 only
        if (sub.equals("xp") || sub.equals("remove") || sub.equals("reset") || sub.equals("add")) {
            if (ap.getCouncilRank() != 3) {
                player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/jobs [join|quit|stats|top|who] [args]"));
                return true;
            }
            return handleAdminCommand(player, sub, args);
        }

        player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/jobs [join|quit|stats|top|who] [args]"));
        return true;
    }

    private void handleTop(Player player, JobType job) {
        List<DatabaseManager.JobTopEntry> entries = DatabaseManager.getInstance().loadAllJobEntries();

        record RankedEntry(String name, double totalXp) {
        }
        List<RankedEntry> ranked = new ArrayList<>();

        for (DatabaseManager.JobTopEntry entry : entries) {
            UUID uuid = entry.uuid();
            JobData jd;
            AranarthPlayer onlineAp = AranarthUtils.getAranarthPlayers().get(uuid);
            if (onlineAp != null) {
                jd = onlineAp.getJobData();
            } else {
                jd = PersistenceUtils.deserializeJobData(entry.jobDataJson());
            }
            double xp = jd.getTotalXp(job);
            if (xp <= 0) {
                continue;
            }

            String displayName = onlineAp != null
                    ? onlineAp.getUsername()
                    : (entry.username().isEmpty() ? "Unknown" : entry.username());
            ranked.add(new RankedEntry(displayName, xp));
        }

        ranked.sort((a, b) -> Double.compare(b.totalXp(), a.totalXp()));

        player.sendMessage(ChatUtils.chatMessage("&8      - - - &e&l" + job.getDisplayName() + " Leaderboard &8- - -"));
        if (ranked.isEmpty()) {
            player.sendMessage(ChatUtils.chatMessage("&7No players have worked this job yet."));
            return;
        }
        NumberFormat nf = NumberFormat.getInstance();
        int limit = Math.min(10, ranked.size());
        for (int i = 0; i < limit; i++) {
            RankedEntry e = ranked.get(i);
            JobData temp = new JobData();
            temp.setTotalXp(job, e.totalXp());
            int level = temp.getLevel(job);
            double currentXp = temp.getCurrentXp(job);
            long required = JobUtils.getXpRequired(level);
            String xpStr = level >= 10 ? "Max Level"
                    : nf.format((long) currentXp) + " / " + nf.format(required) + " XP";
            player.sendMessage(ChatUtils.chatMessage("&8[&6" + (i + 1) + "&8] &e" + e.name()
                    + " &8- &7Level &e" + level + " &8(&7" + xpStr + "&8)"));
        }
    }

    private void handleWho(Player sender, String username) {
        UUID uuid = AranarthUtils.getUUIDFromUsername(username);
        JobData jobData = null;
        String displayName = username;

        if (uuid != null) {
            AranarthPlayer target = AranarthUtils.getPlayer(uuid);
            if (target != null) {
                jobData = target.getJobData();
                displayName = target.getUsername();
            }
        }

        // Fallback to DB for players not in memory this session
        if (jobData == null && DatabaseManager.isActive()) {
            String json = DatabaseManager.getInstance().loadJobDataByUsername(username);
            if (json != null) {
                jobData = PersistenceUtils.deserializeJobData(json);
            }
        }

        if (jobData == null) {
            sender.sendMessage(ChatUtils.chatMessage("&cThis player does not exist!"));
            return;
        }

        sender.sendMessage(ChatUtils.chatMessage("&8      - - - &e" + displayName + "&e's Jobs &8- - -"));

        List<JobType> activeJobs = jobData.getActiveJobs();
        if (!activeJobs.isEmpty()) {
            sender.sendMessage(ChatUtils.chatMessage("&eActive:"));
            for (JobType job : activeJobs) {
                sender.sendMessage(ChatUtils.chatMessage("  " + formatJobProgress(job, jobData, true)));
            }
        } else {
            sender.sendMessage(ChatUtils.chatMessage("&7(No active jobs)"));
        }

        List<JobType> inactiveJobs = new ArrayList<>();
        for (JobType job : JobType.values()) {
            if (!activeJobs.contains(job)) {
                inactiveJobs.add(job);
            }
        }
        if (!inactiveJobs.isEmpty()) {
            sender.sendMessage(ChatUtils.chatMessage("&7Inactive:"));
            for (JobType job : inactiveJobs) {
                sender.sendMessage(ChatUtils.chatMessage("  " + formatJobProgress(job, jobData, false)));
            }
        }
    }

    private String formatJobProgress(JobType job, JobData jobData, boolean active) {
        int level = jobData.getLevel(job);
        double currentXp = jobData.getCurrentXp(job);
        long required = JobUtils.getXpRequired(level);
        NumberFormat nf = NumberFormat.getInstance();
        String xpStr = level >= 10 ? "Max Level"
                : nf.format((long) currentXp) + " / " + nf.format(required) + " XP";
        String nameColor = active ? "&e" : "&7";
        return nameColor + job.getDisplayName() + " &8- &7Level &e" + level + " &8(&7" + xpStr + "&8)";
    }

    private boolean handleAdminCommand(Player sender, String sub, String[] args) {
        if (sub.equals("xp")) {
            if (args.length < 4) {
                sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/jobs xp <player> <job> <amount>"));
                return true;
            }
            TargetResult tr = findTarget(sender, args[1]);
            if (tr == null) {
                return true;
            }
            JobType job = parseJobType(args[2]);
            if (job == null) {
                sender.sendMessage(ChatUtils.chatMessage("&cThe job &e" + args[2] + " &ccould not be found"));
                return true;
            }
            try {
                String amountArg = args[3];
                double currentXp = tr.ap().getJobData().getTotalXp(job);
                double newXp;
                NumberFormat nf = NumberFormat.getInstance();
                if (amountArg.startsWith("+")) {
                    double delta = Double.parseDouble(amountArg.substring(1));
                    newXp = currentXp + delta;
                    sender.sendMessage(ChatUtils.chatMessage("&e" + tr.ap().getUsername() + "&e's &7" + job.getDisplayName() + " XP increased by &e" + nf.format((long) delta)));
                } else if (amountArg.startsWith("-")) {
                    double delta = Double.parseDouble(amountArg.substring(1));
                    newXp = Math.max(0, currentXp - delta);
                    sender.sendMessage(ChatUtils.chatMessage("&e" + tr.ap().getUsername() + "&e's &7" + job.getDisplayName() + " XP decreased by &e" + nf.format((long) delta)));
                } else {
                    newXp = Double.parseDouble(amountArg);
                    sender.sendMessage(ChatUtils.chatMessage("&e" + tr.ap().getUsername() + "&e's &7" + job.getDisplayName() + " XP set to &e" + nf.format((long) newXp)));
                }
                newXp = Math.max(0, newXp);
                tr.ap().getJobData().setTotalXp(job, newXp);
                tr.ap().setJobDataLoaded(true);
                AranarthUtils.setPlayer(tr.uuid(), tr.ap());
                PersistenceUtils.saveJobData(tr.uuid());
                Bukkit.getLogger().info("[AC][Jobs] Admin " + sender.getName() + " set " + tr.ap().getUsername() + "'s " + job.name() + " XP to " + newXp);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatUtils.chatMessage("&cThat value is invalid!"));
            }
            return true;
        }

        if (sub.equals("remove")) {
            if (args.length < 3) {
                sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/jobs remove <player> <job>"));
                return true;
            }
            TargetResult tr = findTarget(sender, args[1]);
            if (tr == null) {
                return true;
            }
            JobType job = parseJobType(args[2]);
            if (job == null) {
                sender.sendMessage(ChatUtils.chatMessage("&cThe job &e" + args[2] + " &ccould not be found"));
                return true;
            }
            if (!tr.ap().getJobData().hasJob(job)) {
                sender.sendMessage(ChatUtils.chatMessage("&e" + tr.ap().getUsername() + " &7is not a &e" + job.getDisplayName()));
                return true;
            }
            tr.ap().getJobData().removeJob(job);
            tr.ap().setJobDataLoaded(true);
            AranarthUtils.setPlayer(tr.uuid(), tr.ap());
            PersistenceUtils.saveJobData(tr.uuid());
            sender.sendMessage(ChatUtils.chatMessage("&e" + tr.ap().getUsername() + " &7has been removed from &e" + job.getDisplayName()));
            Bukkit.getLogger().info("[AC][Jobs] Admin " + sender.getName() + " removed " + tr.ap().getUsername() + " from " + job.name());
            return true;
        }

        if (sub.equals("reset")) {
            if (args.length < 3) {
                sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/jobs reset <player> <job>"));
                return true;
            }
            TargetResult tr = findTarget(sender, args[1]);
            if (tr == null) {
                return true;
            }
            JobType job = parseJobType(args[2]);
            if (job == null) {
                sender.sendMessage(ChatUtils.chatMessage("&cThe job &e" + args[2] + " &ccould not be found"));
                return true;
            }
            tr.ap().getJobData().setTotalXp(job, 0.0);
            tr.ap().getJobData().removeJob(job);
            tr.ap().setJobDataLoaded(true);
            AranarthUtils.setPlayer(tr.uuid(), tr.ap());
            PersistenceUtils.saveJobData(tr.uuid());
            sender.sendMessage(ChatUtils.chatMessage("&e" + tr.ap().getUsername() + "&e's &7" + job.getDisplayName() + " progress has been reset"));
            Bukkit.getLogger().info("[AC][Jobs] Admin " + sender.getName() + " reset " + tr.ap().getUsername() + "'s " + job.name());
            return true;
        }

        if (sub.equals("add")) {
            if (args.length < 3) {
                sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/jobs add <player> <job>"));
                return true;
            }
            TargetResult tr = findTarget(sender, args[1]);
            if (tr == null) {
                return true;
            }
            JobType job = parseJobType(args[2]);
            if (job == null) {
                sender.sendMessage(ChatUtils.chatMessage("&cThe job &e" + args[2] + " &ccould not be found"));
                return true;
            }
            if (tr.ap().getJobData().hasJob(job)) {
                sender.sendMessage(ChatUtils.chatMessage("&e" + tr.ap().getUsername() + " &7is already a &e" + job.getDisplayName()));
                return true;
            }
            int maxJobs = JobUtils.getMaxJobs(tr.ap().getRank());
            if (tr.ap().getJobData().getActiveJobs().size() >= maxJobs) {
                sender.sendMessage(ChatUtils.chatMessage("&e" + tr.ap().getUsername() + " &7is already at their maximum of &e" + maxJobs + " &7job" + (maxJobs == 1 ? "" : "s")));
                return true;
            }
            tr.ap().getJobData().addJob(job);
            tr.ap().setJobDataLoaded(true);
            AranarthUtils.setPlayer(tr.uuid(), tr.ap());
            PersistenceUtils.saveJobData(tr.uuid());
            sender.sendMessage(ChatUtils.chatMessage("&e" + tr.ap().getUsername() + " &7has been added to &e" + job.getDisplayName()));
            Bukkit.getLogger().info("[AC][Jobs] Admin " + sender.getName() + " added " + tr.ap().getUsername() + " to " + job.name());
            return true;
        }

        return true;
    }

    private record TargetResult(UUID uuid, AranarthPlayer ap) {
    }

    private TargetResult findTarget(Player sender, String username) {
        UUID uuid = AranarthUtils.getUUIDFromUsername(username);
        if (uuid == null) {
            sender.sendMessage(ChatUtils.chatMessage("&cThis player does not exist!"));
            return null;
        }
        AranarthPlayer target = AranarthUtils.getPlayer(uuid);
        if (target == null) {
            sender.sendMessage(ChatUtils.chatMessage("&cThis player does not exist!"));
            return null;
        }
        return new TargetResult(uuid, target);
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
