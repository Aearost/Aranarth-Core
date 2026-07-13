package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.ItemUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * Teleports the player to the Survival world, sharing the Survival inventory.
 */
public class CommandSurvival implements CommandExecutor {

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
				return true;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			// On the SMP server: countdown here first, then transfer to Survival for RTP on arrival.
			if (AranarthCore.isSmpServer() && NetworkManager.isActive()) {
				AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
						aranarthPlayer.isInAdminMode(), "&e&lSurvival", "&7Transferring to Survival...", success -> {
					if (success) {
						AranarthPlayer apTransfer = AranarthUtils.getPlayer(player.getUniqueId());
                        apTransfer.setSurvivalInventory(ItemUtils.toBase64(player.getInventory()));
                        AranarthUtils.setPlayer(player.getUniqueId(), apTransfer);
						PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());
						player.getInventory().clear();
						com.aearost.aranarthcore.network.PendingTeleport survivalPt =
								com.aearost.aranarthcore.network.PendingTeleport.forCommand(
										"survival", "&e&lSurvival", "&7You have teleported to Survival");
						survivalPt.setApplyInventory(true);
						NetworkManager.getInstance().setPendingTeleport(player.getUniqueId(), survivalPt);
						String survivalServerName = AranarthCore.getInstance().getConfig()
								.getString("network.servers.survival", "survival");
						NetworkManager.getInstance().transferPlayer(player, survivalServerName);
					}
				});
				return true;
			}

			if (System.currentTimeMillis() < aranarthPlayer.getLastWorldCommandUse() + 60000) {
				if (!aranarthPlayer.isInAdminMode()) {
					int wait = (int) ((aranarthPlayer.getLastWorldCommandUse() + 60000) - System.currentTimeMillis()) / 1000;
					player.sendMessage(ChatUtils.chatMessage("&cYou must wait another &e" + wait + " seconds &cto use this command!"));
					return true;
				}
			}

			World world = Bukkit.getWorld("world");
			Random random = new Random();
			Location selectedLocation = null;
			boolean isLocationFound = false;
			while (!isLocationFound) {
				int x = random.nextInt(24501) - 12250;
				int z = random.nextInt(24501) - 12250;
				if (world.getHighestBlockAt(x, z).getType() != Material.WATER
						&& DominionUtils.getDominionOfChunk(world.getChunkAt(x >> 4, z >> 4)) == null) {
					isLocationFound = true;
					selectedLocation = world.getHighestBlockAt(x, z).getLocation();
					selectedLocation.add(0, 1, 0);
				}
			}

			AranarthUtils.teleportPlayer(player, player.getLocation(), selectedLocation, aranarthPlayer.isInAdminMode(), "&e&lSurvival", "&7You have teleported to Survival", success -> {
				if (success) {
					aranarthPlayer.setLastWorldCommandUse(System.currentTimeMillis());
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eSurvival"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &eSurvival"));
				}
			});
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
			return true;
		}
	}
}
