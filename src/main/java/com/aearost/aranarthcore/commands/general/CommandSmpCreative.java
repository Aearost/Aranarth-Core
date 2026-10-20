package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.PendingTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PlotUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Teleports original players to the SMP creative world.
 */
public class CommandSmpCreative implements CommandExecutor {

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
			// Only the OG players, Council, and Architect are permitted into the SMP creative world
			if (!AranarthUtils.isOriginalPlayer(player.getUniqueId()) && aranarthPlayer.getCouncilRank() == 0
					&& aranarthPlayer.getArchitectRank() == 0) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "access.world_restricted")));
				return false;
			}

			if (AranarthUtils.getTeleportTask(player.getUniqueId()) != null) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.already_teleporting")));
				return false;
			}

			// On the Survival server: the SMP creative world lives on the SMP - count down then transfer.
			if (!AranarthCore.isSmpServer() && NetworkManager.isActive()) {
				AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
						aranarthPlayer.isInAdminMode(), Lang.getFor(player, "teleport.title_smp_creative"), Lang.getFor(player, "teleport.transferring_smp_creative"), success -> {
					if (success) {
						PendingTeleport pt = PendingTeleport.forCommand(
								"smpcreative", Lang.getFor(player, "teleport.title_smp_creative"), Lang.getFor(player, "teleport.to_smp_creative"));
						String smpServerName = AranarthCore.getInstance().getConfig()
								.getString("network.servers.smp", "smp");
						NetworkManager.getInstance().saveInventoryAndTransfer(player, smpServerName, pt);
					}
				});
				return true;
			}

			Location creativeSpawn = PlotUtils.getSmpCreativeSpawnLocation();
			if (creativeSpawn.getWorld() == null) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.could_not_smp_creative")));
				return false;
			}
			AranarthUtils.teleportPlayer(player, player.getLocation(), creativeSpawn, aranarthPlayer.isInAdminMode(), Lang.getFor(player, "teleport.title_smp_creative"), Lang.getFor(player, "teleport.to_smp_creative"), success -> {
				if (success) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.to_smp_creative")));
				} else {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.could_not_smp_creative")));
				}
			});

			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
			return false;
		}
	}
}
