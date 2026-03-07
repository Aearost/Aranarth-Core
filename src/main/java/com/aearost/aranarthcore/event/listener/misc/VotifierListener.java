package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

/**
 * Handles the logic behind a player vote being received by the server.
 */
public class VotifierListener implements Listener {

	public VotifierListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onPlayerVote(VotifierEvent e) {
		String username = e.getVote().getUsername();
		UUID uuid = AranarthUtils.getUUIDFromUsername(username);
		if (uuid != null) {
			Vote vote = e.getVote();
			// If it was a test vote, do not increase the number of votes the player has
//			if (vote.getServiceName().equals("AranarthCore") && vote.getAddress().equals("127.0.0.1")) { TODO
//				return;
//			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
			aranarthPlayer.setVoteTotal(aranarthPlayer.getVoteTotal() + 1);
			aranarthPlayer.setVotePoints(aranarthPlayer.getVotePoints() + 1);
			AranarthUtils.setPlayer(uuid, aranarthPlayer);

			Player player = Bukkit.getPlayer(uuid);
			ItemStack key = new KeyVote().getItem();
			int amount = 1;
			int random = new Random().nextInt(1000);
			// 0.1% chance
			if (random == 0) {
				amount = 10;
			}
			// 1% chance
			else if (random < 10) {
				amount = 5;
			}
			// 5% chance
			else if (random < 50) {
				amount = 3;
			}
			// 10% chance
			else if (random < 100) {
				amount = 2;
			}
			key.setAmount(amount);

			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) {
					onlinePlayer.sendMessage(ChatUtils.chatMessage("&7You voted and received &e" + key.getItemMeta().getDisplayName() + " x" + key.getAmount()));
				} else {
					onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has voted and received &e" + key.getItemMeta().getDisplayName() + " x" + key.getAmount()));
				}
			}

			// If the player is online
			if (player != null) {
				HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(key);
				if (!remainder.isEmpty()) {
					player.getLocation().getWorld().dropItemNaturally(player.getLocation(), remainder.get(0));
					player.sendMessage(ChatUtils.chatMessage("&7Your crate key was dropped to the ground"));
				}
			}
		} else {
			Bukkit.getLogger().info("Player " + username + " voted but has never joined the server before");
		}
	}
}
