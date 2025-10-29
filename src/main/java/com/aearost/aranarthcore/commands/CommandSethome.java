package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to set a home.
 */
public class CommandSethome {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			int playerMaxHomeCount = AranarthUtils.getMaxHomeNum(player);
			if (aranarthPlayer.getHomes().size() < playerMaxHomeCount) {
				if (args.length >= 2) {
					Location loc = player.getLocation();
					World world = loc.getWorld();

					// Start from the player's current Y position and go downward
					int x = loc.getBlockX();
					int z = loc.getBlockZ();
					int y = loc.getBlockY();

					Block solidBlock = null;

					for (int currentY = y; currentY >= world.getMinHeight(); currentY--) {
						Block currentBlock = world.getBlockAt(x, currentY, z);
						Material type = currentBlock.getType();

						// Check if this block is solid and not water/lava
						if (type.isSolid() && type != Material.WATER && type != Material.LAVA) {
							solidBlock = currentBlock;
							break;
						}
					}

					// If a solid block was found, place the player just above it
					Location surfaceLoc;
					if (solidBlock != null) {
						surfaceLoc = solidBlock.getLocation().add(0.0, 1, 0.0); // Center and place feet above
						surfaceLoc.setYaw(loc.getYaw());
						surfaceLoc.setPitch(loc.getPitch());
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot set a home here!"));
						return true;
					}

					// Construct the home name from args
					StringBuilder homeNameBuilder = new StringBuilder();
					for (int i = 1; i < args.length; i++) {
						homeNameBuilder.append(args[i]);
						if (i < args.length - 1) {
							homeNameBuilder.append(" ");
						}
					}
					String homeName = homeNameBuilder.toString();

					if (player.hasPermission("aranarth.chat.hex")) {
						homeName = ChatUtils.translateToColor(homeName);
					} else if (player.hasPermission("aranarth.chat.color")) {
						homeName = ChatUtils.playerColorChat(homeName);
					}

					// Create the home at the computed surface location
					Home home = new Home(homeName, surfaceLoc, Material.BARRIER);
					AranarthUtils.addPlayerHome(player, home);
					player.sendMessage(ChatUtils.chatMessage("&7You have added the home &e" + homeName));
					return true;
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot set more than &e" + playerMaxHomeCount + " &chomes!"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}

		return false;
	}

}
