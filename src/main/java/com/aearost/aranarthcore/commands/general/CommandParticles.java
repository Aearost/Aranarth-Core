package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Modifies the amount of Aranarth particles seen by the player
 */
public class CommandParticles implements CommandExecutor {

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
			if (args.length == 0) {
				player.sendMessage(ChatUtils.chatMessage("&cYou must enter a percentage of particles to see!"));
				return true;
			} else {
				int percentage = 100;
				try {
					percentage = Integer.parseInt(args[0]);
					if (percentage < 0 || percentage > 200) {
						throw new NumberFormatException();
					}
				} catch (NumberFormatException e) {
					player.sendMessage(ChatUtils.chatMessage("&cYou must enter a number between 0 and 200!"));
					return true;
				}

				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				aranarthPlayer.setParticleNum(percentage);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				player.sendMessage(ChatUtils.chatMessage("&7You will now see &e" + percentage + "% &7of particles"));
				return true;
			}
        } else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
        }
    }

}
