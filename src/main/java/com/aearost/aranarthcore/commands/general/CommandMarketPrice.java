package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiMarketPrice;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens the Market Price GUI showing current prices for all tracked items.
 */
public class CommandMarketPrice implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }
        new GuiMarketPrice(player, 0, args.length > 0 ? String.join(" ", args) : null).openGui();
        return true;
    }
}
