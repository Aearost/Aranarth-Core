package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.OfflineBendingPlayer;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.*;
import org.bukkit.entity.*;
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

		// Despawns the mannequins and then respawns them with the updated values
		despawnAvatarMannequins();
		respawnAvatarMannequins();

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
		// Despawns the mannequins and then respawns them with the updated values
		despawnAvatarMannequins();
		respawnAvatarMannequins();

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

//	/**
//	 * Updates the latest avatar mannequin when a new avatar is selected.
//	 */
//	private static void updateNewestAvatarMannequin() {
//		// Center of the avatar room
//		Location center = new Location(Bukkit.getWorld("spawn"), 8, 107, 19);
//		for (Entity entity : center.getNearbyEntities(15, 15, 15)) {
//			if (entity instanceof Mannequin mannequin) {
//				mannequin.setInvulnerable(true);
//				mannequin.setGravity(false);
//				mannequin.setPersistent(true);
//				mannequin.setNoPhysics(false);
//				mannequin.setInvisible(false);
//				mannequin.setImmovable(true);
//
//				UUID uuid = getCurrentAvatar().getUuid();
//				OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
//				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
//				mannequin.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
//				String elementSymbol = getElementSymbol(uuid, getCurrentAvatar());
//				Location above = mannequin.getLocation();
//				above.add(0, 2, 0);
//
//				String name = ChatUtils.translateToColor(elementSymbol + " &r" + aranarthPlayer.getNickname() + " " + elementSymbol);
//
//				// Always will create a new text display
//				mannequin.getWorld().spawn(above, TextDisplay.class, displayEntity -> {
//					displayEntity.setText(name);
//					displayEntity.setBillboard(Display.Billboard.VERTICAL); // Pivots only around the vertical axis
//				});
//			}
//		}
//	}

//	/**
//	 * Cycles all avatar mannequins down the line when an avatar dies.
//	 */
//	private static void shiftAvatarMannequins() {
//		Location center = new Location(Bukkit.getWorld("spawn"), 8, 107, 19);
//		for (Entity entity : center.getNearbyEntities(30, 5, 30)) {
//			if (entity instanceof Mannequin mannequin) {
//
//
//				int avatarIndex = mannequin.getPersistentDataContainer().get(AVATAR_INDEX, PersistentDataType.INTEGER);
//
//				Location above = mannequin.getLocation();
//				above.add(0, 2, 0);
//
//				// Skip the last one as it will simply be overridden
//				if (avatarIndex == 40) {
//					continue;
//				}
//				// Move all avatars over by 1
//				else {
//					// Ensures only the last 40 avatars will be shown
//					// Skips the last one as well as it is null
//					int overallIndex = (avatars.size() - 2) - avatarIndex;
//
//					if (avatars.get(overallIndex + 1) != null) {
//						UUID uuid = avatars.get(overallIndex + 1).getUuid();
//						OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
//						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
//						mannequin.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
//						String elementSymbol = getElementSymbol(uuid, avatars.get(overallIndex + 1));
//
//						// Identifies if a text display already exists
//						TextDisplay textDisplay = null;
//						for (Entity nearby : above.getNearbyEntities(1, 1, 1)) {
//							if (nearby instanceof TextDisplay displayEntity) {
//								textDisplay = displayEntity;
//								break;
//							}
//						}
//
//						String name = ChatUtils.translateToColor(elementSymbol + " &r" + aranarthPlayer.getNickname() + " " + elementSymbol);
//
//						// Creating a new text display
//						if (textDisplay == null) {
//							mannequin.getWorld().spawn(above, TextDisplay.class, displayEntity -> {
//								displayEntity.setText(name);
//								displayEntity.setBillboard(Display.Billboard.VERTICAL); // Pivots only around the vertical axis
//							});
//						}
//						// Updating the existing text display
//						else {
//							textDisplay.setText(name);
//						}
//					}
//				}
//			}
//		}
//		// Despawns the mannequins and then respawns them with the updated values
//		despawnAvatars();
//		respawnAvatars();
//	}

	/**
	 * Despawns all the Avatar mannequins.
	 */
	private static void despawnAvatarMannequins() {
		Location center = new Location(Bukkit.getWorld("spawn"), 8, 107, 19);
		for (Entity entity : center.getNearbyEntities(30, 30, 30)) {
			if (entity instanceof Mannequin mannequin) {
				for (Entity nearby : mannequin.getLocation().getNearbyEntities(2.5, 2.5, 2.5)) {
					// Removes the nametag first
					if (nearby instanceof TextDisplay display) {
						display.remove();
					}
				}
				mannequin.remove();
			}
		}
	}

	/**
	 * Despawns all the Avatar mannequins.
	 */
	private static void respawnAvatarMannequins() {
		if (avatars.isEmpty()) {
			Bukkit.getLogger().info("There are no avatars");
			return;
		}

		World spawn = Bukkit.getWorld("spawn");
		int mannequinIndex = spawnAvatar(new Location(spawn, 16.5, 102, 13.5, 45, 0), (avatars.size() - 1));
		mannequinIndex = spawnAvatar(new Location(spawn, 18.5, 103, 15.5, 90, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 18.5, 103, 18.5, 90, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 18.5, 103, 21.5, 120, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 16.5, 103, 24.5, 135, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 13.5, 104, 25.5, 180, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 10.5, 104, 24.5, 180, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 7.5, 104, 23.5, 180, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 4.5, 105, 25.5, 180, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 1.5, 105, 24.5, 180, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, -1.5, 105, 23.5, -135, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, -2.5, 105, 20.5, -90, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, -0.5, 106, 17.5, -90, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 1.5, 106, 14.5, -25, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 4.5, 106, 13.5, 0, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 7.5, 107, 12.5, 0, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 10.5, 107, 12.5, 0, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 13.5, 107, 12.5, 0, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 16.5, 108, 12.5, 0, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 19.5, 108, 14.5, 45, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 21.5, 108, 17.5, 90, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 21.5, 109, 20.5, 90, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 20.5, 109, 23.5, 150, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 18.5, 109, 26.5, 135, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 15.5, 110, 27.5, 180, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 12.5, 110, 27.5, 180, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 9.5, 110, 26.5, 180, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 6.5, 110, 25.5, 180, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 3.5, 111, 25.5, 180, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 0.5, 111, 24.5, 180, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, -2.5, 111, 23.5, -135, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, -3.5, 111, 20.5, -90, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, -3.5, 112, 17.5, -90, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, -1.5, 112, 14.5, -45, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 1.5, 112, 12.5, 0, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 4.5, 113, 10.5, 0, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 7.5, 113, 9.5, 0, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 10.5, 113, 10.5, 0, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 13.5, 114, 11.5, 0, 0), mannequinIndex);
		mannequinIndex = spawnAvatar(new Location(spawn, 16.5, 114, 12.5, 90, 0), mannequinIndex);
	}

	/**
	 * Spawns an individual avatar.
	 * @param loc The Location of the mannequin.
	 * @param mannequinIndex The index of the mannequin being iterated.
	 * @return The next mannequin index to be used.
	 */
	private static int spawnAvatar(Location loc, int mannequinIndex) {
		// Catches and prevents further spawning if there are not enough avatars
		if (mannequinIndex == -1) {
			return -1;
		}

		// If there is currently no avatar, skip to the next mannequin
		if (avatars.get(mannequinIndex) == null) {
			return mannequinIndex - 1;
		}


		UUID uuid = avatars.get(mannequinIndex).getUuid();
		OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
		AranarthCore plugin = AranarthCore.getInstance();

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			// Fetch and complete the profile (blocking Mojang API call)
			PlayerProfile profile = plugin.getServer().createProfile(uuid);
			boolean completed = profile.complete(true); // Fetches the skin from Mojang

			if (!completed) {
				// Mojang servers unreachable or UUID is somehow incorrect
				return;
			}

			// Jump back to main thread to spawn the mannequin
			Bukkit.getScheduler().runTask(plugin, () -> {
				World world = Bukkit.getWorld("spawn");

				Mannequin mannequin = (Mannequin) world.spawnEntity(loc, EntityType.MANNEQUIN);
				mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
				mannequin.setInvulnerable(true);
				mannequin.setGravity(false);
				mannequin.setPersistent(true);
				mannequin.setNoPhysics(false);
				mannequin.setImmovable(true);

				if (mannequin != null) {
					Location above = mannequin.getLocation().clone().add(0, 2, 0);
					String elementSymbol = getElementSymbol(uuid, avatars.get(mannequinIndex));
					String name = ChatUtils.translateToColor(  elementSymbol + " &r"
							+ AranarthUtils.getPlayer(uuid).getNickname() + " " + elementSymbol);

					// Always will create a new text display
					mannequin.getWorld().spawn(above, TextDisplay.class, displayEntity -> {
						displayEntity.setText(name);
						displayEntity.setBillboard(Display.Billboard.VERTICAL); // Pivots only around the vertical axis
					});
				}
			});
		});
		return mannequinIndex - 1;
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
