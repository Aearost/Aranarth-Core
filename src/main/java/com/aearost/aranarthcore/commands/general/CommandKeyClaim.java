package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

/**
 * Allows players to claim any pending vote crate keys that could not be
 * delivered at the time of voting.
 */
public class CommandKeyClaim implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}

		String worldName = player.getWorld().getName();
		if (!worldName.startsWith("world") && !worldName.startsWith("smp")
				&& !worldName.startsWith("resource") && !worldName.startsWith("spawn")) {
			player.sendMessage(ChatUtils.chatMessage("&cYou can only claim vote keys in survival worlds!"));
			return true;
		}

		UUID uuid = player.getUniqueId();
		Integer pending = AranarthUtils.getPendingVoteKeys().get(uuid);
		if (pending == null || pending == 0) {
			player.sendMessage(ChatUtils.chatMessage("&7You have no pending vote keys to claim!"));
			return true;
		}

		int claimed = 0;
		int remaining = pending;
		while (remaining > 0) {
			ItemStack key = new KeyVote().getItem();
			HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(key);
			if (!remainder.isEmpty()) {
				break;
			}
			claimed++;
			remaining--;
		}

		if (claimed == 0) {
			player.sendMessage(ChatUtils.chatMessage("&cYour inventory is full! Make some room and try again."));
			return true;
		}

		if (remaining == 0) {
			AranarthUtils.removePendingVoteKeys(uuid);
		} else {
			AranarthUtils.setPendingVoteKeys(uuid, remaining);
		}

		if (remaining > 0) {
			player.sendMessage(ChatUtils.chatMessage("&7Claimed &a" + claimed + " &7vote key(s). Your inventory is full; you still have &a" + remaining + " &7key(s) to claim."));
		} else {
			player.sendMessage(ChatUtils.chatMessage("&7Claimed all &a" + claimed + " &7pending vote key(s)!"));
		}

		return true;
	}
}
