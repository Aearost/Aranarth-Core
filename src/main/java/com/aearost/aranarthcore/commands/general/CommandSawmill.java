package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

/**
 * Opens a sawmill (stonecutter) GUI.
 */
public class CommandSawmill implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.tables")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return true;
			}

			MenuType.STONECUTTER.builder()
					.title(Component.text("Sawmill"))
					.checkReachable(false)
					.build(player)
					.open();
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
		}
		return true;
	}

}
