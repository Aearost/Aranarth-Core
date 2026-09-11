package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiBrewBook;
import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.BrewRecipeUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
        }
        return true;
    }

    private boolean handleUnlock(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
            if (ap == null || ap.getCouncilRank() < 3) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
                return true;
            }
        }

        if (args.length < 3) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "brewbook unlock <player> <brew name>")));
            return true;
        }

        String targetName = args[1];
        UUID targetUuid = AranarthUtils.getUUIDFromUsername(targetName);
        if (targetUuid == null) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", targetName)));
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
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("brew.recipe_not_found", "name", brewName)));
            return true;
        }

        if (BrewRecipeUtils.isUnlocked(targetUuid, recipe)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("brew.already_unlocked_admin", "player", targetName, "recipe", recipe.getDisplayName())));
            return true;
        }

        BrewRecipeUtils.unlock(targetUuid, recipe.getId());

        AranarthPlayer targetAp = AranarthUtils.getPlayer(targetUuid);
        if (targetAp != null) {
            boolean questUpdated = QuestUtils.regenerateWeeklyQuestIfBrewReward(targetUuid, targetAp.getRank(), recipe.getId());
            if (questUpdated) {
                Player targetOnline = Bukkit.getPlayer(targetUuid);
                if (targetOnline != null) {
                    targetOnline.sendMessage(ChatUtils.chatMessage(Lang.get("quest.weekly_reward_updated")));
                }
            }
        }

        sender.sendMessage(ChatUtils.chatMessage(Lang.get("brew.recipe_unlocked_admin", "recipe", recipe.getDisplayName(), "player", targetName)));
        Player targetOnline = Bukkit.getPlayer(targetUuid);
        if (targetOnline != null) {
            targetOnline.sendMessage(ChatUtils.chatMessage(Lang.get("brew.recipe_unlocked", "recipe", recipe.getDisplayName())));
            targetOnline.sendMessage(ChatUtils.chatMessage(Lang.get("brew.use_brewbook")));
        }

        return true;
    }
}
