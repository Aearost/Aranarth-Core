package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Lists the players with the highest balances.
 */
public class CommandSkull {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.skull")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
				return true;
			}

			if (args.length == 1) {
				player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac skull <username>"));
				return true;
			}

			UUID skullUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[1]);
			if (skullUuid != null) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(skullUuid);
				OfflinePlayer skullPlayer = Bukkit.getOfflinePlayer(skullUuid);
				ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
				SkullMeta meta = (SkullMeta) skull.getItemMeta();
				meta.setOwningPlayer(skullPlayer);
				String displayName = aranarthPlayer != null ? aranarthPlayer.getNickname() : skullPlayer.getName();
				meta.setDisplayName(ChatUtils.translateToColor("&e" + displayName + "&e's Skull"));
				skull.setItemMeta(meta);
				player.sendMessage(ChatUtils.chatMessage("&7You have given yourself &e" + skullPlayer.getName() + "'s &7skull"));
				player.getInventory().addItem(skull);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &ccould not be found"));
			}
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
	}


}
