package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.objects.*;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.awt.Color;

/**
 * Provides a large variety of utility methods for everything related to land claiming.
 */
public class DominionUtils {

	private static final List<Dominion> dominions = new ArrayList<>();

	private static final Map<UUID, Dominion> dominionById = new HashMap<>();
	private static final Map<String, Dominion> chunkKeyToDominion = new HashMap<>();
	private static final Map<UUID, Dominion> playerToDominion = new HashMap<>();
	public static final long CONQUEST_DEADLINE_MS = 7L * 24 * 60 * 60 * 1000; // 1 week
	public static final long CONQUER_COOLDOWN_MS = 3L * 24 * 60 * 60 * 1000; // 3 days
	public static final long REBEL_DEADLINE_MS = 7L * 24 * 60 * 60 * 1000; // 1 week
	public static final long REBEL_COOLDOWN_MS = 3L * 24 * 60 * 60 * 1000; // 3 days
	public static final long REBEL_INACTIVITY_MS = 3L * 24 * 60 * 60 * 1000; // 3 days
	public static final long CONQUEST_INACTIVITY_MS = 3L * 24 * 60 * 60 * 1000; // 3 days

	/**
	 * Provides the list of Dominions.
	 * @return The list of dominions
	 */
	public static List<Dominion> getDominions() {
		return dominions;
	}

	/**
	 * Provides the Dominion that the player is in.
	 * @param uuid The player's UUID.
	 * @return The Dominion that the player is in.
	 */
	public static Dominion getPlayerDominion(UUID uuid) {
		return playerToDominion.get(uuid);
	}

	/**
	 * Provides the Dominion with the given stable ID.
	 * @param id The dominion's stable UUID.
	 * @return The Dominion, or null if not found.
	 */
	public static Dominion getDominionById(UUID id) {
		return dominionById.get(id);
	}

	/**
	 * Generates the chunk lookup key for the given chunk.
	 * @param chunk The chunk.
	 * @return The lookup key string.
	 */
	private static String getChunkKey(Chunk chunk) {
		return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
	}

	/**
	 * Checks whether a player has a specific permission within the given dominion.
	 * Considers the player's rank if they are a member, or the inter-dominion relation if they are an outsider.
	 * @param player The player to check.
	 * @param dominion The dominion to check permissions in.
	 * @param permission The permission to verify.
	 * @return True if the player has the permission.
	 */
	public static boolean hasPermission(Player player, Dominion dominion, DominionPermission permission) {
		// Player-specific overrides take precedence over all rank/relation permissions
		Boolean playerOverride = dominion.getPlayerPermissionOverride(player.getUniqueId(), permission);
		if (playerOverride != null) {
			return playerOverride;
		}

		Dominion playerDominion = getPlayerDominion(player.getUniqueId());
		if (playerDominion != null && playerDominion.isSameDominion(dominion)) {
			DominionRank rank = dominion.getMemberRank(player.getUniqueId());
			if (rank == null) {
				rank = DominionRank.NEWCOMER;
			}
			return dominion.getDominionPermissions().hasPermission(rank, permission);
		}
		// Conqueror bypass build permission for the leader and lieutenants
		if (permission == DominionPermission.BUILD
				&& playerDominion != null
				&& playerDominion.getConquered().contains(dominion.getLeader())) {
			DominionRank playerRank = playerDominion.getMemberRank(player.getUniqueId());
			if (playerRank == DominionRank.LEADER || playerRank == DominionRank.LIEUTENANT) {
				return true;
			}
		}
		DominionRank relation = getRelationKey(playerDominion, dominion);
		return dominion.getDominionPermissions().hasPermission(relation, permission);
	}

	/**
	 * Determines the relation rank between two dominions for the purposes of permission lookups.
	 * @param playerDominion The dominion of the acting player (may be null).
	 * @param targetDominion The dominion being acted upon.
	 * @return The relation rank: ALLIED, TRUCED, ENEMIED, NEUTRAL, or WANDERER.
	 */
	public static DominionRank getRelationKey(Dominion playerDominion, Dominion targetDominion) {
		if (playerDominion == null) {
			return DominionRank.WANDERER;
		}
		if (playerDominion.isAllied(targetDominion)) {
			return DominionRank.ALLIED;
		}
		if (playerDominion.isTruced(targetDominion)) {
			return DominionRank.TRUCED;
		}
		if (playerDominion.isEnemied(targetDominion)) {
			return DominionRank.ENEMIED;
		}
		return DominionRank.NEUTRAL;
	}

	/**
	 * Creates a new dominion.
	 * @param dominion The dominion being added.
	 */
	public static void createDominion(Dominion dominion) {
		dominions.add(dominion);
		dominionById.put(dominion.getId(), dominion);
		for (UUID memberUuid : dominion.getMembers()) {
			playerToDominion.put(memberUuid, dominion);
		}
		for (Chunk chunk : dominion.getChunks()) {
			chunkKeyToDominion.put(getChunkKey(chunk), dominion);
		}
	}

	/**
	 * Removes a player from the playerToDominion lookup map.
	 * Use this when removing a member from a dominion before calling updateDominion,
	 * since updateDominion uses the same object reference and can't detect the removal.
	 * @param uuid The player's UUID.
	 */
	public static void removePlayerFromDominion(UUID uuid) {
		playerToDominion.remove(uuid);
	}

	/**
	 * Updates an existing dominion with new values, keeping all lookup maps in sync.
	 * @param dominion The updated dominion.
	 */
	public static void updateDominion(Dominion dominion) {
		int i = 0;
		while (i < dominions.size()) {
			if (dominions.get(i).isSameDominion(dominion)) {
				break;
			}
			i++;
		}
		if (i < dominions.size()) {
			Dominion old = dominions.get(i);

			// Remove old member entries
			for (UUID memberUuid : old.getMembers()) {
				playerToDominion.remove(memberUuid);
			}
			// Remove old chunk entries
			for (Chunk chunk : old.getChunks()) {
				chunkKeyToDominion.remove(getChunkKey(chunk));
			}

			dominions.set(i, dominion);
		} else {
			dominions.add(dominion);
		}

		// Re-populate maps with updated dominion
		dominionById.put(dominion.getId(), dominion);
		for (UUID memberUuid : dominion.getMembers()) {
			playerToDominion.put(memberUuid, dominion);
		}
		for (Chunk chunk : dominion.getChunks()) {
			chunkKeyToDominion.put(getChunkKey(chunk), dominion);
		}
	}

	/**
	 * Claims the chunk for the dominion or one of its outposts.
	 *
	 * @param player The player attempting to claim the chunk.
	 * @param chunkToClaim The chunk the player is attempting to claim.
	 * @return A chat-formatted message describing the outcome.
	 */
	public static String claimChunk(Player player, Chunk chunkToClaim) {
		// Reject if already claimed
		Dominion dominionOfChunk = getDominionOfChunk(chunkToClaim);
		if (dominionOfChunk != null) {
			Dominion playerDominion = getPlayerDominion(player.getUniqueId());
			if (playerDominion != null && playerDominion.isSameDominion(dominionOfChunk)) {
				return "&cThis chunk is already claimed by your dominion";
			}
			return "&cThis chunk is already claimed by &e" + dominionOfChunk.getName();
		}
		Outpost outpostOfChunk = OutpostUtils.getOutpostOfChunk(chunkToClaim);
		if (outpostOfChunk != null) {
			Dominion playerDominion = getPlayerDominion(player.getUniqueId());
			if (playerDominion != null && outpostOfChunk.getDominionId().equals(playerDominion.getId())) {
				return "&cThis chunk is already claimed by your outpost &e" + outpostOfChunk.getName();
			}
			return "&cThis chunk is already claimed by another dominion's outpost";
		}

		Dominion playerDominion = getPlayerDominion(player.getUniqueId());
		if (playerDominion == null) {
			return "&cYou are not part of a dominion!";
		}
		if (!playerDominion.getLeader().equals(player.getUniqueId())
				&& playerDominion.getMemberRank(player.getUniqueId()) != DominionRank.LIEUTENANT) {
			return "&cOnly the Leader or Lieutenant can claim land!";
		}
		if (playerDominion.getConqueredRequest() != null) {
			return "&cYou cannot claim chunks while your Dominion is under active conquest!";
		}

		int claimPrice = 250;

		// Adjacent to main dominion
		if (isConnectedToClaims(playerDominion.getChunks(), chunkToClaim)) {
			if (playerDominion.getBalance() < claimPrice) {
				return "&cYour dominion cannot afford this!";
			}
			if (playerDominion.getChunks().size() >= playerDominion.getMaxChunks()) {
				return "&cYou cannot claim more than &e" + playerDominion.getMaxChunks() + " chunks!";
			}
			playerDominion.setBalance(playerDominion.getBalance() - claimPrice);
			playerDominion.getChunks().add(chunkToClaim);
			chunkKeyToDominion.put(getChunkKey(chunkToClaim), playerDominion);
			resizeFoodArray(playerDominion);
			updateDominion(playerDominion);
			DominionLevelUtils.reevaluateDominion(playerDominion);
			return "&e" + playerDominion.getName() + " &7has claimed &e"
					+ playerDominion.getChunks().size() + "/" + playerDominion.getMaxChunks() + " chunks";
		}

		// Adjacent to an outpost (lowest index first)
		List<Outpost> outposts = OutpostUtils.getDominionOutposts(playerDominion.getId());
		for (Outpost outpost : outposts) {
			if (OutpostUtils.isConnectedToOutpostClaims(outpost.getChunks(), chunkToClaim)) {
				return OutpostUtils.claimOutpostChunk(player, outpost, playerDominion, chunkToClaim);
			}
		}

		return "&cThis chunk is not connected to your Dominion or an outpost";
	}

	/**
	 * Provides the dominion that owns the chunk.
	 * @param chunk The chunk being verified.
	 * @return The dominion that owns the chunk.
	 */
	public static Dominion getDominionOfChunk(Chunk chunk) {
		return chunkKeyToDominion.get(getChunkKey(chunk));
	}

	/**
	 * Returns the dominion that owns the given chunk, checking both main dominion
	 * claims and outpost claims. Returns null if the chunk is unclaimed.
	 */
	public static Dominion getDominionOfChunkAnywhere(Chunk chunk) {
		Dominion dominion = getDominionOfChunk(chunk);
		if (dominion != null) {
			return dominion;
		}
		Outpost outpost = OutpostUtils.getOutpostOfChunk(chunk);
		if (outpost != null) {
			return getDominionById(outpost.getDominionId());
		}
		return null;
	}

	/**
	 * Unclaims the chunk that the user is standing in.
	 * If the chunk belongs to an outpost, delegates to {@link OutpostUtils#unclaimOutpostChunk}.
	 * @param player The player that is attempting to unclaim the chunk.
	 * @return The message of whether the chunk was unclaimed.
	 */
	public static String unclaimChunk(Player player) {
		Chunk chunk = player.getLocation().getChunk();

		Outpost outpostOfChunk = OutpostUtils.getOutpostOfChunk(chunk);
		if (outpostOfChunk != null) {
			Dominion playerDominion = getPlayerDominion(player.getUniqueId());
			if (playerDominion == null) {
				return "&cYou are not part of a Dominion";
			}
			if (!outpostOfChunk.getDominionId().equals(playerDominion.getId())) {
				return "&cThis chunk is claimed by another dominion's outpost!";
			}
			return OutpostUtils.unclaimOutpostChunk(player, outpostOfChunk, playerDominion, chunk);
		}

		// Main dominion chunk
		Dominion dominionOfChunk = getDominionOfChunk(chunk);
		if (dominionOfChunk != null) {
			Dominion playerDominion = getPlayerDominion(player.getUniqueId());
			if (playerDominion != null) {
				if (playerDominion.isSameDominion(dominionOfChunk)) {
					if (playerDominion.getLeader().equals(player.getUniqueId()) || playerDominion.getMemberRank(player.getUniqueId()) == DominionRank.LIEUTENANT) {
						if (playerDominion.getConqueredRequest() != null) {
							return "&cYou cannot unclaim chunks while your Dominion is under active conquest!";
						}
						Chunk homeChunk = playerDominion.getDominionHome().getChunk();
						if (chunk.getX() == homeChunk.getX() && chunk.getZ() == homeChunk.getZ()) {
							return "&cYou cannot unclaim the chunk that the home is in!";
						} else {
							if (isAllClaimsConnectedAfterUnclaiming(dominionOfChunk, chunk)) {
								AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
								aranarthPlayer.setBalance(aranarthPlayer.getBalance() + 125);
								AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
								playerDominion.getChunks().remove(chunk);
								chunkKeyToDominion.remove(getChunkKey(chunk));
								resizeFoodArray(playerDominion);
								updateDominion(playerDominion);
								DominionLevelUtils.reevaluateDominion(playerDominion);
								return "&7This chunk has been unclaimed successfully";
							} else {
								return "&cYou cannot unclaim this chunk as they all must remain connected!";
							}
						}
					} else {
						return "&cOnly the Leader or Lieutenant can unclaim land!";
					}
				} else {
					return "&cThis chunk not claimed by " + dominionOfChunk.getName() + "!";
				}
			} else {
				return "&cYou are not part of a Dominion";
			}
		} else {
			return "&cThis chunk is not claimed!";
		}
	}

	/**
	 * Disbands the input Dominion.
	 * @param dominion The Dominion to be disbanded.
	 */
	public static void disbandDominion(Dominion dominion) {
		Bukkit.broadcastMessage(ChatUtils.chatMessage("&7The Dominion of &e" + dominion.getName() + " &7has been disbanded"));
		DiscordUtils.dominionMessage(dominion, "The Dominion of " + dominion.getName() + " &7has been disbanded", Color.RED);
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(dominion.getLeader());
		aranarthPlayer.setBalance(aranarthPlayer.getBalance() + dominion.getBalance());
		if (Bukkit.getOfflinePlayer(dominion.getLeader()).isOnline()) {
			Bukkit.getPlayer(dominion.getLeader()).sendMessage(ChatUtils.chatMessage("&7Your Dominion's balance has been added to your own"));
		}

		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.5F);
		}

		// Clean up outposts
		for (Outpost outpost : new ArrayList<>(OutpostUtils.getDominionOutposts(dominion.getId()))) {
			OutpostUtils.disbandOutpost(dominion, outpost);
		}

		// Clean up lookup maps
		dominionById.remove(dominion.getId());
		for (UUID memberUuid : dominion.getMembers()) {
			playerToDominion.remove(memberUuid);
		}
		chunkKeyToDominion.values().removeIf(d -> d.isSameDominion(dominion));

		dominions.remove(dominion);
	}

	/**
	 * Determines if the input chunk is connected to the rest of the claims of the dominion.
	 * @param dominionChunks The chunks of the Dominion.
	 * @param chunk The chunk.
	 * @return Confirmation if the input chunk is connected to the rest of the claims of the dominion.
	 */
	public static boolean isConnectedToClaims(List<Chunk> dominionChunks, Chunk chunk) {
		int chunkX = chunk.getX();
		int chunkZ = chunk.getZ();
		for (Chunk dominionChunk : dominionChunks) {
			if (!dominionChunk.getWorld().getName().equals(chunk.getWorld().getName())) {
				continue;
			}

			int differenceX = Math.abs(dominionChunk.getX() - chunkX);
			int differenceZ = Math.abs(dominionChunk.getZ() - chunkZ);

			// Adjacent on one axis, same on the other
			if ((differenceX == 1 && differenceZ == 0) || (differenceX == 0 && differenceZ == 1)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines if unclaiming the input claim would result in an unconnected claim.
	 * @param dominion The dominion.
	 * @param chunkToRemove The chunk attempting to be unclaimed.
	 * @return Confirmation if unclaiming the input claim would result in an unconnected claim.
	 */
	public static boolean isAllClaimsConnectedAfterUnclaiming(Dominion dominion, Chunk chunkToRemove) {
		// Creates a copy of the chunks to see if the claims will remain connected once the chunk is removed
		List<Chunk> remaining = new ArrayList<>(dominion.getChunks());
		remaining.remove(chunkToRemove);

		// Will always be connected
		if (remaining.size() <= 1) {
			return true;
		}

		Set<Chunk> visited = new HashSet<>();
		Deque<Chunk> queue = new ArrayDeque<>();

		// Start from any remaining chunk
		Chunk start = remaining.get(0);
		queue.add(start);
		visited.add(start);

		while (!queue.isEmpty()) {
			Chunk current = queue.poll();

			for (Chunk other : remaining) {
				if (visited.contains(other)) {
					continue;
				}

				if (isSideAdjacent(current, other)) {
					visited.add(other);
					queue.add(other);
				}
			}
		}

		// If we couldn't reach every chunk, the claims split
		return visited.size() == remaining.size();
	}

	/**
	 * Verifies whether the two chunks are side by side.
	 * @param a The first chunk.
	 * @param b The second chunk.
	 * @return Confirmation whether the two chunks are side by side.
	 */
	private static boolean isSideAdjacent(Chunk a, Chunk b) {
		if (!a.getWorld().equals(b.getWorld())) {
			return false;
		}

		// Only one of two can differ
		int dx = Math.abs(a.getX() - b.getX());
		int dz = Math.abs(a.getZ() - b.getZ());

		if (dx + dz == 1) {
			return true;
		}
		return false;
	}

	/**
	 * Determines if the player is a wanderer — not a member of any Dominion.
	 * @param uuid The player's UUID.
	 * @return True if the player has no Dominion.
	 */
	public static boolean isWanderer(UUID uuid) {
		return getPlayerDominion(uuid) == null;
	}

	/**
	 * Updates the leader of the Dominion by updating all references to the Dominion's Leader.
	 * @param dominionBeingUpdated The Dominion that is being updated.
	 * @param newLeader The UUID of the new leader of the Dominion.
	 * @param isDeleting If the Dominion is being deleted.
	 */
	public static void updateDominionLeader(Dominion dominionBeingUpdated, UUID newLeader, boolean isDeleting) {
		UUID oldLeader = dominionBeingUpdated.getLeader();
		for (Dominion dominion : getDominions()) {
			for (int i = 0; i < dominion.getAllianceRequests().size(); i++) {
				if (dominion.getAllianceRequests().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getAllianceRequests().set(i, newLeader);
					} else {
						dominion.getAllianceRequests().remove(oldLeader);
					}
					break;
				}
			}
			for (int i = 0; i < dominion.getTruceRequests().size(); i++) {
				if (dominion.getTruceRequests().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getTruceRequests().set(i, newLeader);
					} else {
						dominion.getTruceRequests().remove(oldLeader);
					}
					break;
				}
			}
			for (int i = 0; i < dominion.getNeutralRequests().size(); i++) {
				if (dominion.getNeutralRequests().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getNeutralRequests().set(i, newLeader);
					} else {
						dominion.getNeutralRequests().remove(oldLeader);
					}
					break;
				}
			}
			for (int i = 0; i < dominion.getAllied().size(); i++) {
				if (dominion.getAllied().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getAllied().set(i, newLeader);
					} else {
						dominion.getAllied().remove(oldLeader);
					}
					break;
				}
			}
			for (int i = 0; i < dominion.getTruced().size(); i++) {
				if (dominion.getTruced().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getTruced().set(i, newLeader);
					} else {
						dominion.getTruced().remove(oldLeader);
					}
					break;
				}
			}
			for (int i = 0; i < dominion.getEnemied().size(); i++) {
				if (dominion.getEnemied().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getEnemied().set(i, newLeader);
					} else {
						dominion.getEnemied().remove(oldLeader);
					}
					break;
				}
			}
			for (int i = 0; i < dominion.getConquered().size(); i++) {
				if (dominion.getConquered().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getConquered().set(i, newLeader);
					} else {
						dominion.getConquered().remove(oldLeader);
					}
					break;
				}
			}
			if (dominion.getConqueredRequest() != null) {
				if (dominion.getConqueredRequest().equals(oldLeader)) {
					if (!isDeleting) {
						dominion.setConqueredRequest(newLeader);
					} else {
						dominion.setConqueredRequest(null);
					}
				}
			}
			if (dominion.getRebelRequest() != null) {
				if (dominion.getRebelRequest().equals(oldLeader)) {
					if (!isDeleting) {
						dominion.setRebelRequest(newLeader);
					} else {
						dominion.setRebelRequest(null);
					}
				}
			}

			updateDominion(dominion);
		}
		if (!isDeleting) {
			// Demote old leader to Lieutenant and promote new leader
			dominionBeingUpdated.setMemberRank(oldLeader, DominionRank.LIEUTENANT);
			dominionBeingUpdated.setMemberRank(newLeader, DominionRank.LEADER);
			dominionBeingUpdated.setLeader(newLeader);
			updateDominion(dominionBeingUpdated);
		} else {
			disbandDominion(dominionBeingUpdated);
		}
	}

	/**
	 * Consumes contents of a Dominion's designated food inventory.
	 */
	public static void reEvaluateFoodInventory() {
		// Close all inventories before evaluating
		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			if (ChatUtils.stripColorFormatting(onlinePlayer.getOpenInventory().getTitle()).endsWith("Food")) {
				onlinePlayer.closeInventory();
			}
		}

		for (Dominion dominion : new ArrayList<>(getDominions())) {
			// Conquered dominions are exempt from daily food/money/land taxes
			if (getConquerorOfDominion(dominion) != null) {
				continue;
			}

			int totalFoodPower = getTotalFoodPower(dominion);

			int powerBeingConsumed = 0;
			// Consume 100 power per day for <=25 chunks
			if (dominion.getChunks().size() <= 25) {
				powerBeingConsumed = 100;
			} else if (dominion.getChunks().size() <= 100) {
				powerBeingConsumed = 250;
			} else {
				powerBeingConsumed = 500;
			}

			if (totalFoodPower >= powerBeingConsumed) {
				consumeFood(dominion, powerBeingConsumed);
				if (Bukkit.getOfflinePlayer(dominion.getLeader()).isOnline()) {
					Player onlineLeader = Bukkit.getPlayer(dominion.getLeader());
					onlineLeader.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + "'s &7daily food rations have been consumed"));
				}
			} else {
				int result = consumeMoneyOrLand(dominion, 250);
				if (result == -1) {
					updateDominionLeader(dominion, null, true);
				} else {
					for (UUID memberUuid : dominion.getMembers()) {
						if (Bukkit.getOfflinePlayer(memberUuid).isOnline()) {
							Player member = Bukkit.getPlayer(memberUuid);
							member.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + " &7did not have enough food in its reserves"));
							// Money was consumed
							if (result == 1) {
								member.sendMessage(ChatUtils.chatMessage("&7Instead, &6$250 &7was consumed &7was sold to pay for the tax"));
							}
							// Land was consumed
							else {
								member.sendMessage(ChatUtils.chatMessage("&7Instead, a chunk was sold to pay for the tax"));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Provides the total food power of the Dominion.
	 * @param dominion The Dominion.
	 * @return The total food power of the Dominion.
	 */
	public static int getTotalFoodPower(Dominion dominion) {
		int totalPower = 0;
		for (ItemStack food : dominion.getFood()) {
			if (food == null || food.getType() == Material.AIR) {
				continue;
			}

			// Determines if there's an increase, or if the Dominion is conquered, a decrease
			int amplifier = dominion.getConquered().size();
			if (amplifier == 0) {
				if (getConquerorOfDominion(dominion) != null) {
					amplifier = -1;
				}
			}
			int powerOfFoodItem = getClaimFoodPower(food.getType(), amplifier);
			totalPower += (powerOfFoodItem * food.getAmount());
		}
		return totalPower;
	}

	/**
	 * Determines the claim power that the input item contains.
	 * @param type The Material of item being provided.
	 * @param amplifier The amplifier to be applied to the item, limited to 5.
	 * @return The claim power that the input item contains.
	 */
	public static int getClaimFoodPower(Material type, int amplifier) {
		int power = 0;
		if (type == Material.ENCHANTED_GOLDEN_APPLE || type == Material.CAKE) {
			power = 500;
		} else if (type == Material.MUSHROOM_STEW || type == Material.BEETROOT_SOUP) {
			power = 150;
		} else if (type == Material.HAY_BLOCK || type == Material.RABBIT_STEW) {
			power = 50;
		} else if (type == Material.GOLDEN_APPLE || type == Material.COOKED_PORKCHOP || type == Material.COOKED_MUTTON
				|| type == Material.COOKED_BEEF || type == Material.COOKED_CHICKEN || type == Material.COOKED_RABBIT
				|| type == Material.COOKED_COD || type == Material.COOKED_SALMON) {
			power = 32;
		} else if (type == Material.PUMPKIN_PIE) {
			power = 25;
		} else if (type == Material.DRIED_KELP_BLOCK) {
			power = 20;
		} else if (type == Material.BREAD || type == Material.APPLE) {
			power = 16;
		} else if (type == Material.GOLDEN_CARROT) {
			power = 10;
		} else if (type == Material.PORKCHOP || type == Material.MUTTON
				|| type == Material.BEEF || type == Material.CHICKEN || type == Material.RABBIT
				|| type == Material.COD || type == Material.SALMON) {
			power = 8;
		} else if (type == Material.POISONOUS_POTATO) {
			power = 6;
		} else if (type == Material.WHEAT || type == Material.BEETROOT) {
			power = 5;
		} else if (type == Material.BAKED_POTATO) {
			power = 3;
		} else if (type == Material.CARROT || type == Material.POTATO || type == Material.DRIED_KELP) {
			power = 2;
		} else if (type == Material.MELON_SLICE || type == Material.SWEET_BERRIES || type == Material.GLOW_BERRIES
				|| type == Material.CHORUS_FRUIT || type == Material.COOKIE) {
			power = 1;
		}

		// Limited to an amplifier of 5
		if (amplifier > 5) {
			amplifier = 5;
		}

		// Provides different yields based on the input
		return switch (amplifier) {
			case -1 -> power = (int) (power * 0.75);
			case 1 -> power = (int) (power * 1.25);
			case 2 -> power = (int) (power * 1.5);
			case 3 -> power = (int) (power * 1.75);
			case 4 -> power = power * 2;
			case 5 -> power = (int) (power * 2.5);
			default -> power;
        };
	}

	/**
	 * Returns the correct food array size for a dominion based on its chunk count.
	 * ≤25 chunks = 18 slots, ≤100 chunks = 36 slots, >100 chunks = 54 slots.
	 */
	public static int getFoodArraySize(Dominion dominion) {
		int chunkCount = dominion.getChunks().size();
		if (chunkCount <= 25) {
			return 18;
		} else if (chunkCount <= 100) {
			return 36;
		} else {
			return 54;
		}
	}

	/**
	 * Removes a specific chunk from a dominion, updates the internal chunk-key index,
	 * resizes the food array, and persists the change.
	 * Used by the level penalty system. The caller is responsible for verifying
	 * connectivity before calling this method.
	 *
	 * @param dominion The dominion to remove the chunk from.
	 * @param chunk    The chunk to remove.
	 */
	public static void removePenaltyChunk(Dominion dominion, Chunk chunk) {
		AranarthUtils.removeLocksInChunk(chunk);
		dominion.getChunks().remove(chunk);
		chunkKeyToDominion.remove(getChunkKey(chunk));
		resizeFoodArray(dominion);
		updateDominion(dominion);
	}

	/**
	 * Resizes the dominion's food array to match the correct size for its current chunk count.
	 * Items in slots that fall outside the new size are lost when shrinking.
	 */
	public static void resizeFoodArray(Dominion dominion) {
		int targetSize = getFoodArraySize(dominion);
		ItemStack[] current = dominion.getFood();
		if (current.length == targetSize) {
			return;
		}
		ItemStack[] resized = new ItemStack[targetSize];
        System.arraycopy(current, 0, resized, 0, Math.min(current.length, targetSize));
		dominion.setFood(resized);
	}

	/**
	 * Determines whether the input Material is a food item.
	 * @param type The type of item being verified.
	 * @return Confirmation whether the input Material is a food item.
	 */
	public static boolean isFoodItem(Material type) {
		return getClaimFoodPower(type, 0) > 0;
	}

	/**
	 * Consumes food from the Dominion's food reserve.
	 * @param dominion The dominion.
	 * @param powerBeingConsumed The total power amount being consumed.
	 */
	public static void consumeFood(Dominion dominion, int powerBeingConsumed) {
		// Do not consume more items as the previous item is still being consumed
		if (dominion.getFoodPowerBeingConsumed() >= powerBeingConsumed) {
			dominion.setFoodPowerBeingConsumed(dominion.getFoodPowerBeingConsumed() - powerBeingConsumed);
			updateDominion(dominion);
			return;
		}

		// Set the default to consider what's currently being consumed still
		int combinedPowerOfItems = dominion.getFoodPowerBeingConsumed();

		// Go through the food currently in the reserves
		for (ItemStack food : dominion.getFood()) {
			if (food == null || food.getType() == Material.AIR) {
				continue;
			}

			// Determines if there's an increase, or if the Dominion is conquered, a decrease
			int amplifier = dominion.getConquered().size();
			if (amplifier == 0) {
				if (getConquerorOfDominion(dominion) != null) {
					amplifier = -1;
				}
			}

			int powerOfItem = getClaimFoodPower(food.getType(), amplifier);
			// Take one quantity at a time of that particular item
			for (int quantity = food.getAmount(); quantity > 0; quantity--) {
				food.setAmount(food.getAmount() - 1);
				combinedPowerOfItems += powerOfItem;

				// If there is no more power needed
				if (combinedPowerOfItems >= powerBeingConsumed) {
					int remainingPower = combinedPowerOfItems - powerBeingConsumed;
					dominion.setFoodPowerBeingConsumed(remainingPower);
					updateDominion(dominion);
					return;
				}
			}
		}
	}

	/**
	 * Consumes money or land the Dominion has when there is not enough food available.
	 * @param dominion The dominion.
	 * @param moneyToConsume The amount of money to be consumed.
	 * @return 1 if consuming money, 0 if consuming a chunk, -1 if disbanding the Dominion.
	 */
	public static int consumeMoneyOrLand(Dominion dominion, int moneyToConsume) {
		if (dominion.getBalance() >= moneyToConsume) {
			dominion.setBalance(dominion.getBalance() - moneyToConsume);
			updateDominion(dominion);
			return 1;
		} else {
			Chunk chunkToRemove = null;
			// Only the balance runs out, start unclaiming the outer chunks, where the last unclaimed chunk will be the home
			for (Chunk chunk : dominion.getChunks()) {
				// Unclaim the dominion home last
				if (dominion.getChunks().size() > 1 && dominion.getDominionHome().getChunk().equals(chunk)) {
					continue;
				}

				if (isAllClaimsConnectedAfterUnclaiming(dominion, chunk)) {
					chunkToRemove = chunk;
					break;
				}
			}

			// If unclaiming the last chunk
			if (dominion.getChunks().size() == 1) {
				return -1;
			} else {
				dominion.getChunks().remove(chunkToRemove);
				chunkKeyToDominion.remove(getChunkKey(chunkToRemove));
				resizeFoodArray(dominion);
				updateDominion(dominion);
				return 0;
			}
		}
	}

	/**
	 * Provides the list of biomes that a Dominion has access to claim from.
	 * @param dominion The Dominion.
	 * @return The list of biomes that a Dominion has access to claim from.
	 */
	public static List<Biome> getResourceClaimTypes(Dominion dominion) {
		List<Biome> biomes = new ArrayList<>();
		int y = 63;
		for (Chunk chunk : dominion.getChunks()) {
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					Biome biome = chunk.getBlock(x, y, z).getBiome();
					if (!biomes.contains(biome) && biome != Biome.LUSH_CAVES
							&& biome != Biome.DRIPSTONE_CAVES && biome != Biome.DEEP_DARK) {
						biomes.add(biome);
					}
				}
			}
		}

		return biomes;
	}

	/**
	 * Provides the name of the biome.
	 * @param biome The biome.
	 * @return The name of the biome.
	 */
	public static String getBiomeName(Biome biome) {
		String[] parts = biome.toString().split("/");
		String nameWithExtra = parts[2].substring(11);
		String name = nameWithExtra.split("]")[0];
		return ChatUtils.getFormattedItemName(name);
	}

	/**
	 * Provides the icon associated to the biome.
	 * @param biome The biome.
	 * @return The icon.
	 */
	public static List<ItemStack> getResourcesByDominionAndBiome(Dominion dominion, Biome biome) {
		List<ItemStack> items = new ArrayList<>();
		Random random = new Random();

		// Used to adjust the yields of Dominion resources based on the size of the Dominion
		int dominionSize = 0;
		// Consume 100 power per day for <=25 chunks
		if (dominion.getChunks().size() <= 25) {
			dominionSize = 1;
		} else if (dominion.getChunks().size() <= 100) {
			dominionSize = 2;
		} else {
			dominionSize = 3;
		}

		// Dirts
		// Stones
		// Woods
		// Other blocks
		// Plants
		// Ores
		// Special

		// Oceans, Rivers, Beaches, Islands
		if (biome == Biome.OCEAN) {
			items.add(new ItemStack(Material.GRAVEL, 64));
			items.add(new ItemStack(Material.SAND, 32));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.KELP, 32));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.LAPIS_LAZULI, 16));
			items.add(new ItemStack(Material.INK_SAC, 8));
			items.add(new ItemStack(Material.COD, 16));

			int oddsAmount = 4;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NAUTILUS_SHELL, 1));
			}
		}
		else if (biome == Biome.RIVER) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 32));
			items.add(new ItemStack(Material.SAND, 32));
			items.add(new ItemStack(Material.GRAVEL, 32));
			items.add(new ItemStack(Material.CLAY, 16));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.OAK_LOG, 4));
			items.add(new ItemStack(Material.SUGAR_CANE, 8));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
			items.add(new ItemStack(Material.INK_SAC, 8));
		}
		else if (biome == Biome.FROZEN_OCEAN) {
			items.add(new ItemStack(Material.GRAVEL, 64));
			items.add(new ItemStack(Material.SAND, 32));
			items.add(new ItemStack(Material.DIRT, 32));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.ICE, 64));
			items.add(new ItemStack(Material.PACKED_ICE, 64));
			items.add(new ItemStack(Material.BLUE_ICE, 8));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
			items.add(new ItemStack(Material.SALMON, 8));
			items.add(new ItemStack(Material.INK_SAC, 4));

			int oddsAmount = 4;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NAUTILUS_SHELL, 1));
			}
		}
		else if (biome == Biome.FROZEN_RIVER) {
			items.add(new ItemStack(Material.GRAVEL, 32));
			items.add(new ItemStack(Material.DIRT, 32));
			items.add(new ItemStack(Material.SAND, 32));
			items.add(new ItemStack(Material.CLAY, 8));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.ICE, 32));
			items.add(new ItemStack(Material.SUGAR_CANE, 8));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
			items.add(new ItemStack(Material.INK_SAC, 4));
		}
		else if (biome == Biome.BEACH) {
			items.add(new ItemStack(Material.SAND, 32));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.SUGAR_CANE, 8));

			int oddsAmount = 8;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.TURTLE_SCUTE, 1));
			}
		}
		else if (biome == Biome.DEEP_OCEAN) {
			items.add(new ItemStack(Material.GRAVEL, 64));
			items.add(new ItemStack(Material.GRAVEL, 64));
			items.add(new ItemStack(Material.SAND, 32));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.KELP, 32));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.LAPIS_LAZULI, 16));
			items.add(new ItemStack(Material.INK_SAC, 8));
			items.add(new ItemStack(Material.COD, 16));

			int oddsAmount = 4;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NAUTILUS_SHELL, 1));
			}

			// Simulates a sea temple
			if (random.nextInt(8) == 0) {
				items.add(new ItemStack(Material.PRISMARINE, 64));
				items.add(new ItemStack(Material.PRISMARINE_BRICKS, 64));
				items.add(new ItemStack(Material.DARK_PRISMARINE, 16));
				items.add(new ItemStack(Material.SEA_LANTERN, 16));
			}
		}
		else if (biome == Biome.STONY_SHORE) {
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.GRAVEL, 64));
			items.add(new ItemStack(Material.COAL, 16));
			items.add(new ItemStack(Material.RAW_IRON, 4));
			items.add(new ItemStack(Material.RAW_COPPER, 4));
		}
		else if (biome == Biome.SNOWY_BEACH) {
			items.add(new ItemStack(Material.SAND, 32));
			items.add(new ItemStack(Material.SNOW, 32));
			items.add(new ItemStack(Material.COAL, 16));
			items.add(new ItemStack(Material.RAW_IRON, 4));
		}
		else if (biome == Biome.WARM_OCEAN) {
			items.add(new ItemStack(Material.SAND, 64));
			items.add(new ItemStack(Material.STONE, 32));

			int coralType = random.nextInt(5);
			if (coralType == 0) {
				items.add(new ItemStack(Material.TUBE_CORAL_BLOCK, 8));
				items.add(new ItemStack(Material.TUBE_CORAL, 4));
				items.add(new ItemStack(Material.TUBE_CORAL_FAN, 4));
			} else if (coralType == 1) {
				items.add(new ItemStack(Material.BRAIN_CORAL_BLOCK, 8));
				items.add(new ItemStack(Material.BRAIN_CORAL, 4));
				items.add(new ItemStack(Material.BRAIN_CORAL_FAN, 4));
			} else if (coralType == 2) {
				items.add(new ItemStack(Material.BUBBLE_CORAL_BLOCK, 8));
				items.add(new ItemStack(Material.BUBBLE_CORAL, 4));
				items.add(new ItemStack(Material.BUBBLE_CORAL_FAN, 4));
			} else if (coralType == 3) {
				items.add(new ItemStack(Material.FIRE_CORAL_BLOCK, 8));
				items.add(new ItemStack(Material.FIRE_CORAL, 4));
				items.add(new ItemStack(Material.FIRE_CORAL_FAN, 4));
			} else if (coralType == 4) {
				items.add(new ItemStack(Material.HORN_CORAL_BLOCK, 8));
				items.add(new ItemStack(Material.HORN_CORAL, 4));
				items.add(new ItemStack(Material.HORN_CORAL_FAN, 4));
			}
			items.add(new ItemStack(Material.SEA_PICKLE, 8));

			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.LAPIS_LAZULI, 16));
			items.add(new ItemStack(Material.INK_SAC, 8));
			items.add(new ItemStack(Material.TROPICAL_FISH, 16));
			items.add(new ItemStack(Material.PUFFERFISH, 2));

			int oddsAmount = 4;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NAUTILUS_SHELL, 1));
			}
		}
		else if (biome == Biome.LUKEWARM_OCEAN) {
			items.add(new ItemStack(Material.SAND, 64));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.KELP, 32));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.LAPIS_LAZULI, 16));
			items.add(new ItemStack(Material.INK_SAC, 8));
			items.add(new ItemStack(Material.TROPICAL_FISH, 4));
			items.add(new ItemStack(Material.PUFFERFISH, 2));
			items.add(new ItemStack(Material.COD, 8));

			int oddsAmount = 4;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NAUTILUS_SHELL, 1));
			}
		}
		else if (biome == Biome.COLD_OCEAN) {
			items.add(new ItemStack(Material.GRAVEL, 64));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.KELP, 32));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.COD, 8));
			items.add(new ItemStack(Material.SALMON, 8));
			items.add(new ItemStack(Material.INK_SAC, 8));

			int oddsAmount = 4;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NAUTILUS_SHELL, 1));
			}
		}
		else if (biome == Biome.DEEP_LUKEWARM_OCEAN) {
			items.add(new ItemStack(Material.SAND, 64));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.KELP, 32));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.LAPIS_LAZULI, 16));
			items.add(new ItemStack(Material.INK_SAC, 8));
			items.add(new ItemStack(Material.COD, 4));
			items.add(new ItemStack(Material.TROPICAL_FISH, 16));

			int oddsAmount = 4;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NAUTILUS_SHELL, 1));
			}

			// Simulates a sea temple
			if (random.nextInt(8) == 0) {
				items.add(new ItemStack(Material.PRISMARINE, 64));
				items.add(new ItemStack(Material.PRISMARINE_BRICKS, 64));
				items.add(new ItemStack(Material.DARK_PRISMARINE, 16));
				items.add(new ItemStack(Material.SEA_LANTERN, 16));
			}
		}
		else if (biome == Biome.DEEP_COLD_OCEAN) {
			items.add(new ItemStack(Material.GRAVEL, 64));
			items.add(new ItemStack(Material.SAND, 32));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.KELP, 32));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.LAPIS_LAZULI, 16));
			items.add(new ItemStack(Material.INK_SAC, 8));
			items.add(new ItemStack(Material.COD, 16));

			int oddsAmount = 4;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NAUTILUS_SHELL, 1));
			}

			// Simulates a sea temple
			if (random.nextInt(8) == 0) {
				items.add(new ItemStack(Material.PRISMARINE, 64));
				items.add(new ItemStack(Material.PRISMARINE_BRICKS, 64));
				items.add(new ItemStack(Material.DARK_PRISMARINE, 16));
				items.add(new ItemStack(Material.SEA_LANTERN, 16));
			}
		}
		else if (biome == Biome.DEEP_FROZEN_OCEAN) {
			items.add(new ItemStack(Material.GRAVEL, 64));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.KELP, 32));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.LAPIS_LAZULI, 16));
			items.add(new ItemStack(Material.INK_SAC, 8));
			items.add(new ItemStack(Material.SALMON, 16));

			int oddsAmount = 4;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NAUTILUS_SHELL, 1));
			}

			// Simulates a sea temple
			if (random.nextInt(8) == 0) {
				items.add(new ItemStack(Material.PRISMARINE, 64));
				items.add(new ItemStack(Material.PRISMARINE_BRICKS, 64));
				items.add(new ItemStack(Material.DARK_PRISMARINE, 16));
				items.add(new ItemStack(Material.SEA_LANTERN, 16));
			}
		}
		else if (biome == Biome.MUSHROOM_FIELDS) {
			items.add(new ItemStack(Material.MYCELIUM, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.RED_MUSHROOM_BLOCK, 32));
			items.add(new ItemStack(Material.BROWN_MUSHROOM_BLOCK, 32));
			items.add(new ItemStack(Material.MUSHROOM_STEM, 32));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));

			if (random.nextInt(3) == 0) {
				items.add(new ItemStack(Material.DIAMOND, 1));
			}
		}

		// Flat Biomes
		else if (biome == Biome.PLAINS) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.OAK_LOG, 8));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
			items.add(new ItemStack(Material.DANDELION, 8));
			int flowerNum = random.nextInt(3);
			if (flowerNum == 0) {
				items.add(new ItemStack(Material.POPPY, 8));
			} else if (flowerNum == 1) {
				items.add(new ItemStack(Material.OXEYE_DAISY, 8));
			} else if (flowerNum == 2) {
				items.add(new ItemStack(Material.CORNFLOWER, 8));
			}
		}
		else if (biome == Biome.SUNFLOWER_PLAINS) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.OAK_LOG, 8));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
			items.add(new ItemStack(Material.DANDELION, 8));
			int flowerNum = random.nextInt(3);
			if (flowerNum == 0) {
				items.add(new ItemStack(Material.POPPY, 8));
			} else if (flowerNum == 1) {
				items.add(new ItemStack(Material.OXEYE_DAISY, 8));
			} else if (flowerNum == 2) {
				items.add(new ItemStack(Material.CORNFLOWER, 8));
			}
			items.add(new ItemStack(Material.SUNFLOWER, 16));
		}
		else if (biome == Biome.SPARSE_JUNGLE) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.JUNGLE_LOG, 8));
			items.add(new ItemStack(Material.OAK_LOG, 2));
			items.add(new ItemStack(Material.VINE, 4));
			items.add(new ItemStack(Material.MELON, 2));
			items.add(new ItemStack(Material.PUMPKIN, 2));
			items.add(new ItemStack(Material.COCOA_BEANS, 2));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
		}
		else if (biome == Biome.SNOWY_PLAINS) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.SNOW, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.SPRUCE_LOG, 8));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
		}
		else if (biome == Biome.ICE_SPIKES) {
			items.add(new ItemStack(Material.SNOW_BLOCK, 64));
			items.add(new ItemStack(Material.PACKED_ICE, 64));
			items.add(new ItemStack(Material.ICE, 32));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
		}

		// Forests
		else if (biome == Biome.FOREST) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.OAK_LOG, 16));
			items.add(new ItemStack(Material.BIRCH_LOG, 16));
			items.add(new ItemStack(Material.LEAF_LITTER, 16));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
			items.add(new ItemStack(Material.APPLE, 4));

			int odds = 16;
			if (AranarthUtils.getMonth() == Month.SOLARVOR) {
				odds = 4;
			}
			if (random.nextInt(odds) == 0) {
				items.add(new GodAppleFragment().getItem());
			}
		}
		else if (biome == Biome.TAIGA) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.SPRUCE_LOG, 32));
			items.add(new ItemStack(Material.SWEET_BERRIES, 16));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
		}
		else if (biome == Biome.SWAMP) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.OAK_LOG, 32));
			items.add(new ItemStack(Material.CLAY, 32));
			items.add(new ItemStack(Material.FIREFLY_BUSH, 8));
			items.add(new ItemStack(Material.BROWN_MUSHROOM, 8));
			items.add(new ItemStack(Material.RED_MUSHROOM, 8));
			items.add(new ItemStack(Material.LILY_PAD, 16));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
			items.add(new ItemStack(Material.SLIME_BALL, 2));
		}
		else if (biome == Biome.MANGROVE_SWAMP) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 16));
			items.add(new ItemStack(Material.MUD, 64));
			items.add(new ItemStack(Material.MUD, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.MANGROVE_LOG, 64));
			items.add(new ItemStack(Material.MANGROVE_ROOTS, 32));
			items.add(new ItemStack(Material.MUDDY_MANGROVE_ROOTS, 32));
			items.add(new ItemStack(Material.MOSS_CARPET, 32));
			items.add(new ItemStack(Material.LILY_PAD, 8));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
		}
		else if (biome == Biome.JUNGLE) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.JUNGLE_LOG, 32));
			items.add(new ItemStack(Material.OAK_LOG, 4));
			items.add(new ItemStack(Material.BAMBOO, 4));
			items.add(new ItemStack(Material.VINE, 16));
			items.add(new ItemStack(Material.MELON, 2));
			items.add(new ItemStack(Material.COCOA_BEANS, 4));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
		}
		else if (biome == Biome.BAMBOO_JUNGLE) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 16));
			items.add(new ItemStack(Material.PODZOL, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.JUNGLE_LOG, 4));
			items.add(new ItemStack(Material.OAK_LOG, 8));
			items.add(new ItemStack(Material.BAMBOO, 64));
			items.add(new ItemStack(Material.MELON, 2));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
		}
		else if (biome == Biome.BIRCH_FOREST || biome == Biome.OLD_GROWTH_BIRCH_FOREST) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.BIRCH_LOG, 32));
			items.add(new ItemStack(Material.WILDFLOWERS, 32));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
		}
		else if (biome == Biome.DARK_FOREST) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.DARK_OAK_LOG, 64));
			items.add(new ItemStack(Material.OAK_LOG, 16));
			items.add(new ItemStack(Material.BROWN_MUSHROOM_BLOCK, 16));
			items.add(new ItemStack(Material.RED_MUSHROOM_BLOCK, 16));
			items.add(new ItemStack(Material.MUSHROOM_STEM, 16));
			items.add(new ItemStack(Material.LEAF_LITTER, 16));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
			items.add(new ItemStack(Material.RAW_IRON, 4));
			items.add(new ItemStack(Material.APPLE, 4));

			int odds = 16;
			if (AranarthUtils.getMonth() == Month.SOLARVOR) {
				odds = 4;
			}
			if (random.nextInt(odds) == 0) {
				items.add(new GodAppleFragment().getItem());
			}
		}
		else if (biome == Biome.PALE_GARDEN) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.PALE_OAK_LOG, 64));
			items.add(new ItemStack(Material.PALE_MOSS_BLOCK, 16));
			items.add(new ItemStack(Material.PALE_HANGING_MOSS, 8));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
			items.add(new ItemStack(Material.RESIN_CLUMP, 8));
		}
		else if (biome == Biome.SNOWY_TAIGA) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.SPRUCE_LOG, 32));
			items.add(new ItemStack(Material.SNOW, 32));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
		}
		else if (biome == Biome.OLD_GROWTH_PINE_TAIGA || biome == Biome.OLD_GROWTH_SPRUCE_TAIGA) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 32));
			items.add(new ItemStack(Material.PODZOL, 32));
			items.add(new ItemStack(Material.COARSE_DIRT, 32));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.MOSSY_COBBLESTONE, 16));
			items.add(new ItemStack(Material.SPRUCE_LOG, 64));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));
			items.add(new ItemStack(Material.BROWN_MUSHROOM, 16));
		}
		else if (biome == Biome.FLOWER_FOREST) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.OAK_LOG, 16));
			items.add(new ItemStack(Material.BIRCH_LOG, 16));
			items.add(new ItemStack(Material.LEAF_LITTER, 16));
			int flowerNum = random.nextInt(14);
			if (flowerNum == 0) {
				items.add(new ItemStack(Material.DANDELION, 8));
			} else if (flowerNum == 1) {
				items.add(new ItemStack(Material.POPPY, 8));
			} else if (flowerNum == 2) {
				items.add(new ItemStack(Material.WHITE_TULIP, 8));
			} else if (flowerNum == 3) {
				items.add(new ItemStack(Material.PINK_TULIP, 8));
			} else if (flowerNum == 4) {
				items.add(new ItemStack(Material.ORANGE_TULIP, 8));
			} else if (flowerNum == 5) {
				items.add(new ItemStack(Material.RED_TULIP, 8));
			} else if (flowerNum == 6) {
				items.add(new ItemStack(Material.ALLIUM, 8));
			} else if (flowerNum == 7) {
				items.add(new ItemStack(Material.AZURE_BLUET, 8));
			} else if (flowerNum == 8) {
				items.add(new ItemStack(Material.OXEYE_DAISY, 8));
			} else if (flowerNum == 9) {
				items.add(new ItemStack(Material.LILY_OF_THE_VALLEY, 8));
			} else if (flowerNum == 10) {
				items.add(new ItemStack(Material.CORNFLOWER, 8));
			} else if (flowerNum == 11) {
				items.add(new ItemStack(Material.LILAC, 4));
			} else if (flowerNum == 12) {
				items.add(new ItemStack(Material.PEONY, 4));
			} else if (flowerNum == 13) {
				items.add(new ItemStack(Material.ROSE_BUSH, 4));
			}
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_IRON, 4));

			items.add(new ItemStack(Material.APPLE, 4));

			int odds = 16;
			if (AranarthUtils.getMonth() == Month.SOLARVOR) {
				odds = 4;
			}
			if (random.nextInt(odds) == 0) {
				items.add(new GodAppleFragment().getItem());
			}
		}

		// Mountains and Large Hills
		else if (biome == Biome.WINDSWEPT_HILLS) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 32));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.GRANITE, 16));
			items.add(new ItemStack(Material.DIORITE, 16));
			items.add(new ItemStack(Material.ANDESITE, 16));
			items.add(new ItemStack(Material.SPRUCE_LOG, 4));
			items.add(new ItemStack(Material.OAK_LOG, 4));
			items.add(new ItemStack(Material.COAL, 16));
			items.add(new ItemStack(Material.RAW_COPPER, 16));
			items.add(new ItemStack(Material.RAW_IRON, 16));
			items.add(new ItemStack(Material.RAW_GOLD, 8));
			items.add(new ItemStack(Material.EMERALD, 2));
		}
		else if (biome == Biome.WINDSWEPT_FOREST) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 32));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.GRANITE, 16));
			items.add(new ItemStack(Material.DIORITE, 16));
			items.add(new ItemStack(Material.ANDESITE, 16));
			items.add(new ItemStack(Material.SPRUCE_LOG, 16));
			items.add(new ItemStack(Material.OAK_LOG, 4));
			items.add(new ItemStack(Material.COAL, 16));
			items.add(new ItemStack(Material.RAW_COPPER, 16));
			items.add(new ItemStack(Material.RAW_IRON, 16));
			items.add(new ItemStack(Material.RAW_GOLD, 8));
			items.add(new ItemStack(Material.EMERALD, 2));
		}
		else if (biome == Biome.WINDSWEPT_GRAVELLY_HILLS) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 32));
			items.add(new ItemStack(Material.GRAVEL, 32));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.SPRUCE_LOG, 8));
			items.add(new ItemStack(Material.COAL, 16));
			items.add(new ItemStack(Material.RAW_COPPER, 16));
			items.add(new ItemStack(Material.RAW_IRON, 16));
			items.add(new ItemStack(Material.RAW_GOLD, 8));
			items.add(new ItemStack(Material.EMERALD, 2));
		}
		else if (biome == Biome.WINDSWEPT_SAVANNA) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 32));
			items.add(new ItemStack(Material.COARSE_DIRT, 16));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.ACACIA_LOG, 16));
			items.add(new ItemStack(Material.COAL, 16));
			items.add(new ItemStack(Material.RAW_COPPER, 16));
			items.add(new ItemStack(Material.RAW_IRON, 16));
			items.add(new ItemStack(Material.RAW_GOLD, 8));
			items.add(new ItemStack(Material.EMERALD, 2));
		}
		else if (biome == Biome.GROVE) {
			items.add(new ItemStack(Material.SNOW_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.SPRUCE_LOG, 32));
			items.add(new ItemStack(Material.COAL, 16));
			items.add(new ItemStack(Material.RAW_COPPER, 16));
			items.add(new ItemStack(Material.RAW_IRON, 16));
			items.add(new ItemStack(Material.RAW_GOLD, 8));
			items.add(new ItemStack(Material.EMERALD, 2));
		}
		else if (biome == Biome.FROZEN_PEAKS) {
			items.add(new ItemStack(Material.SNOW_BLOCK, 64));
			items.add(new ItemStack(Material.PACKED_ICE, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.COAL, 16));
			items.add(new ItemStack(Material.RAW_COPPER, 16));
			items.add(new ItemStack(Material.RAW_IRON, 16));
			items.add(new ItemStack(Material.RAW_GOLD, 8));
			items.add(new ItemStack(Material.EMERALD, 2));
		}
		else if (biome == Biome.MEADOW) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.DANDELION, 8));
			items.add(new ItemStack(Material.CORNFLOWER, 8));
			items.add(new ItemStack(Material.WILDFLOWERS, 32));
			items.add(new ItemStack(Material.COAL, 16));
			items.add(new ItemStack(Material.RAW_COPPER, 16));
			items.add(new ItemStack(Material.RAW_IRON, 16));
			items.add(new ItemStack(Material.RAW_GOLD, 8));
			items.add(new ItemStack(Material.EMERALD, 2));
		}
		else if (biome == Biome.JAGGED_PEAKS || biome == Biome.SNOWY_SLOPES) {
			items.add(new ItemStack(Material.SNOW_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.COAL, 16));
			items.add(new ItemStack(Material.RAW_COPPER, 16));
			items.add(new ItemStack(Material.RAW_IRON, 16));
			items.add(new ItemStack(Material.RAW_GOLD, 8));
			items.add(new ItemStack(Material.EMERALD, 2));
		}
		else if (biome == Biome.STONY_PEAKS) {
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.CALCITE, 32));
			items.add(new ItemStack(Material.COAL, 16));
			items.add(new ItemStack(Material.RAW_COPPER, 16));
			items.add(new ItemStack(Material.RAW_IRON, 16));
			items.add(new ItemStack(Material.RAW_GOLD, 8));
			items.add(new ItemStack(Material.EMERALD, 2));
		}
		else if (biome == Biome.CHERRY_GROVE) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.CHERRY_LOG, 32));
			items.add(new ItemStack(Material.PINK_PETALS, 32));
			items.add(new ItemStack(Material.COAL, 16));
			items.add(new ItemStack(Material.RAW_COPPER, 16));
			items.add(new ItemStack(Material.RAW_IRON, 16));
			items.add(new ItemStack(Material.RAW_GOLD, 8));
			items.add(new ItemStack(Material.EMERALD, 2));
		}

		// Dry and Desert Biomes
		else if (biome == Biome.DESERT) {
			items.add(new ItemStack(Material.SAND, 64));
			items.add(new ItemStack(Material.SAND, 64));
			items.add(new ItemStack(Material.SANDSTONE, 64));
			items.add(new ItemStack(Material.STONE, 64));
			// Higher fossil rate in deserts
			if (random.nextInt(10) == 0) {
				items.add(new ItemStack(Material.BONE_BLOCK, 32));
			}
			items.add(new ItemStack(Material.CACTUS, 8));
			items.add(new ItemStack(Material.CACTUS_FLOWER, 4));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_GOLD, 4));

		}
		else if (biome == Biome.SAVANNA || biome == Biome.SAVANNA_PLATEAU) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 64));
			items.add(new ItemStack(Material.STONE, 64));
			items.add(new ItemStack(Material.ACACIA_LOG, 32));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_GOLD, 4));

			int oddsAmount = 4;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.ARMADILLO_SCUTE, 1));
			}
		}
		else if (biome == Biome.BADLANDS || biome == Biome.ERODED_BADLANDS) {
			items.add(new ItemStack(Material.RED_SAND, 32));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.RED_SANDSTONE, 32));
			items.add(new ItemStack(Material.TERRACOTTA, 64));
			int terracottaVariant = random.nextInt(6);
			if (terracottaVariant == 0) {
				items.add(new ItemStack(Material.RED_TERRACOTTA, 16));
			} else if (terracottaVariant == 1) {
				items.add(new ItemStack(Material.ORANGE_TERRACOTTA, 16));
			} else if (terracottaVariant == 2) {
				items.add(new ItemStack(Material.YELLOW_TERRACOTTA, 16));
			} else if (terracottaVariant == 3) {
				items.add(new ItemStack(Material.BROWN_TERRACOTTA, 16));
			} else if (terracottaVariant == 4) {
				items.add(new ItemStack(Material.LIGHT_GRAY_TERRACOTTA, 16));
			} else if (terracottaVariant == 5) {
				items.add(new ItemStack(Material.WHITE_TERRACOTTA, 16));
			}
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_GOLD, 8));

			int oddsAmount = 4;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.ARMADILLO_SCUTE, 1));
			}
		}
		else if (biome == Biome.WOODED_BADLANDS) {
			items.add(new ItemStack(Material.GRASS_BLOCK, 32));
			items.add(new ItemStack(Material.COARSE_DIRT, 32));
			items.add(new ItemStack(Material.STONE, 32));
			items.add(new ItemStack(Material.TERRACOTTA, 32));
			int terracottaVariant = random.nextInt(6);
			if (terracottaVariant == 0) {
				items.add(new ItemStack(Material.RED_TERRACOTTA, 8));
			} else if (terracottaVariant == 1) {
				items.add(new ItemStack(Material.ORANGE_TERRACOTTA, 8));
			} else if (terracottaVariant == 2) {
				items.add(new ItemStack(Material.YELLOW_TERRACOTTA, 8));
			} else if (terracottaVariant == 3) {
				items.add(new ItemStack(Material.BROWN_TERRACOTTA, 8));
			} else if (terracottaVariant == 4) {
				items.add(new ItemStack(Material.LIGHT_GRAY_TERRACOTTA, 8));
			} else if (terracottaVariant == 5) {
				items.add(new ItemStack(Material.WHITE_TERRACOTTA, 8));
			}
			items.add(new ItemStack(Material.OAK_LOG, 32));
			items.add(new ItemStack(Material.LEAF_LITTER, 16));
			items.add(new ItemStack(Material.COAL, 8));
			items.add(new ItemStack(Material.RAW_GOLD, 8));

			int oddsAmount = 4;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 1;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 2;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.ARMADILLO_SCUTE, 1));
			}
		}

		// Nether and End
		else if (biome == Biome.NETHER_WASTES) {
			items.add(new ItemStack(Material.NETHERRACK, 64));
			items.add(new ItemStack(Material.NETHERRACK, 64));
			items.add(new ItemStack(Material.BLACKSTONE, 32));
			items.add(new ItemStack(Material.MAGMA_BLOCK, 8));
			items.add(new ItemStack(Material.QUARTZ, 64));
			items.add(new ItemStack(Material.GOLD_NUGGET, 64));

			int tearOdds = 2;
			int scrapOdds = 10;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				tearOdds = tearOdds - 1;
				scrapOdds = scrapOdds - 5;
			} else if (dominionSize == 3) {
				tearOdds = tearOdds - 2;
				scrapOdds = scrapOdds - 8;
			}

			if (random.nextInt(tearOdds) == 0) {
				items.add(new ItemStack(Material.GHAST_TEAR, 1));
			}
			if (random.nextInt(scrapOdds) == 0) {
				items.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
			}

			// Simulates a fortress
			if (random.nextInt(8) == 0) {
				items.add(new ItemStack(Material.NETHER_BRICKS, 64));
				items.add(new ItemStack(Material.NETHER_BRICKS, 64));
				items.add(new ItemStack(Material.NETHER_WART, 32));
			}
			// Simulates a bastion
			if (random.nextInt(12) == 0) {
				items.add(new ItemStack(Material.BLACKSTONE, 64));
				items.add(new ItemStack(Material.BLACKSTONE, 64));
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 64));
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 64));
				items.add(new ItemStack(Material.GILDED_BLACKSTONE, 32));
				items.add(new ItemStack(Material.GOLDEN_CARROT, 32));
				if (random.nextInt(3) == 0) {
					items.add(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
				}
			}
		}
		else if (biome == Biome.SOUL_SAND_VALLEY) {
			items.add(new ItemStack(Material.SOUL_SAND, 32));
			items.add(new ItemStack(Material.SOUL_SOIL, 32));
			items.add(new ItemStack(Material.GRAVEL, 8));
			items.add(new ItemStack(Material.NETHERRACK, 32));
			items.add(new ItemStack(Material.BASALT, 16));
			items.add(new ItemStack(Material.BLACKSTONE, 32));
			items.add(new ItemStack(Material.BONE_BLOCK, 8));
			items.add(new ItemStack(Material.QUARTZ, 16));
			items.add(new ItemStack(Material.GOLD_NUGGET, 16));
			items.add(new ItemStack(Material.BONE, 4));

			int tearOdds = 1;
			int scrapOdds = 10;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				tearOdds = 2;
				scrapOdds = scrapOdds - 5;
			} else if (dominionSize == 3) {
				tearOdds = 3;
				scrapOdds = scrapOdds - 8;
			}

			// 1-3 ghast tears
			items.add(new ItemStack(Material.GHAST_TEAR, random.nextInt(tearOdds) + 1));

			if (random.nextInt(scrapOdds) == 0) {
				items.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
			}

			// Simulates a fortress
			if (random.nextInt(8) == 0) {
				items.add(new ItemStack(Material.NETHER_BRICKS, 64));
				items.add(new ItemStack(Material.NETHER_BRICKS, 64));
				items.add(new ItemStack(Material.NETHER_WART, 32));
			}
			// Simulates a bastion
			if (random.nextInt(12) == 0) {
				items.add(new ItemStack(Material.BLACKSTONE, 64));
				items.add(new ItemStack(Material.BLACKSTONE, 64));
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 64));
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 64));
				items.add(new ItemStack(Material.GILDED_BLACKSTONE, 32));
				items.add(new ItemStack(Material.GOLDEN_CARROT, 32));
				if (random.nextInt(3) == 0) {
					items.add(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
				}
			}

			// Simulates a dried ghast
			if (random.nextInt(8) == 0) {
				items.add(new ItemStack(Material.DRIED_GHAST, 1));
			}
		}
		else if (biome == Biome.CRIMSON_FOREST) {
			items.add(new ItemStack(Material.CRIMSON_NYLIUM, 64));
			items.add(new ItemStack(Material.NETHERRACK, 64));
			items.add(new ItemStack(Material.BLACKSTONE, 64));
			items.add(new ItemStack(Material.CRIMSON_STEM, 64));
			items.add(new ItemStack(Material.SHROOMLIGHT, 16));
			items.add(new ItemStack(Material.WEEPING_VINES, 16));
			items.add(new ItemStack(Material.CRIMSON_FUNGUS, 16));
			items.add(new ItemStack(Material.QUARTZ, 32));
			items.add(new ItemStack(Material.GOLD_NUGGET, 32));
			items.add(new ItemStack(Material.PORKCHOP, 16));

			int scrapOdds = 10;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				scrapOdds = scrapOdds - 5;
			} else if (dominionSize == 3) {
				scrapOdds = scrapOdds - 8;
			}

			if (random.nextInt(scrapOdds) == 0) {
				items.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
			}

			// Simulates a fortress
			if (random.nextInt(8) == 0) {
				items.add(new ItemStack(Material.NETHER_BRICKS, 64));
				items.add(new ItemStack(Material.NETHER_BRICKS, 64));
				items.add(new ItemStack(Material.NETHER_WART, 32));
			}
			// Simulates a bastion
			if (random.nextInt(12) == 0) {
				items.add(new ItemStack(Material.BLACKSTONE, 64));
				items.add(new ItemStack(Material.BLACKSTONE, 64));
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 64));
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 64));
				items.add(new ItemStack(Material.GILDED_BLACKSTONE, 32));
				items.add(new ItemStack(Material.GOLDEN_CARROT, 32));
				if (random.nextInt(3) == 0) {
					items.add(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
				}
			}
		}
		else if (biome == Biome.WARPED_FOREST) {
			items.add(new ItemStack(Material.WARPED_NYLIUM, 32));
			items.add(new ItemStack(Material.NETHERRACK, 64));
			items.add(new ItemStack(Material.BLACKSTONE, 32));
			items.add(new ItemStack(Material.WARPED_STEM, 32));
			items.add(new ItemStack(Material.SHROOMLIGHT, 8));
			items.add(new ItemStack(Material.TWISTING_VINES, 8));
			items.add(new ItemStack(Material.WARPED_FUNGUS, 8));
			items.add(new ItemStack(Material.QUARTZ, 16));
			items.add(new ItemStack(Material.GOLD_NUGGET, 16));

			int pearlOdds = 1;
			int scrapOdds = 10;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				pearlOdds = 2;
				scrapOdds = scrapOdds - 5;
			} else if (dominionSize == 3) {
				pearlOdds = 3;
				scrapOdds = scrapOdds - 8;
			}

			// 1-3 ender pearls
			items.add(new ItemStack(Material.ENDER_PEARL, random.nextInt(pearlOdds) + 1));

			if (random.nextInt(scrapOdds) == 0) {
				items.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
			}

			// Simulates a fortress
			if (random.nextInt(8) == 0) {
				items.add(new ItemStack(Material.NETHER_BRICKS, 64));
				items.add(new ItemStack(Material.NETHER_BRICKS, 64));
				items.add(new ItemStack(Material.NETHER_WART, 32));
			}
			// Simulates a bastion
			if (random.nextInt(12) == 0) {
				items.add(new ItemStack(Material.BLACKSTONE, 64));
				items.add(new ItemStack(Material.BLACKSTONE, 64));
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 64));
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 64));
				items.add(new ItemStack(Material.GILDED_BLACKSTONE, 32));
				items.add(new ItemStack(Material.GOLDEN_CARROT, 32));
				if (random.nextInt(3) == 0) {
					items.add(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
				}
			}
		}
		else if (biome == Biome.BASALT_DELTAS) {
			items.add(new ItemStack(Material.NETHERRACK, 32));
			items.add(new ItemStack(Material.BASALT, 64));
			items.add(new ItemStack(Material.BASALT, 64));
			items.add(new ItemStack(Material.BLACKSTONE, 64));
			items.add(new ItemStack(Material.BLACKSTONE, 64));
			items.add(new ItemStack(Material.MAGMA_BLOCK, 8));
			items.add(new ItemStack(Material.QUARTZ, 32));
			items.add(new ItemStack(Material.GOLD_NUGGET, 64));
			items.add(new ItemStack(Material.MAGMA_CREAM, 4));

			int tearOdds = 1;
			int scrapOdds = 10;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				tearOdds = 1;
				scrapOdds = scrapOdds - 5;
			} else if (dominionSize == 3) {
				tearOdds = 2;
				scrapOdds = scrapOdds - 8;
			}

			// 0-2 ghast tears, lower than other biomes
			int ghastTears = random.nextInt(tearOdds + 1);
			if (ghastTears > 0) {
				items.add(new ItemStack(Material.GHAST_TEAR, ghastTears));
			}

			if (random.nextInt(scrapOdds) == 0) {
				items.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
			}

			// Simulates a fortress
			if (random.nextInt(8) == 0) {
				items.add(new ItemStack(Material.NETHER_BRICKS, 64));
				items.add(new ItemStack(Material.NETHER_BRICKS, 64));
				items.add(new ItemStack(Material.NETHER_WART, 32));
			}
			// Simulates a bastion
			if (random.nextInt(12) == 0) {
				items.add(new ItemStack(Material.BLACKSTONE, 64));
				items.add(new ItemStack(Material.BLACKSTONE, 64));
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 64));
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 64));
				items.add(new ItemStack(Material.GILDED_BLACKSTONE, 32));
				items.add(new ItemStack(Material.GOLDEN_CARROT, 32));
				if (random.nextInt(3) == 0) {
					items.add(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
				}
			}
		}
		else if (biome == Biome.THE_END || biome == Biome.END_BARRENS || biome == Biome.END_HIGHLANDS
				|| biome == Biome.END_MIDLANDS || biome == Biome.SMALL_END_ISLANDS) {
			items.add(new ItemStack(Material.END_STONE, 64));
			items.add(new ItemStack(Material.END_STONE, 64));
			items.add(new ItemStack(Material.OBSIDIAN, 16));
			items.add(new ItemStack(Material.CHORUS_PLANT, 16));
			items.add(new ItemStack(Material.CHORUS_FLOWER, 4));

			// Simulates an end city
			if (random.nextInt(5) == 0) {
				items.add(new ItemStack(Material.END_STONE_BRICKS, 64));
				items.add(new ItemStack(Material.END_STONE_BRICKS, 64));
				items.add(new ItemStack(Material.PURPUR_BLOCK, 64));
				items.add(new ItemStack(Material.PURPUR_PILLAR, 64));
				items.add(new ItemStack(Material.END_ROD, 16));

				int elytraOdds = 8;
				// Increases the odds based on the size
				if (dominionSize == 2) {
					elytraOdds = 6;
				} else if (dominionSize == 3) {
					elytraOdds = 3;
				}

				if (random.nextInt(elytraOdds) == 0) {
					items.add(new ItemStack(Material.ELYTRA, 1));
				}
			}
		}

		List<ItemStack> multiplierItems = new ArrayList<>();
		for (ItemStack item : items) {
			multiplierItems.add(item);

			// Skip double add for these items
			if (item.getType() == Material.NAUTILUS_SHELL || item.getType() == Material.NETHERITE_SCRAP
					|| item.getType() == Material.TURTLE_SCUTE || item.getType() == Material.ARMADILLO_SCUTE
					|| item.getType() == Material.GHAST_TEAR || item.getType() == Material.DIAMOND
					|| item.isSimilar(new GodAppleFragment().getItem())) {
				continue;
			}

			// Add additional time depending on the size
			if (dominionSize == 2) {
				multiplierItems.add(item);
			} else if (dominionSize == 3) {
				multiplierItems.add(item);
				multiplierItems.add(item);
			}
		}

		return multiplierItems;
	}

	/**
	 * Provides the rewards for the Dominion.
	 */
	public static void provideDominionRewards() {
		for (Dominion dominion : getDominions()) {
			int claimableAmount = dominion.getClaimableResources();
			OfflinePlayer player = Bukkit.getOfflinePlayer(dominion.getLeader());

			boolean isAmountIncreasing = false;
			boolean isClaimPrevented = false;
			int maxClaimableResourcesAmount = getMaxClaimableResourcesAmount(dominion);

			if (claimableAmount < maxClaimableResourcesAmount) {
				if (isAllowedToClaimResources(dominion)) {
					claimableAmount++;
					dominion.setClaimableResources(claimableAmount);
					isAmountIncreasing = true;
				} else {
					isClaimPrevented = true;
				}
			}

			if (!dominion.getConquered().isEmpty()) {
				// $500 per week per conquered Dominion, limit of 5 conquered rewards per week
				int money = dominion.getConquered().size() >= 5 ? 2500 : dominion.getConquered().size() * 500;
				dominion.setBalance(dominion.getBalance() + money);
				// Deduct $75/week per vassal as upkeep cost (conqueror funds the conquered dominion's taxes)
				int upkeepCost = dominion.getConquered().size() >= 5 ? 375 : dominion.getConquered().size() * 75;
				dominion.setBalance(dominion.getBalance() - upkeepCost);
			}

			// If the dominion is over its chunk limit, unclaim one chunk per week
			int maxChunks = dominion.getMaxChunks();
			if (dominion.getChunks().size() > maxChunks) {
				Chunk homeChunk = dominion.getDominionHome().getChunk();
				Chunk chunkToRemove = null;
				for (Chunk chunk : dominion.getChunks()) {
					if (chunk.getX() == homeChunk.getX() && chunk.getZ() == homeChunk.getZ()
							&& chunk.getWorld().equals(homeChunk.getWorld())) {
						continue;
					}
					if (isAllClaimsConnectedAfterUnclaiming(dominion, chunk)) {
						chunkToRemove = chunk;
						break;
					}
				}
				if (chunkToRemove != null) {
					dominion.getChunks().remove(chunkToRemove);
					chunkKeyToDominion.remove(getChunkKey(chunkToRemove));
					resizeFoodArray(dominion);
					if (player.isOnline()) {
						player.getPlayer().sendMessage(ChatUtils.chatMessage(
								"&cYour Dominion is over its chunk limit and has lost a chunk! "
										+ "&7(" + dominion.getChunks().size() + "/" + maxChunks + " chunks)"));
					}
				}
			}

			updateDominion(dominion);

			if (player.isOnline()) {
				if (isAmountIncreasing) {
					player.getPlayer().sendMessage(ChatUtils.chatMessage("&7It is a new week - Dominion resources may be claimed"));
				} else {
					if (isClaimPrevented) {
						player.getPlayer().sendMessage(ChatUtils.chatMessage("&7Your Dominion was unable to claim resources this week"));
					} else {
						player.getPlayer().sendMessage(ChatUtils.chatMessage("&7You must claim the available resources from your Dominion"));
					}
				}

			}
		}
	}

	/**
	 * Provides the UUID of the Dominion leader that conquered the input Dominion.
	 * @param dominion The Dominion.
	 * @return The UUID of the Dominion leader that conquered the input Dominion.
	 */
	public static UUID getConquerorOfDominion(Dominion dominion) {
		for (Dominion dominionInList : getDominions()) {
			if (dominionInList.getConquered().contains(dominion.getLeader())) {
				return dominionInList.getLeader();
			}
		}
		return null;
	}

	/**
	 * Provides the maximum amount of resources that the Dominion is permitted to claim.
	 * @param dominion The Dominion.
	 * @return The maximum amount of resources that the Dominion is permitted to claim.
	 */
	public static int getMaxClaimableResourcesAmount(Dominion dominion) {
		// If the Dominion is conquered, they are limited to a total of 8 claims
		if (getConquerorOfDominion(dominion) != null) {
			return 8;
		}
		// Base amount of 16, and increase by 8 for each conquered Dominion
		int total = 16;
		total += dominion.getConquered().size() * 8;
		return total;
	}

	/**
	 * Determines whether the Dominion will be permitted to claim resources.
	 * 50% chance for conquered Dominions, 100% chance for non-conquered.
	 * @param dominion The Dominion.
	 * @return Whether the Dominion will be permitted to claim resources.
	 */
	private static boolean isAllowedToClaimResources(Dominion dominion) {
		// If the Dominion is conquered, there is a 50% chance that the claim will be skipped
		if (getConquerorOfDominion(dominion) != null) {
			return new Random().nextBoolean();
		}

		// Always allowed to claim if not conquered
		return true;
	}

	/**
	 * Provides the color code for the given rank.
	 * @param rank The rank.
	 * @return The color code string.
	 */
	public static String getRankColor(DominionRank rank) {
		return switch (rank) {
			case LEADER -> "&6";
			case LIEUTENANT -> "&b";
			case CITIZEN -> "&a";
			case NEWCOMER -> "&7";
			case ALLIED -> "&5";
			case TRUCED -> "&d";
			case NEUTRAL -> "&f";
			case WANDERER -> "&e";
			case ENEMIED -> "&c";
		};
	}

	/**
	 * Provides the formatted Dominion rank name.
	 * @param rank The rank.
	 * @return The formatted Dominion rank name.
	 */
	public static String getFormattedRankName(DominionRank rank) {
		return getRankColor(rank) + (rank.name().toUpperCase()).charAt(0) + (rank.name().toLowerCase()).substring(1);
	}

	/**
	 * Verifies active conquest/rebellion requests and resolves any that have reached a deadline or activity threshold.
	 */
	public static void checkAndProcessConquestDeadlines() {
		long now = System.currentTimeMillis();

		List<Dominion> conquestInactive = new ArrayList<>();
		List<Dominion> conquestExpired = new ArrayList<>();
		List<Dominion> rebellionInactive = new ArrayList<>();
		List<Dominion> rebellionExpired = new ArrayList<>();

		for (Dominion dominion : getDominions()) {
			// Conquest checks (inactivity takes priority over expiry)
			if (dominion.getConqueredRequest() != null && dominion.getConqueredRequestTimestamp() > 0) {
				long defenderLastSeen = dominion.getConqueredRequestDefenderLastSeen();
				// If no defender has logged on for 3 consecutive days, auto-conquer
				if (defenderLastSeen > 0 && now - defenderLastSeen >= CONQUEST_INACTIVITY_MS) {
					conquestInactive.add(dominion);
				}
				// If the 7-day conquest window expires with defenders having been active
				else if (now - dominion.getConqueredRequestTimestamp() >= CONQUEST_DEADLINE_MS) {
					conquestExpired.add(dominion);
				}
			}
			// Rebellion checks (inactivity takes priority over expiry)
			if (dominion.getRebelRequest() != null && dominion.getRebelRequestTimestamp() > 0) {
				long lastSeen = dominion.getRebelRequestConquerorLastSeen();
				// If the conquerors are inactive for 3 consecutive days, rebellion auto-succeeds
				if (lastSeen > 0 && now - lastSeen >= REBEL_INACTIVITY_MS) {
					rebellionInactive.add(dominion);
				}
				// If the 7-day rebellion window expires with no resolution
				else if (now - dominion.getRebelRequestTimestamp() >= REBEL_DEADLINE_MS) {
					rebellionExpired.add(dominion);
				}
			}
		}

		// Conquest auto-success, no defender logged on for 3 consecutive days
		for (Dominion defender : conquestInactive) {
			Dominion conqueror = getPlayerDominion(defender.getConqueredRequest());
			defender.setConqueredRequest(null);
			defender.setConqueredRequestTimestamp(0L);
			defender.setConqueredRequestDefenderLastSeen(0L);

			if (conqueror == null) {
				updateDominion(defender);
				continue;
			}

			List<UUID> conquered = conqueror.getConquered();
			conquered.add(defender.getLeader());
			conquered.addAll(defender.getConquered());
			defender.setConquered(new ArrayList<>());
			conqueror.setConquered(conquered);
			conqueror.setLastConquerAttemptTimestamp(now);
			defender.setLastRebelAttemptTimestamp(now);
			defender.setConqueredTimestamp(now);
			updateDominion(defender);
			updateDominion(conqueror);
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_7, 1F, 1F);
				onlinePlayer.sendMessage(ChatUtils.chatMessage(defender.getName() + " &4has been conquered by &e" + conqueror.getName()));
			}
			DiscordUtils.dominionMessage(defender, defender.getName() + " has been conquered by " + conqueror.getName(), new Color(101, 0, 0));
		}

		// Conquest expired, 7-day window ended with defenders having been active, no outcome
		for (Dominion defender : conquestExpired) {
			Dominion conqueror = getPlayerDominion(defender.getConqueredRequest());
			defender.setConqueredRequest(null);
			defender.setConqueredRequestTimestamp(0L);
			defender.setConqueredRequestDefenderLastSeen(0L);

			if (conqueror == null) {
				updateDominion(defender);
				continue;
			}

			conqueror.setLastConquerAttemptTimestamp(now);
			updateDominion(defender);
			updateDominion(conqueror);
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 1F);
				onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + conqueror.getName() + " &7's conquest of &e" + defender.getName() + " &7has failed"));
			}
			DiscordUtils.dominionMessage(defender, conqueror.getName() + "'s conquest of " + defender.getName() + " has failed", new Color(135, 245, 220));
		}

		// Rebellion auto-success, conqueror was inactive for 3 consecutive days
		for (Dominion conqueror : rebellionInactive) {
			Dominion rebel = getPlayerDominion(conqueror.getRebelRequest());
			conqueror.setRebelRequest(null);
			conqueror.setRebelRequestTimestamp(0L);
			conqueror.setRebelRequestConquerorLastSeen(0L);

			if (rebel == null) {
				updateDominion(conqueror);
				continue;
			}

			List<UUID> conquered = conqueror.getConquered();
			conquered.remove(rebel.getLeader());
			conqueror.setConquered(conquered);
			rebel.setLastRebelAttemptTimestamp(now);
			updateDominion(conqueror);
			updateDominion(rebel);
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 1F);
				onlinePlayer.sendMessage(ChatUtils.chatMessage(rebel.getName() + " &dhas successfully rebelled against &e" + conqueror.getName()));
			}
			DiscordUtils.dominionMessage(conqueror, rebel.getName() + " has successfully rebelled against " + conqueror.getName(), new Color(135, 245, 220));
		}

		// Rebellion expired, 7 days passed with no resolution so the dominion stays conquered
		for (Dominion conqueror : rebellionExpired) {
			Dominion rebel = getPlayerDominion(conqueror.getRebelRequest());
			conqueror.setRebelRequest(null);
			conqueror.setRebelRequestTimestamp(0L);
			conqueror.setRebelRequestConquerorLastSeen(0L);

			if (rebel != null) {
				rebel.setLastRebelAttemptTimestamp(now);
				updateDominion(rebel);
			}
			updateDominion(conqueror);

			if (rebel != null) {
				for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
					onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_4, 1F, 1F);
					onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + rebel.getName() + "&7's rebellion against &e" + conqueror.getName() + " &7has failed"));
				}
				DiscordUtils.dominionMessage(conqueror, rebel.getName() + "'s rebellion against " + conqueror.getName() + " has failed", new Color(101, 0, 0));
			}
		}
	}

	/**
	 * Provides the death penalty multiplier that applies to the victim based on the
	 * current war state between the victim's and killer's dominions.
	 * @param victimUuid The UUID of the player who died.
	 * @param killerUuid The UUID of the player who killed them.
	 * @return The multiplier to apply to the victim's death penalties (1 or 3).
	 */
	public static int getDeathPenaltyMultiplier(UUID victimUuid, UUID killerUuid) {
		Dominion victimDom = getPlayerDominion(victimUuid);
		Dominion killerDom = getPlayerDominion(killerUuid);
		// One of the two is null
		if (victimDom == null || killerDom == null || victimDom.isSameDominion(killerDom)) {
			return 1;
		}

		// Active conquest: killerDom is conquering victimDom (victim is defender)
		if (victimDom.getConqueredRequest() != null
				&& victimDom.getConqueredRequest().equals(killerDom.getLeader())) {
			return 3;
		}
		// Active conquest: victimDom is conquering killerDom (victim is attacker)
		if (killerDom.getConqueredRequest() != null
				&& killerDom.getConqueredRequest().equals(victimDom.getLeader())) {
			return 3;
		}

		// Active rebellion: victimDom is the conqueror being rebelled against by killerDom
		if (victimDom.getRebelRequest() != null
				&& victimDom.getRebelRequest().equals(killerDom.getLeader())
				&& victimDom.getConquered().contains(killerDom.getLeader())) {
			return 3;
		}
		// Active rebellion: killerDom is the conqueror, victimDom is the rebelling dominion → 1x for rebel
		if (killerDom.getRebelRequest() != null
				&& killerDom.getRebelRequest().equals(victimDom.getLeader())
				&& killerDom.getConquered().contains(victimDom.getLeader())) {
			return 1;
		}

		return 1;
	}
}
