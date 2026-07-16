package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * /ac migrate — one-time command to push all in-memory data into MySQL.
 * Run this once on the Survival server after first deploying the network setup.
 */
public class CommandMigrate {

    public static boolean onCommand(CommandSender sender, String[] args) {
        if (!DatabaseManager.isActive()) {
            sender.sendMessage(ChatUtils.chatMessage("&cMySQL is not active. Check your config and logs."));
            return true;
        }

        sender.sendMessage(ChatUtils.chatMessage("&7Starting data migration to MySQL... this may take a moment."));

        Bukkit.getScheduler().runTaskAsynchronously(
                AranarthCore.getInstance(), () -> {
            try {
                PersistenceUtils.syncAranarthPlayersToDatabase();
                PersistenceUtils.syncKillDeathToDatabase();
                PersistenceUtils.syncVotesToDatabase();
                PersistenceUtils.syncQuestDataToDatabase();
                PersistenceUtils.syncLoginStreaksToDatabase();
                PersistenceUtils.syncMailToDatabase();
                PersistenceUtils.syncMountsToDatabase();
                PersistenceUtils.syncPunishmentsToDatabase();
                PersistenceUtils.syncBoostsToDatabase();

                Bukkit.getScheduler().runTask(
                        AranarthCore.getInstance(),
                        () -> sender.sendMessage(ChatUtils.chatMessage("&aData migration complete!")));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(
                        AranarthCore.getInstance(),
                        () -> sender.sendMessage(ChatUtils.chatMessage("&cMigration failed — check console for details.")));
                AranarthCore.getInstance().getLogger().severe("Migration error: " + e.getMessage());
            }
        });

        return true;
    }
}
