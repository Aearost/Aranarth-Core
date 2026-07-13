package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiHomes;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.PendingTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to teleport to one of their homes
 */
public class CommandHome implements CommandExecutor {

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
			if (args.length >= 1) {
				StringBuilder homeNameBuilder = new StringBuilder();
				for (int i = 0; i < args.length; i++) {
					homeNameBuilder.append(args[i]);
					if (i < args.length - 1) {
						homeNameBuilder.append(" ");
					}
				}

				String homeName = homeNameBuilder.toString();
				for (Home home : aranarthPlayer.getHomes()) {
					if (homeName.equalsIgnoreCase(ChatUtils.stripColorFormatting(home.getName()))) {
						boolean networkActive = NetworkManager.isActive();

						// Survival server → SMP home: transfer to SMP then teleport there
						if (networkActive && home.isSmpHome() && !AranarthCore.isSmpServer()) {
							String smpWorldPart = home.getWorldName().substring(4); // strip "smp:"
							double hx = home.getLocation().getX();
							double hy = home.getLocation().getY();
							double hz = home.getLocation().getZ();
							float hyaw = home.getLocation().getYaw();
							float hpitch = home.getLocation().getPitch();
							String smpServer = AranarthCore.getInstance().getConfig()
									.getString("network.servers.smp", "smp");
							AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
									aranarthPlayer.isInAdminMode(), home.getName(), "&7Transferring to your home...", success -> {
								if (success) {
									NetworkManager.getInstance().saveInventoryAndTransfer(player, smpServer,
											new PendingTeleport(smpWorldPart, hx, hy, hz, hyaw, hpitch,
													home.getName(), "&7You have teleported to your home"));
								}
							});
							return true;
						}

						// SMP server → Survival home: transfer to Survival then teleport there
						if (networkActive && !home.isSmpHome() && AranarthCore.isSmpServer()) {
							double hx = home.getLocation().getX();
							double hy = home.getLocation().getY();
							double hz = home.getLocation().getZ();
							float hyaw = home.getLocation().getYaw();
							float hpitch = home.getLocation().getPitch();
							String survivalServer = AranarthCore.getInstance().getConfig()
									.getString("network.servers.survival", "survival");
							AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
									aranarthPlayer.isInAdminMode(), home.getName(), "&7Transferring to your home...", success -> {
								if (success) {
									NetworkManager.getInstance().saveInventoryAndTransfer(player, survivalServer,
											new PendingTeleport(home.getWorldName(), hx, hy, hz, hyaw, hpitch,
													home.getName(), "&7You have teleported to your home"));
								}
							});
							return true;
						}

						// Same-server home
						AranarthUtils.teleportPlayer(player, player.getLocation(), home.getLocation(), aranarthPlayer.isInAdminMode(), home.getName(), "&7You have teleported to your home", success -> {
							if (success) {
								player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + home.getName()));
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &e" + home.getName()));
							}
						});
						return true;
					}
				}
				player.sendMessage(ChatUtils.chatMessage("&cThis home could not be found!"));
			} else {
				if (aranarthPlayer.getHomes().isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage("&7You do not have any homes"));
				} else {
					GuiHomes gui = new GuiHomes(player);
					gui.openGui();
					return true;
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
		}
		return false;
	}
}
