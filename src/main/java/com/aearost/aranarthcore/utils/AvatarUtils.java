package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.*;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;


/**
 * Provides a large variety of utility methods for everything related to AranarthCore.
 */
public class AvatarUtils {

	private static List<Avatar> avatars = new ArrayList<>();

	/**
	 * Provides the list of all avatars.
	 * @return The list of all avatars.
	 */
	public static List<Avatar> getAvatars() {
		return avatars;
	}

	/**
	 * Updates the list of all avatars.
	 * @param newAvatars The list of all avatars.
	 */
	public static void setAvatars(List<Avatar> newAvatars) {
		avatars = newAvatars;
	}

	/**
	 * Adds a new avatar.
	 * @param avatar The new avatar.
	 */
	public static void addAvatar(Avatar avatar) {
		avatars.add(avatar);
	}

	/**
	 * Provides the current reigning Avatar.
	 * @return The current reigning Avatar.
	 */
	public static Avatar getCurrentAvatar() {
		if (!avatars.isEmpty()) {
			return avatars.get(avatars.size() - 1);
		}
		return null;
	}

	/**
	 * Selects a new Avatar from the list of online players.
	 * @return Confirmation if a new Avatar was selected.
	 */
	public static boolean selectAvatar() {
		if (Bukkit.getOnlinePlayers().isEmpty()) {
			return false;
		}

		Random random = new Random();
		int index = random.nextInt(Bukkit.getOnlinePlayers().size());
		Avatar avatar = null;

		if (Bukkit.getOnlinePlayers().size() == 1) {
			index = 0;
		}

		int attempts = 0;
		// First avatar should be fully random
		if (avatars.isEmpty()) {
			while (true) {
				// Try again upon next execution
				if (attempts == 500) {
					return false;
				}

				Player player = (Player) Bukkit.getOnlinePlayers().toArray()[index];
				BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
				if (bendingPlayer == null) {
					attempts++;
					continue;
				}

				if (bendingPlayer.getElements().contains(Element.CHI)) {
					attempts++;
					continue;
				} else {
					char element = bendingPlayer.getElements().get(0).getName().charAt(0);
					avatar = new Avatar(player.getUniqueId(), DateUtils.getRawInGameDate(), "",
							DateUtils.getRawInRealLifeDate(), "", element);

					setNewAvatar(avatar);
					DiscordUtils.addAvatarMessageToDiscord(avatar, true);
					return true;
				}
			}
		}

		Avatar previousAvatar = avatars.get(avatars.size() - 1);

		// If there is currently a reigning avatar
		if (previousAvatar != null) {
			removeCurrentAvatar();
			return true;
		} else {
			char previousAvatarElement = avatars.get(avatars.size() - 2).getElement();
			char newAvatarElement = switch (previousAvatarElement) {
				case 'A' -> 'W';
				case 'W' -> 'E';
				case 'E' -> 'F';
				default -> 'A';
			};

			UUID recentAvatar = null;
			while (true) {
				Player player = (Player) Bukkit.getOnlinePlayers().toArray()[index];
				BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
				char playerElement = bendingPlayer.getElements().get(0).getName().charAt(0);

				// Try again upon next execution
				if (attempts == 1000) {
					if (recentAvatar != null) {
						Bukkit.getLogger().info("No other avatar found, defaulting to this one");
						avatar = new Avatar(player.getUniqueId(), DateUtils.getRawInGameDate(), "",
								DateUtils.getRawInRealLifeDate(), "", playerElement);
						avatars.remove(avatars.size() - 1); // It is the null placeholder
						setNewAvatar(avatar);
						DiscordUtils.addAvatarMessageToDiscord(avatar, true);
						return true;
					}
					return false;
				}

				if (bendingPlayer == null) {
					attempts++;
					continue;
				}

				if (bendingPlayer.getElements().contains(Element.CHI)) {
					attempts++;
					continue;
				} else {
					// Will select if not one of the last 5 avatars
					// Otherwise search for another and default to them if none other is found

					if (playerElement == newAvatarElement) {
						if (!isOneOfLastFiveAvatars(player.getUniqueId())) {
							avatar = new Avatar(player.getUniqueId(), DateUtils.getRawInGameDate(), "",
									DateUtils.getRawInRealLifeDate(), "", playerElement);
							avatars.remove(avatars.size() - 1); // It is the null placeholder
							setNewAvatar(avatar);
							DiscordUtils.addAvatarMessageToDiscord(avatar, true);
							return true;
						} else {
							recentAvatar = player.getUniqueId();
							attempts++;
						}
					} else {
						attempts++;
					}
				}
			}
		}
	}

	/**
	 * Determines if the player was one of the last five avatars.
	 * @param uuid The player's UUID.
	 * @return Confirmation if the player was one of the last five avatars.
	 */
	private static boolean isOneOfLastFiveAvatars(UUID uuid) {
		List<UUID> lastFive = new ArrayList<>();
		// Cycle from last to first
		for (int i = avatars.size() - 1; i > 0; i--) {
			if (avatars.get(i) == null) {
				continue;
			}
			lastFive.add(avatars.get(i).getUuid());
		}

		return lastFive.contains(uuid);
	}

	/**
	 * Updates the Avatar to the new Avatar.
	 * @param avatar The new Avatar.
	 */
	public static void setNewAvatar(Avatar avatar) {
		avatars.add(avatar);
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(avatar.getUuid());
//		PermissionUtils.assignAvatarPermissions(avatar); TODO

		for (Player player : Bukkit.getOnlinePlayers()) {
			player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_INHALE, 1F, 0.1F);
		}

		// Adds a 2-second delay
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&5&l&oThe new Avatar &d" + aranarthPlayer.getNickname() + " &5&l&ohas risen!"));
				for (Player player : Bukkit.getOnlinePlayers()) {
					player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1F, 0.8F);
				}
			}
		}.runTaskLater(AranarthCore.getInstance(), 30);
	}

	/**
	 * Removes the current Avatar.
	 */
	public static void removeCurrentAvatar() {
		Avatar oldAvatar = avatars.get(avatars.size() - 1);
		if (oldAvatar == null) {
			Bukkit.getLogger().info("Currently no a");
			return;
		}

		oldAvatar.setEndInGame(DateUtils.getRawInGameDate());
		oldAvatar.setEndInRealLife(DateUtils.getRawInRealLifeDate());
		avatars.set(avatars.size() - 1, oldAvatar);
		avatars.add(null);
		DiscordUtils.addAvatarMessageToDiscord(oldAvatar, false);

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(oldAvatar.getUuid());
		Bukkit.broadcastMessage(ChatUtils.chatMessage("&5&l&oThe Avatar &d" + aranarthPlayer.getNickname() + " &5&l&ohas deceased..."));
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1F, 1.5F);
		}

		// Adds a 2-second delay
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&5&l&oA new Avatar must be found..."));
				for (Player player : Bukkit.getOnlinePlayers()) {
					player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_IDLE_AIR, 1F, 0.4F);
				}
			}
		}.runTaskLater(AranarthCore.getInstance(), 70);
	}

}
