package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.enums.SpecialDay;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkTabManager;
import com.aearost.aranarthcore.network.PendingTeleport;
import com.aearost.aranarthcore.items.HoneyGlazedHam;
import com.aearost.aranarthcore.items.Quiver;
import com.aearost.aranarthcore.items.arrow.ArrowIron;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.*;
import com.aearost.aranarthcore.utils.ItemUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.event.BendingPlayerLoadEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Adds a new entry to the players HashMap if the player is not being tracked.
 * Additionally, customizes the join/leave server message format.
 */
public class PlayerServerJoinListener implements Listener {

	public PlayerServerJoinListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(final PlayerJoinEvent e) {
		Player player = e.getPlayer();

		// Clear any stale BendingPlayer data PK may have cached from a previous session on
		// this server. Without this, players who switch to SMP, change element, then return
		// here would still see their old element because PK skips the DB load when the player
		// is already present in its cache. Clearing here forces PK's own join listener (which
		// fires at NORMAL priority, after this) to do a fresh MySQL read.
		BendingPlayer.getPlayers().remove(player.getUniqueId());
		BendingPlayer.getOfflinePlayers().remove(player.getUniqueId());

		// Load the player's last logout location for login routing and position restoration.
		// Done synchronously here (matching the getPendingTeleport pattern below) so the result
		// is available immediately and can be captured by the delayed-task lambda.
		final DatabaseManager.LastLocation lastLoc = DatabaseManager.isActive()
				? DatabaseManager.getInstance().loadLastLocation(player.getUniqueId())
				: null;

		// Detect cross-server transfers: either a pending TP from a normal cross-server action,
		// or the player logged off on a different server and needs to be routed there on login.
		String thisServerName = NetworkManager.isActive() ? NetworkManager.getInstance().getThisServer() : null;
		boolean needsServerRouting = lastLoc != null && thisServerName != null && !lastLoc.server.equals(thisServerName);
		boolean isCrossServerTransfer = needsServerRouting || (NetworkManager.isActive()
				&& NetworkManager.getInstance().getPendingTeleport(player.getUniqueId()) != null);

		// If the player was offline when the resource world was reset, teleport them to spawn
		long resetTime = AranarthUtils.getLastResourceWorldResetTime();
		if (resetTime > 0 && player.getWorld().getName().startsWith("resource")
				&& player.getLastPlayed() < resetTime) {
			player.teleport(new Location(Bukkit.getWorld("spawn"), 0.5, 101, 0.5, 180, 0));
			player.sendMessage(ChatUtils.chatMessage("&7You were in the resource world when it was reset, so you have been sent to &eSpawn&7."));
		}

		boolean isNewPlayer = false;
		if (!AranarthUtils.hasPlayedBefore(player)) {
			AranarthUtils.addPlayer(player.getUniqueId(), new AranarthPlayer(player.getName()));
			LocalDateTime now = LocalDateTime.now();
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			aranarthPlayer.setFirstJoinDate(now.getMonthValue() + "/" + now.getDayOfMonth() + "/" + now.getYear());
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			player.teleport(new Location(Bukkit.getWorld("spawn"), 0.5, 101, 0.5, 180, 0));
			isNewPlayer = true;
		}
		// If the player changed their username
		else if (AranarthUtils.getUsername(player) != null && !AranarthUtils.getUsername(player).equals(player.getName())) {
			AranarthUtils.setUsername(player);
		}

		if (AvatarUtils.getCurrentAvatar() != null) {
			// Called to bind the Avatar's abilities to prevent loss of avatar abilities
			if (AvatarUtils.getCurrentAvatar().getUuid().equals(player.getUniqueId())) {
				PersistenceUtils.loadAvatarBinds();

				// Adds a 2-second delay
				new BukkitRunnable() {
					@Override
					public void run() {
						BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
						if (bendingPlayer != null) {
							player.performCommand("b board");
							player.performCommand("b board");
						}
					}
				}.runTaskLater(AranarthCore.getInstance(), 30);
			}
		}

		// Update conquest/rebellion activity tracking for this player's dominion
		Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		if (playerDominion != null) {
			// Defender logging on during an active conquest, reset their inactivity clock
			if (playerDominion.getConqueredRequest() != null) {
				playerDominion.setConqueredRequestDefenderLastSeen(System.currentTimeMillis());
				DominionUtils.updateDominion(playerDominion);
			}
			// Conqueror logging on during an active rebellion, reset their inactivity clock
			if (playerDominion.getRebelRequest() != null) {
				playerDominion.setRebelRequestConquerorLastSeen(System.currentTimeMillis());
				DominionUtils.updateDominion(playerDominion);
			}
		}

		// Announce this player to the network and check for a pending cross-server teleport.
		// Done early so the pending TP can fire after the player is fully initialised.
		if (NetworkManager.isActive()) {
			// evaluatePlayerPermissions hasn't run yet but AranarthPlayer data is loaded,
			// so publish what we know now; the roster entry will be accurate enough for tab/chat.
			AranarthPlayer apForNetwork = AranarthUtils.getPlayer(player.getUniqueId());
			NetworkManager.getInstance().publishPlayerJoin(player.getUniqueId(), apForNetwork);
		}

		// Permissions must be applied before nickname check is done
		PermissionUtils.evaluatePlayerPermissions(player);

		// Clears a player's nickname if they do not have permission for one
		if (!player.hasPermission("aranarth.nick")) {
			if (!AranarthUtils.getNickname(player).equals(player.getName())) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				aranarthPlayer.setNickname("");
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				player.sendMessage(ChatUtils.chatMessage("&7Your nickname has been cleared"));
			}
		}

		if (isCrossServerTransfer) {
			e.setJoinMessage(null);
		} else {
			if (!isNewPlayer) {
				displayMotd(player);
			}
			DateUtils dateUtils = new DateUtils();
			String nameToDisplay;

			if (!AranarthUtils.getNickname(player).isEmpty()) {
				nameToDisplay = "&7" + AranarthUtils.getNickname(player);
			} else {
				nameToDisplay = "&7" + AranarthUtils.getUsername(player);
			}

			if (dateUtils.isValentinesDay()) {
				e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.VALENTINES)));
			} else if (dateUtils.isEaster()) {
				e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.EASTER)));
			} else if (dateUtils.isHalloween()) {
				e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.HALLOWEEN)));
			} else if (dateUtils.isChristmas()) {
				e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.CHRISTMAS)));
			} else {
				e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + nameToDisplay));
			}
		}

		boolean finalIsNewPlayer = isNewPlayer;
		new BukkitRunnable() {
			@Override
			public void run() {
				// Inject existing remote-server players into this player's tab list.
				NetworkTabManager.syncAllToPlayer(player);

				// Execute any pending cross-server teleport (e.g. player transferred here from SMP
				// to complete a /tp or /tpaccept, or returning from SMP via /survival).
				boolean hadPendingTp = false;
				if (NetworkManager.isActive()) {
					PendingTeleport pending = NetworkManager.getInstance().getPendingTeleport(player.getUniqueId());
					if (pending != null) {
						hadPendingTp = true;
						NetworkManager.getInstance().clearPendingTeleport(player.getUniqueId());

						// If this transfer requires an inventory swap, reload player data from
						// the source server (which saved it to MySQL just before transferring)
						// and apply the stored survival inventory.
						if (pending.isApplyInventory()) {
							PersistenceUtils.reloadPlayerFromDatabase(player.getUniqueId());
							PersistenceUtils.loadPlayerTogglesFromDatabase(player.getUniqueId());
							AranarthPlayer apInv = AranarthUtils.getPlayer(player.getUniqueId());
							if (apInv != null && !apInv.getSurvivalInventory().isEmpty()) {
								try {
									player.getInventory().setContents(
											ItemUtils.itemStackArrayFromBase64(apInv.getSurvivalInventory()));
								} catch (Exception ignored) {
									player.getInventory().clear();
								}
							} else {
								player.getInventory().clear();
							}
							if (apInv != null && !apInv.getSurvivalEnderChest().isEmpty()) {
								try {
									player.getEnderChest().setContents(
											ItemUtils.itemStackArrayFromBase64(apInv.getSurvivalEnderChest()));
								} catch (Exception ignored) {
									player.getEnderChest().clear();
								}
							}
							if (apInv != null) {
								player.setHealth(Math.min(apInv.getSurvivalHealth(), player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
								player.setFoodLevel(apInv.getSurvivalFoodLevel());
								player.setSaturation(apInv.getSurvivalSaturation());
								player.setLevel(apInv.getSurvivalExpLevel());
								player.setExp(apInv.getSurvivalExpProgress());
							}
							player.setGameMode(GameMode.SURVIVAL);
						}

						if ("player".equals(pending.getType())) {
							// TP to wherever the named player currently is
							Player target = Bukkit.getPlayer(java.util.UUID.fromString(pending.getTargetUuid()));
							if (target != null) {
								AranarthUtils.teleportPlayer(player, player.getLocation(), target.getLocation(),
										true, pending.getTitleMain(), pending.getTitleSub(), success -> {});
							}
						} else if ("command".equals(pending.getType())) {
							// Dispatch a command as if the player ran it on this server.
							// Force admin mode so the command skips its own countdown
							// (the countdown already happened on the source server).
							AranarthPlayer apCmd = AranarthUtils.getPlayer(player.getUniqueId());
							boolean wasAdmin = apCmd.isInAdminMode();
							apCmd.setInAdminMode(true);
							AranarthUtils.setPlayer(player.getUniqueId(), apCmd);
							Bukkit.dispatchCommand(player, pending.getCommand());
							AranarthPlayer apCmdAfter = AranarthUtils.getPlayer(player.getUniqueId());
							apCmdAfter.setInAdminMode(wasAdmin);
							AranarthUtils.setPlayer(player.getUniqueId(), apCmdAfter);
						} else {
							// TP to the stored coordinates
							org.bukkit.World w = Bukkit.getWorld(pending.getWorld());
							if (w != null) {
								org.bukkit.Location dest = new org.bukkit.Location(w,
										pending.getX(), pending.getY(), pending.getZ(),
										pending.getYaw(), pending.getPitch());
								AranarthUtils.teleportPlayer(player, player.getLocation(), dest,
										true, pending.getTitleMain(), pending.getTitleSub(), success -> {});
							}
						}
					}
				}

				// When arriving via cross-server transfer, force other clients to reload this
				// player's skin and tab entry by briefly hiding then re-showing them.
				if (hadPendingTp) {
					new BukkitRunnable() {
						@Override
						public void run() {
							if (!player.isOnline()) return;
							for (Player other : Bukkit.getOnlinePlayers()) {
								if (!other.getUniqueId().equals(player.getUniqueId())) {
									other.hidePlayer(AranarthCore.getInstance(), player);
									other.showPlayer(AranarthCore.getInstance(), player);
								}
							}
							// Also force a tab update for the joining player to see themselves
							AranarthUtils.updateTab();
						}
					}.runTaskLater(AranarthCore.getInstance(), 20L);
				}

				// Restore the player to where they last logged out.
				// If they logged off on this server: teleport them to the saved position.
				// If they logged off on another server: route them there via a pending teleport,
				// which the receiving server will execute on arrival.
				if (!hadPendingTp && lastLoc != null && NetworkManager.isActive()) {
					if (lastLoc.server.equals(NetworkManager.getInstance().getThisServer())) {
						org.bukkit.World w = Bukkit.getWorld(lastLoc.world);
						if (w != null) {
							org.bukkit.Location dest = new org.bukkit.Location(
									w, lastLoc.x, lastLoc.y, lastLoc.z, lastLoc.yaw, lastLoc.pitch);
							new BukkitRunnable() {
								@Override
								public void run() {
									if (player.isOnline()) player.teleport(dest);
								}
							}.runTaskLater(AranarthCore.getInstance(), 2L);
						}
					} else {
						PendingTeleport pt = new PendingTeleport(
								lastLoc.world, lastLoc.x, lastLoc.y, lastLoc.z, lastLoc.yaw, lastLoc.pitch, "", "");
						String velocityTarget = AranarthCore.getInstance().getConfig()
								.getString("network.servers." + lastLoc.server, lastLoc.server);
						NetworkManager.getInstance().setPendingAndTransfer(player, velocityTarget, pt);
					}
				}

				// Displays a welcome message after the join message
				if (finalIsNewPlayer) {
					Bukkit.broadcastMessage("");
					Bukkit.broadcastMessage(ChatUtils.translateToColor("                &6&l-------------------------"));
					Bukkit.broadcastMessage(ChatUtils.translateToColor("                     &7Welcome, &e" + player.getName() + ","
							+ "\n                    &7to the &6&lRealm of Aranarth!"));
					Bukkit.broadcastMessage(ChatUtils.translateToColor("                &6&l-------------------------"));
					Bukkit.broadcastMessage("");
					// Broadcast welcome to other servers in the network
					if (NetworkManager.isActive()) {
						NetworkManager.getInstance().publishChat("", "                &6&l-------------------------");
						NetworkManager.getInstance().publishChat("", "                     &7Welcome, &e" + player.getName() + "," +
								"\n                    &7to the &6&lRealm of Aranarth!");
						NetworkManager.getInstance().publishChat("", "                &6&l-------------------------");
					}

					player.sendMessage(ChatUtils.chatMessage("&7Be sure to read the &e/rules &7and check out &e/warp Tutorial"));

					// Give starter kit
					PlayerInventory inv = player.getInventory();
					inv.setItem(0, new ItemStack(Material.STONE_SWORD));
					inv.setItem(1, new ItemStack(Material.STONE_PICKAXE));
					inv.setItem(2, new ItemStack(Material.STONE_AXE));
					inv.setItem(3, new ItemStack(Material.STONE_SHOVEL));
					ItemStack honeyGlazedHam = new HoneyGlazedHam().getItem();
					honeyGlazedHam.setAmount(16);
					inv.setItem(4, honeyGlazedHam);
					inv.setItem(5, new ItemStack(Material.BUNDLE));
					ItemStack ironArrows = new ArrowIron().getItem();
					ironArrows.setAmount(16);
					inv.setItem(6, ironArrows);
					inv.setItem(7, new Quiver().getItem());
					inv.setItem(8, new ItemStack(Material.BOW));

					// Equip leather armor
					inv.setHelmet(new ItemStack(Material.LEATHER_HELMET));
					inv.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
					inv.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
					inv.setBoots(new ItemStack(Material.LEATHER_BOOTS));
				}
			}
		}.runTaskLater(AranarthCore.getInstance(), 1L);

		if (finalIsNewPlayer) {
			for (Player online : Bukkit.getOnlinePlayers()) {
				online.playSound(online, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1F, 0.8F);
			}
		} else if (!isCrossServerTransfer) {
			playJoinSound();
		}
		AranarthUtils.updateTab();
	}

	/**
	 * Re-evaluates bending sub-elements once ProjectKorra has finished loading
	 * the player's BendingPlayer object. This handles the race condition where
	 * evaluatePlayerPermissions runs before the BendingPlayer is available in
	 * ONLINE_PLAYERS (either because AranarthCore's listener fires before
	 * PKListener's, or because the data was loaded asynchronously from the DB).
	 */
	@EventHandler
	public void onBendingPlayerLoad(BendingPlayerLoadEvent e) {
		if (!e.isOnline()) {
			return;
		}
		BendingPlayer bendingPlayer = (BendingPlayer) e.getBendingPlayer();
		Player player = bendingPlayer.getPlayer();
		if (player == null || !player.isOnline()) {
			return;
		}
		PermissionUtils.updateSubElements(player);
	}

	/**
	 * Displays the MOTD to the player when they join.
	 * @param player The player.
	 */
	public static void displayMotd(Player player) {
		// Displays the MOTD when the player joins
		int day = AranarthUtils.getDay();
		String weekday = DateUtils.provideWeekdayName(AranarthUtils.getWeekday());
		String month = DateUtils.provideMonthName(AranarthUtils.getMonth());
		int year = AranarthUtils.getYear();

		String[] messages = DateUtils.determineServerDate(day, weekday, month, year);
		player.sendMessage(ChatUtils.translateToColor("&6&l-------------------------------------"));

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		List<String> toggling = new ArrayList<>();
		if (aranarthPlayer.isTogglingChat()) {
			toggling.add("&e&oChat Messages");
		}
		if (aranarthPlayer.isTogglingMessages()) {
			toggling.add("&e&oDirect Messages");
		}
		if (aranarthPlayer.isTogglingTp()) {
			toggling.add("&e&oTeleport Requests");
		}
		if (!aranarthPlayer.isUsingSpawnBoost()) {
			toggling.add("&e&oSpawn Boost");
		}
		if (aranarthPlayer.isTogglingChangeClaim()) {
			toggling.add("&e&oClaim Changes");
		}
		if (aranarthPlayer.isTogglingInventoryAssist()) {
			toggling.add("&e&oInventory Assist");
		}
		if (!aranarthPlayer.isAddingToShulker()) {
			toggling.add("&e&oShulker Assist");
		}
		if (aranarthPlayer.getBlacklistingMethod() == -1) {
			toggling.add("&e&oBlacklist");
		}
		if (!aranarthPlayer.isCompressingItems()) {
			toggling.add("&e&oCompressor");
		}
		if (!aranarthPlayer.isAutoLockingChests()) {
			toggling.add("&e&oChest Locks");
		}
		if (aranarthPlayer.hasBlueFireDisabled()) {
			toggling.add("&e&oBlue Fire");
		}
		if (aranarthPlayer.isDayMessageDisabled()) {
			toggling.add("&e&oDay Message");
		}
		if (aranarthPlayer.isWeatherMessageDisabled()) {
			toggling.add("&e&oWeather Message");
		}
		if (aranarthPlayer.isBulkSellShulkerEnabled()) {
			toggling.add("&e&oBulk Sell Shulker");
		}

		if (!toggling.isEmpty()) {
			String toggledFeatures = "  &7&oYou currently have ";
			for (int i = 0; i < toggling.size(); i++) {
				toggledFeatures += toggling.get(i);
				if  (i < toggling.size() - 2) {
					toggledFeatures += "&7&o, ";
				} else if (i < toggling.size() - 1) {
					toggledFeatures += " &7&oand ";
				}
			}
			toggledFeatures += " &7&otoggled";
			player.sendMessage(ChatUtils.translateToColor(toggledFeatures));
		}

		Avatar avatar = AvatarUtils.getCurrentAvatar();
		if (avatar == null) {
			player.sendMessage(ChatUtils.translateToColor("  &7&oAranarth is currently without an Avatar..."));
		} else {
			String avatarNickname = AranarthUtils.getPlayer(avatar.getUuid()).getNickname();
			String element = "";
			if (avatar.getElement() == 'W') {
				element = "&b水";
			} else if (avatar.getElement() == 'E') {
				element = "&a土";
			} else if (avatar.getElement() == 'F') {
				element = "&c火";
			} else {
				element = "&7気";
			}
			player.sendMessage(ChatUtils.translateToColor(
					"  &5&lThe current Avatar is " + element + " &d" + avatarNickname + " " + element));
		}

		NumberFormat formatter = NumberFormat.getCurrencyInstance();

		player.sendMessage(ChatUtils.translateToColor("  &7&oYour balance is currently &6" + formatter.format(aranarthPlayer.getBalance())));
		HashMap<String, String> activeBoosts = AranarthUtils.getActiveServerBoostsMessages();
		if (activeBoosts.isEmpty()) {
			player.sendMessage(ChatUtils.translateToColor("  &7There are currently no active server boosts"));
		} else {
			for (String boost : activeBoosts.keySet()) {
				player.sendMessage(ChatUtils.translateToColor("  " + boost + " &7is active for &e" + activeBoosts.get(boost)));
			}
		}

		player.sendMessage("  " + messages[1]); // Date message

		int mailCount = MailUtils.getMail(player.getUniqueId()).size();
		if (mailCount > 0) {
			player.sendMessage(ChatUtils.translateToColor("  &7You have &e" + mailCount + " &7mail message" + (mailCount == 1 ? "" : "s")));
		}

		// Login streak notification
		boolean streakReset = LoginStreakUtils.ensureStreakValid(player.getUniqueId());
		if (streakReset) {
			player.sendMessage(ChatUtils.translateToColor("  &7Your login streak has been reset to Day 1"));
		} else if (LoginStreakUtils.canClaim(player.getUniqueId())) {
			player.sendMessage(ChatUtils.translateToColor("  &7Don't forget to claim your daily login reward with &e/streak"));
		}

		// Pending crate key notification
		UUID uuid = player.getUniqueId();
		int pendingKeys = 0;
		Integer pv = AranarthUtils.getPendingVoteKeys().get(uuid);
		Integer pr = AranarthUtils.getPendingRareKeys().get(uuid);
		Integer pe = AranarthUtils.getPendingEpicKeys().get(uuid);
		Integer pg = AranarthUtils.getPendingGodlyKeys().get(uuid);
		if (pv != null) pendingKeys += pv;
		if (pr != null) pendingKeys += pr;
		if (pe != null) pendingKeys += pe;
		if (pg != null) pendingKeys += pg;
		if (pendingKeys > 0) {
			String keyWord = pendingKeys == 1 ? "key" : "keys";
			player.sendMessage(ChatUtils.translateToColor("  &7You have &e" + pendingKeys + " pending crate " + keyWord + " &7- use &e/keyclaim &7to collect!"));
		}

		player.sendMessage(ChatUtils.translateToColor("&6&l-------------------------------------"));
		player.sendMessage("");

		// Adds all aranarth recipes
		Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
			Recipe recipe = it.next();
			if (recipe instanceof Keyed) {
				NamespacedKey key = ((Keyed) recipe).getKey();
				if (key.getNamespace().equalsIgnoreCase("aranarthcore")) {
					player.discoverRecipe(key);
				}
			}
		}
	}

	/**
	 * Plays a sound effect when a player joins the server.
	 */
	private void playJoinSound() {
		new BukkitRunnable() {
			int runs = 0;
			@Override
			public void run() {
				if (runs == 0) {
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1F);
					}
					runs++;
				} else if (runs == 1){
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1.2F);
					}
					runs++;
				} else {
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1.6F);
					}
					cancel();
				}
			}
		}.runTaskTimer(AranarthCore.getInstance(), 0, 5); // Runs every 5 ticks
	}


}
