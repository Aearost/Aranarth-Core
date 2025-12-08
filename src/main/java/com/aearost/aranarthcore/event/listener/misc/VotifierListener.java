package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.crates.KeyVote;
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
//			if (vote.getServiceName().equals("AranarthCore") && vote.getAddress().equals("127.0.0.1")) {
//				return;
//			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
			aranarthPlayer.setVoteTotal(aranarthPlayer.getVoteTotal() + 1);
			aranarthPlayer.setVotePoints(aranarthPlayer.getVotePoints() + 1);
			AranarthUtils.setPlayer(uuid, aranarthPlayer);

			Player player = Bukkit.getPlayer(uuid);
			ItemStack key = new KeyVote().getItem();
			// If the player is online
			if (player != null) {
				HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(key);
				player.sendMessage(ChatUtils.chatMessage("&7You have received a &e" + key.getItemMeta().getDisplayName() + "!"));
				if (!remainder.isEmpty()) {
					player.getLocation().getWorld().dropItemNaturally(player.getLocation(), remainder.get(0));
					player.sendMessage(ChatUtils.chatMessage("&7Your crate key was dropped to the ground"));
				}
			}

			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) {
					continue;
				} else {
					if (player != null && player.isOnline()) {
						onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has voted and received a &e" + key.getItemMeta().getDisplayName() + "!"));
					} else {
						onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has voted for the server!"));
					}
				}
			}

			// Probably good to make a VotePlayer class or something like that
			// Store the date each vote was made i.e 1207251 --> December 7th 2025 on vote site #1
			// Or it might just be better to store two more int in aranarth player which would be "totalVoteNum" and "votePoints"
			// Using this and adding logic in the listener, I could make it so that it will add to a temporary variable in AranarthUtils
			// i.e add to another separate file the totalVoteNumForAllPlayers whenever the vote event is done, and can have one row per month
			// Make vote points be able to purchase perks or crate keys, or for expensive amounts, purchase monthly saint
		} else {
			Bukkit.getLogger().info("Player " + username + " voted but has never joined the server before");
		}
	}
}
