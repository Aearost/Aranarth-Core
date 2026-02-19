package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.OfflineBendingPlayer;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;


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
				}
				// An avatar was selected
				else {
					if (bendingPlayer.getElements().isEmpty()) {
						attempts++;
						continue;
					}
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
		}
		// If there is not currently a reigning avatar but one has previously existed
		else {
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

				// Try again upon next execution
				if (attempts == 1000) {
					if (recentAvatar != null) {
						char playerElement = bendingPlayer.getElements().get(0).getName().charAt(0);
						Bukkit.getLogger().info("No other avatar found, defaulting to this one");
						avatar = new Avatar(player.getUniqueId(), DateUtils.getRawInGameDate(), "",
								DateUtils.getRawInRealLifeDate(), "", playerElement);
						avatars.remove(avatars.size() - 1); // Removing the null placeholder
						setNewAvatar(avatar);
						DiscordUtils.addAvatarMessageToDiscord(avatar, true);
						return true;
					}
					return false;
				}

				if (bendingPlayer == null || bendingPlayer.getElements().isEmpty()) {
					attempts++;
					continue;
				}

				if (bendingPlayer.getElements().contains(Element.CHI)) {
					attempts++;
					continue;
				} else {
					char playerElement = bendingPlayer.getElements().get(0).getName().charAt(0);

					if (playerElement == newAvatarElement) {
						// An avatar was selected
						if (!isOneOfLastFiveAvatars(player.getUniqueId())) {

							avatar = new Avatar(player.getUniqueId(), DateUtils.getRawInGameDate(), "",
									DateUtils.getRawInRealLifeDate(), "", playerElement);
							avatars.remove(avatars.size() - 1); // Removing the null placeholder
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
		if (Bukkit.getOfflinePlayer(avatar.getUuid()).isOnline()) {
			PermissionUtils.evaluatePlayerPermissions(Bukkit.getPlayer(avatar.getUuid()));
		}
		updateNewestAvatarMannequin();

		for (Player player : Bukkit.getOnlinePlayers()) {
			player.playSound(player, Sound.ENTITY_BREEZE_INHALE, 1F, 0.1F);
		}

		// Adds a 2-second delay
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&5&l&oThe new Avatar &d" + aranarthPlayer.getNickname() + " &5&l&ohas risen!"));
				for (Player player : Bukkit.getOnlinePlayers()) {
					player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1F, 0.8F);
				}
				PersistenceUtils.saveAvatarBinds();
			}
		}.runTaskLater(AranarthCore.getInstance(), 30);
	}

	/**
	 * Removes the current Avatar.
	 */
	public static void removeCurrentAvatar() {
		Avatar oldAvatar = avatars.get(avatars.size() - 1);
		if (oldAvatar == null) {
			return;
		}

		oldAvatar.setEndInGame(DateUtils.getRawInGameDate());
		oldAvatar.setEndInRealLife(DateUtils.getRawInRealLifeDate());
		avatars.set(avatars.size() - 1, oldAvatar);
		avatars.add(null);
		PermissionUtils.updateAvatarPermissions(oldAvatar.getUuid(), true);
		shiftAvatarMannequins();

		DiscordUtils.addAvatarMessageToDiscord(oldAvatar, false);

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(oldAvatar.getUuid());
		Bukkit.broadcastMessage(ChatUtils.chatMessage("&5&l&oThe Avatar &d" + aranarthPlayer.getNickname() + " &5&l&ohas deceased..."));
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.playSound(player, Sound.ENTITY_WITHER_DEATH, 1F, 1.5F);
		}

		// Adds a 2-second delay
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&5&l&oA new Avatar must be found..."));
				for (Player player : Bukkit.getOnlinePlayers()) {
					player.playSound(player, Sound.ENTITY_BREEZE_IDLE_AIR, 1F, 0.4F);
				}
			}
		}.runTaskLater(AranarthCore.getInstance(), 70);
	}

	/**
	 * Updates the latest avatar mannequin when a new avatar is selected.
	 */
	private static void updateNewestAvatarMannequin() {
		Location center = new Location(Bukkit.getWorld("world"), 0, 200, 0);
		for (Entity entity : center.getNearbyEntities(1, 5, 1)) {
			if (entity instanceof Mannequin mannequin) {
				mannequin.setInvulnerable(true);
				mannequin.setGravity(false);
				mannequin.setPersistent(true);
				mannequin.setNoPhysics(false);
				mannequin.setInvisible(false);
				mannequin.setImmovable(true);

				UUID uuid = getCurrentAvatar().getUuid();
				OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
				mannequin.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
				String elementSymbol = getElementSymbol(uuid, getCurrentAvatar());
				mannequin.setCustomName(ChatUtils.translateToColor(elementSymbol + " &r" + aranarthPlayer.getNickname() + " " + elementSymbol));
			}
		}
	}

	/**
	 * Cycles all avatar mannequins down the line when an avatar dies.
	 */
	private static void shiftAvatarMannequins() {
		Location center = new Location(Bukkit.getWorld("world"), 0, 200, 0);
		for (Entity entity : center.getNearbyEntities(30, 5, 30)) {
			if (entity instanceof Mannequin mannequin) {
				mannequin.setInvulnerable(true);
				mannequin.setGravity(false);
				mannequin.setPersistent(true);
				mannequin.setNoPhysics(false);
				mannequin.setImmovable(true);

				Bukkit.getLogger().info("-----------");
				int indexInShownAvatars = getMannequinPosition(mannequin);

				// Skip the last one as it will simply be overridden
				if (indexInShownAvatars == 4) {
					continue;
				}
				// Hide the first one as there is no active avatar
				else if (indexInShownAvatars == 0) {
					mannequin.setInvisible(true);
				}
				// Move all avatars over by 1
				else {
					Bukkit.getLogger().info("Index Before: " + indexInShownAvatars);
					// Ensures only the last 50 avatars will be shown
					// Skips the last one as well as it is null
					int overallIndex = (avatars.size() - 2) - indexInShownAvatars;

					Bukkit.getLogger().info("Index After: " + overallIndex);
					if (avatars.get(overallIndex + 1) != null) {
						Bukkit.getLogger().info("Updating the mannequin");
						UUID uuid = avatars.get(overallIndex + 1).getUuid();
						OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
						mannequin.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
						String elementSymbol = getElementSymbol(uuid, avatars.get(overallIndex + 1));
						mannequin.setCustomName(ChatUtils.translateToColor(elementSymbol + " &r" + aranarthPlayer.getNickname() + " " + elementSymbol));
						mannequin.setCustomNameVisible(true);
					}
				}


			}
		}
	}

	/**
	 * Provides the numeric position that the mannequin is in the cycle.
	 * @param mannequin The mannequin.
	 * @return The numeric position that the mannequin is in the cycle.
	 */
	private static int getMannequinPosition(Mannequin mannequin) {
		Location loc = mannequin.getLocation();
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		Bukkit.getLogger().info(x + "|" + y + "|" + z);

		if (x == 0 && y == 200 && z == 0) {
			return 0;
		} else if (x == 2 && y == 200 && z == 0) {
			return 1;
		} else if (x == 4 && y == 200 && z == 0) {
			return 2;
		} else if (x == 6 && y == 200 && z == 0) {
			return 3;
		} else {
			return 4;
		}
//		else if (x == 10 && y == 200 && z == 0) {
//			return 5;
//		} else if (x == 12 && y == 200 && z == 0) {
//			return 6;
//		} else if (x == 14 && y == 200 && z == 0) {
//			return 7;
//		} else if (x == 16 && y == 200 && z == 0) {
//			return 8;
//		} else if (x == 18 && y == 200 && z == 0) {
//			return 9;
//		} else if (x == 20 && y == 200 && z == 0) {
//			return 10;
//		} else if (x == 22 && y == 200 && z == 0) {
//			return 11;
//		} else if (x == 24 && y == 200 && z == 0) {
//			return 12;
//		}
	}

	/**
	 * Provides the symbol of the current element of the input player.
	 * @param uuid The UUID of the player.
	 * @param avatar The associated avatar iteration if the input is or was an avatar.
	 * @return The symbol of the current element of the input player.
	 */
	public static String getElementSymbol(UUID uuid, Avatar avatar) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
		OfflineBendingPlayer bendingPlayer = new OfflineBendingPlayer(player);

		Element element = Element.AIR;
		if (avatar != null) {
			char elementCharacter = avatar.getElement();
			if (elementCharacter == 'W') {
				element = Element.WATER;
			} else if (elementCharacter == 'E') {
				element = Element.EARTH;
			} else if (elementCharacter == 'F') {
				element = Element.FIRE;
			}
		} else {
			element = bendingPlayer.getElements().getFirst();
		}

		String elementAsString = "";
		if (element == Element.WATER) {
			elementAsString = "&b水";
		} else if (element == Element.EARTH) {
			elementAsString = "&a土";
		} else if (element == Element.FIRE) {
			elementAsString = "&c火";
		} else {
			elementAsString = "&7気";
		}
		return elementAsString;
	}

}
