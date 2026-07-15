package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Allows two players to mutually start a shared countdown.
 */
public class CommandCountdown implements CommandExecutor {

	private static final Map<UUID, UUID> pendingRequests = new HashMap<>();

	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}

		if (args.length < 1) {
			player.sendMessage(ChatUtils.chatMessage("&cUsage: /countdown <player>"));
			return true;
		}

		UUID targetUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
		if (targetUuid == null) {
			player.sendMessage(ChatUtils.chatMessage("&cPlayer not found!"));
			return true;
		}

		Player target = Bukkit.getPlayer(targetUuid);
		if (target == null || !target.isOnline()) {
			player.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
			return true;
		}

		if (target.equals(player)) {
			player.sendMessage(ChatUtils.chatMessage("&cYou cannot countdown with yourself!"));
			return true;
		}

		String targetNickname = AranarthUtils.getNickname(target);
		String playerNickname = AranarthUtils.getNickname(player);

		// If the target already sent a request to this player, start the countdown
		if (pendingRequests.containsKey(targetUuid) && pendingRequests.get(targetUuid).equals(player.getUniqueId())) {
			pendingRequests.remove(targetUuid);
			player.sendMessage(ChatUtils.chatMessage("&7Starting countdown with &e" + targetNickname + "&7!"));
			target.sendMessage(ChatUtils.chatMessage("&e" + playerNickname + " &7accepted! Starting countdown!"));
			startCountdown(player, target);
			return true;
		}

		// Register a new request
		pendingRequests.put(player.getUniqueId(), targetUuid);
		player.sendMessage(ChatUtils.chatMessage("&7Countdown request sent to &e" + targetNickname + "&7!"));
		target.sendMessage(ChatUtils.chatMessage("&e" + playerNickname + " &7wants to start a countdown with you!"));
		target.sendMessage(ChatUtils.chatMessage("&7Use &e/countdown " + player.getName() + " &7to confirm!"));
		return true;
	}

	private static void startCountdown(Player playerA, Player playerB) {
		new BukkitRunnable() {
			int count = 5;

			@Override
			public void run() {
				if (!playerA.isOnline() || !playerB.isOnline()) {
					cancel();
					return;
				}

				if (count > 0) {
					String color = count <= 2 ? "&e&l" : "&c&l";
					String title = ChatUtils.translateToColor(color + count);
					String subtitle = ChatUtils.translateToColor("&7Get ready...");
					playerA.sendTitle(title, subtitle, 3, 16, 5);
					playerB.sendTitle(title, subtitle, 3, 16, 5);
					// Rising major chord (root, major third, perfect fifth)
					float root = 0.8f + (0.1f * (5 - count));
					float third = root * 1.2599f;
					float fifth = root * 1.4983f;
					for (Player p : new Player[]{playerA, playerB}) {
						p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, root);
						p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.85f, third);
						p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, fifth);
					}
					count--;
				} else {
					String title = ChatUtils.translateToColor("&a&lGO!");
					playerA.sendTitle(title, "", 2, 35, 15);
					playerB.sendTitle(title, "", 2, 35, 15);
					// Ascending arpeggio jingle: C E G C
					float base = 1.0f;
					long[] delays = {0L, 3L, 6L, 9L};
					float[] pitches = {base, base * 1.2599f, base * 1.4983f, base * 2.0f};
					for (int i = 0; i < pitches.length; i++) {
						float pitch = pitches[i];
						Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
							if (playerA.isOnline()) playerA.playSound(playerA.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);
							if (playerB.isOnline()) playerB.playSound(playerB.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);
						}, delays[i]);
					}
					cancel();
				}
			}
		}.runTaskTimer(AranarthCore.getInstance(), 0L, 20L);
	}
}
