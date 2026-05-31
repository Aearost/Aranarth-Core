package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows for the player to unlock the next clicked Locked Container.
 */
public class CommandUnlock implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.isUnlockingContainer()) {
				aranarthPlayer.setUnlockingContainer(false);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				sender.sendMessage(ChatUtils.chatMessage("&7You are no longer unlocking containers"));
			} else {
				aranarthPlayer.setTrustedPlayerUUID(null);
				aranarthPlayer.setUntrustedPlayerUUID(null);
				aranarthPlayer.setUnlockingContainer(true);
				aranarthPlayer.setLockingContainer(false);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				sender.sendMessage(ChatUtils.chatMessage("&7You are now unlocking containers - right-click to unlock them"));
				sender.sendMessage(ChatUtils.chatMessage("&7Run &e/unlock &7again to exit unlocking mode"));
			}
        } else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis command can only be executed in-game!"));
        }
        return true;
    }

}
