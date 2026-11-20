package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiRecipes;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Overrides the vanilla /recipe command as a shortcut to /aranarth recipes.
 */
public class CommandRecipe implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
            return true;
        }
        GuiRecipes.open(player, args.length > 0 ? String.join(" ", args) : null);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Free-text search, so player names should not be suggested
        return List.of();
    }
}
