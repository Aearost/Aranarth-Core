package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiBrewBook;
import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.BrewRecipeUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.QuestUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

/**
 * Opens the player's Brew Book, showing all unlocked BreweryX recipes.
 * Also handles the council-only 'unlock' subcommand.
 */
public class CommandBrewBook implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("unlock")) {
            return handleUnlock(sender, args);
        }
        if (sender instanceof Player player) {
            new GuiBrewBook(player, 0).openGui();
        } else {
            sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
        }
        return true;
    }

    private boolean handleUnlock(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
            if (ap == null || ap.getCouncilRank() < 3) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
                return true;
            }
        }

        if (args.length < 3) {
            sender.sendMessage(ChatUtils.chatMessage("&cUsage: &e/brewbook unlock <player> <brew name>"));
            return true;
        }

        String targetName = args[1];
        UUID targetUuid = AranarthUtils.getUUIDFromUsername(targetName);
        if (targetUuid == null) {
            sender.sendMessage(ChatUtils.chatMessage("&cPlayer &e" + targetName + " &ccould not be found"));
            return true;
        }

        String brewName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        BrewRecipe recipe = null;
        for (BrewRecipe r : BrewRecipe.values()) {
            if (r.getDisplayName() != null && r.getDisplayName().equalsIgnoreCase(brewName)) {
                recipe = r;
                break;
            }
        }

        if (recipe == null) {
            sender.sendMessage(ChatUtils.chatMessage("&cBrew recipe &e" + brewName + " &ccould not be found"));
            return true;
        }

        if (BrewRecipeUtils.isUnlocked(targetUuid, recipe)) {
            sender.sendMessage(ChatUtils.chatMessage("&e" + targetName + " &calready has the &e" + recipe.getDisplayName() + " &crecipe unlocked"));
            return true;
        }

        BrewRecipeUtils.unlock(targetUuid, recipe.getId());

        AranarthPlayer targetAp = AranarthUtils.getPlayer(targetUuid);
        if (targetAp != null) {
            boolean questUpdated = QuestUtils.regenerateWeeklyQuestIfBrewReward(targetUuid, targetAp.getRank(), recipe.getId());
            if (questUpdated) {
                Player targetOnline = Bukkit.getPlayer(targetUuid);
                if (targetOnline != null) {
                    targetOnline.sendMessage(ChatUtils.chatMessage("&7One of your weekly quests has been updated with a new reward"));
                }
            }
        }

        sender.sendMessage(ChatUtils.chatMessage("&aUnlocked the &e" + recipe.getDisplayName() + " &abrew recipe for &e" + targetName));
        Player targetOnline = Bukkit.getPlayer(targetUuid);
        if (targetOnline != null) {
            targetOnline.sendMessage(ChatUtils.chatMessage("&7The &e" + recipe.getDisplayName() + " &7brew recipe has been unlocked for you!"));
            targetOnline.sendMessage(ChatUtils.chatMessage("&7Use &e/brewbook &7to view your unlocked recipes"));
        }

        return true;
    }
}
