package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.objects.AranarthVote;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
			if (vote.getServiceName().equals("AranarthCore") && vote.getAddress().equals("127.0.0.1")) {
				return;
			}

			Player player = Bukkit.getPlayer(uuid);
			ItemStack key = new KeyVote().getItem();
			int amount = 1;
			int random = new Random().nextInt(1000);
			// 0.1% chance
			if (random == 0) {
				amount = 25;
			}
			// 0.5% chance
			else if (random == 5) {
				amount = 10;
			}
			// 1.2% chance
			else if (random < 12) {
				amount = 5;
			}
			// 5% chance
			else if (random < 50) {
				amount = 3;
			}
			// 15% chance
			else if (random < 150) {
				amount = 2;
			}

			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) {
					onlinePlayer.sendMessage(ChatUtils.chatMessage("&7You voted and received &a" + amount + " vote points!"));
				} else {
					onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + AranarthUtils.getPlayer(player.getUniqueId()).getNickname() + " &7has voted and received &a" + amount + " vote points!"));
				}
				onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, ThreadLocalRandom.current().nextFloat(1.4F, 1.7F));
			}

			// Adds their vote
			AranarthUtils.addVote(new AranarthVote(player.getUniqueId(), amount, System.currentTimeMillis()));

			// Give the key if the player is online and in a valid world, otherwise store it as pending
			String worldName = player != null ? player.getWorld().getName() : "";
			boolean validWorld = worldName.startsWith("world") || worldName.startsWith("smp") || worldName.startsWith("resource");
			if (player != null && validWorld) {
				HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(key);
				if (!remainder.isEmpty()) {
					AranarthUtils.addPendingVoteKeys(uuid, 1);
					player.sendMessage(ChatUtils.chatMessage("&7Your inventory was full! Use &e/keyclaim &7to claim your vote key"));
				}
			} else {
				AranarthUtils.addPendingVoteKeys(uuid, 1);
				if (player != null) {
					player.sendMessage(ChatUtils.chatMessage("&7You cannot receive crate keys here! Use &e/keyclaim &7in Survival!"));
				}
			}
		} else {
			Bukkit.getLogger().info("Player " + username + " voted but has never joined the server before");
		}
	}
}
