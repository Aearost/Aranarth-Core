package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.NetworkManager;
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
 * Teleports the player to the spawn of the Creative plots world, using the creative inventory.
 */
public class CommandCreative implements CommandExecutor {

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

			if (args.length > 0 && args[0].equalsIgnoreCase("expand")) {
				expand(player, aranarthPlayer, args);
				return true;
			}

			if (AranarthUtils.getTeleportTask(player.getUniqueId()) != null) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.already_teleporting")));
				return false;
			}

			// On the SMP server: the plots world lives on Survival - count down then transfer.
			if (!PlotUtils.isPlotServer()) {
				CommandPlot.transferToPlotServer(player, aranarthPlayer, "creative", "&e&lCreative");
				return true;
			}

			Location creativeSpawn = PlotUtils.getSpawnLocation();
			if (creativeSpawn.getWorld() == null) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.could_not_creative")));
				return false;
			}
			AranarthUtils.teleportPlayer(player, player.getLocation(), creativeSpawn, aranarthPlayer.isInAdminMode(), "&e&lCreative", Lang.get("teleport.to_creative"), success -> {
				if (success) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.to_creative")));
				} else {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.could_not_creative")));
				}
			});

			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return false;
		}
	}

	/**
	 * Adds rings of plots around the Creative plots world, pre-generating the new area.
	 */
	private void expand(Player player, AranarthPlayer aranarthPlayer, String[] args) {
		if (aranarthPlayer.getCouncilRank() != 3) {
			player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
			return;
		}
		if (args.length != 2) {
			player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "creative expand <rings>")));
			return;
		}
		int rings;
		try {
			rings = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			rings = 0;
		}
		if (rings < 1) {
			player.sendMessage(ChatUtils.chatMessage(Lang.get("creative.invalid_rings")));
			return;
		}

		// The plots world only exists on Survival
		if (!PlotUtils.isPlotServer()) {
			if (!NetworkManager.isActive()) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.could_not_creative")));
				return;
			}
			NetworkManager.getInstance().publishCreativeExpand(player.getUniqueId(), rings);
			return;
		}
		int plotsAcross = PlotUtils.expand(rings);
		if (plotsAcross < 0) {
			player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.could_not_creative")));
			return;
		}
		player.sendMessage(ChatUtils.chatMessage(Lang.get("creative.expanded", "size", plotsAcross)));
	}
}
