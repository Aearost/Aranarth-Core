package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.aearost.aranarthcore.commands.general.CommandLock.scheduleToggleExpiry;

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
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("lock.mode_exit_lock")));
			} else {
				aranarthPlayer.setTrustedPlayerUUID(null);
				aranarthPlayer.setUntrustedPlayerUUID(null);
				aranarthPlayer.setUnlockingContainer(true);
				aranarthPlayer.setLockingContainer(false);
				aranarthPlayer.setContainerToggleExpiry(System.currentTimeMillis() + 5000);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("lock.unlock_mode_enter")));
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("lock.unlock_mode_hint")));
				scheduleToggleExpiry(player.getUniqueId());
			}
        } else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
        }
        return true;
    }

}
