package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * /ac migrate - one-time command to push all in-memory data into MySQL.
 * Run this once on the Survival server after first deploying the network setup.
 */
public class CommandMigrate {

    public static boolean onCommand(CommandSender sender, String[] args) {
        if (!DatabaseManager.isActive()) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("migrate.mysql_not_active")));
            return true;
        }

        sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.migration_start")));

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
                        () -> sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.migration_done"))));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(
                        AranarthCore.getInstance(),
                        () -> sender.sendMessage(ChatUtils.chatMessage(Lang.get("migrate.failed"))));
                AranarthCore.getInstance().getLogger().severe("Migration error: " + e.getMessage());
            }
        });

        return true;
    }
}
