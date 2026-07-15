package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.PendingTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Teleports the player to the arena world, sharing the survival inventory.
 */
public class CommandArena implements CommandExecutor {

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
			if (AranarthUtils.getTeleportTask(player.getUniqueId()) != null) {
				player.sendMessage(ChatUtils.chatMessage("&cYou are already teleporting somewhere!"));
				return false;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			// On the SMP server: arena world lives on Survival — count down then transfer.
			if (AranarthCore.isSmpServer() && NetworkManager.isActive()) {
				AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
						aranarthPlayer.isInAdminMode(), "&e&lArena", "&7Transferring to the Arena...", success -> {
					if (success) {
						PendingTeleport pt = PendingTeleport.forCommand(
								"arena", "&e&lArena", "&7You have teleported to the Arena");
						String survivalServerName = AranarthCore.getInstance().getConfig()
								.getString("network.servers.survival", "survival");
						NetworkManager.getInstance().saveInventoryAndTransfer(player, survivalServerName, pt);
					}
				});
				return true;
			}

			Location arenaSpawn = new Location(Bukkit.getWorld("arena"), 0.5, 105, 0.5, 180, 0);
			AranarthUtils.teleportPlayer(player, player.getLocation(), arenaSpawn, aranarthPlayer.isInAdminMode(), "&e&lArena", "&7You have teleported to the Arena", success -> {
				if (success) {
					player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to the &eArena &7world"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to the &eArena &cworld"));
				}
			});
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
		}
		return false;
	}
}
