package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Provides a large variety of utility methods for everything related to land claiming.
 */
public class DominionUtils {

	private static final List<Dominion> dominions = new ArrayList<>();

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
		Dominion playerDominion = null;
		for (Dominion dominion : dominions) {
			for (UUID memberUuid : dominion.getMembers()) {
                if (memberUuid.equals(uuid)) {
                    playerDominion = dominion;
                    break;
                }
			}
		}
		return playerDominion;
	}

	/**
	 * Creates a new dominion.
	 * @param dominion The dominion being added.
	 */
	public static void createDominion(Dominion dominion) {
		dominions.add(dominion);
	}

	/**
	 * Updates an existing dominion with new values.
	 * @param dominion The new value of the dominion.
	 */
	public static void updateDominion(Dominion dominion) {
		int i = 0;
		while (i < dominions.size()) {
			if (dominions.get(i).getLeader().equals(dominion.getLeader())) {
				break;
			}
			i++;
		}
		dominions.set(i, dominion);
	}

	/**
	 * Claims the chunk for the dominion.
	 * @param player The player attempting to claim the chunk.
	 * @param chunkToClaim The chunk the player is attempting to claim.
	 * @return The message of whether the chunk was claimed.
	 */
	public static String claimChunk(Player player, Chunk chunkToClaim) {
		Dominion dominionOfChunk = getDominionOfChunk(chunkToClaim);

		Dominion playerDominion = getPlayerDominion(player.getUniqueId());
		if (playerDominion != null) {
			if (playerDominion.getLeader().equals(player.getUniqueId())) {
				if (dominionOfChunk == null) {
					int claimPrice = 250;
					if (playerDominion.getBalance() >= claimPrice) {
						if (playerDominion.getChunks().size() < (playerDominion.getMembers().size() * 25)) {
							if (isConnectedToClaims(playerDominion.getChunks(), chunkToClaim)) {
								double newBalance = playerDominion.getBalance() - claimPrice;
								playerDominion.setBalance(newBalance);
								List<Chunk> chunks = playerDominion.getChunks();
								chunks.add(chunkToClaim);
								updateDominion(playerDominion);
								return "&e" + playerDominion.getName() + " &7has claimed &e" +
										playerDominion.getChunks().size() + "/" + (playerDominion.getMembers().size() * 25) + " chunks";
							} else {
								return "&cThis chunk is not connected to the rest of your Dominion!";
							}
						} else {
							return "&cYou cannot claim more than &e" + (playerDominion.getMembers().size() * 25) + " chunks!";
						}
					} else {
						return "&cYour dominion cannot afford this!";
					}
				} else {
					if (playerDominion.getLeader().equals(dominionOfChunk.getLeader())) {
						return "&cThis chunk is already claimed by your dominion";
					} else {
						return "&cThis chunk is already claimed by &e" + dominionOfChunk.getName();
					}
				}
			} else {
				return "&cOnly the owner of the dominion can claim land!";
			}
		} else {
			return "&cYou are not part of a dominion!";
		}
    }

	/**
	 * Provides the dominion that owns the chunk.
	 * @param chunk The chunk being verified.
	 * @return The dominion that owns the chunk.
	 */
	public static Dominion getDominionOfChunk(Chunk chunk) {
		for (Dominion dominion : dominions) {
			if (dominion.getChunks().contains(chunk)) {
				return dominion;
			}
		}
		return null;
	}

	/**
	 * Unclaims the chunk that the user is standing in.
	 * @param player The player that is attempting to unclaim the chunk.
	 * @return The message of whether the chunk was unclaimed.
	 */
	public static String unclaimChunk(Player player) {
		Chunk chunk = player.getLocation().getChunk();
		Dominion dominionOfChunk = getDominionOfChunk(chunk);
		if (dominionOfChunk != null) {
			Dominion playerDominion = getPlayerDominion(player.getUniqueId());
			if (playerDominion != null) {
				if (playerDominion.getLeader().equals(dominionOfChunk.getLeader())) {
					Chunk homeChunk = playerDominion.getDominionHome().getChunk();
					if (chunk.getX() == homeChunk.getX() && chunk.getZ() == homeChunk.getZ()) {
						return "&cYou cannot unclaim the chunk that the home is in!";
					} else {
						if (isAllClaimsConnectedAfterUnclaiming(dominionOfChunk, chunk)) {
							AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
							aranarthPlayer.setBalance(aranarthPlayer.getBalance() + 125);
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
							playerDominion.getChunks().remove(chunk);
							updateDominion(playerDominion);
							return "&7This chunk has been unclaimed successfully";
						} else {
							return "&cYou cannot unclaim this chunk as they all must remain connected!";
						}
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
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(dominion.getLeader());
		aranarthPlayer.setBalance(aranarthPlayer.getBalance() + dominion.getBalance());
		if (Bukkit.getOfflinePlayer(dominion.getLeader()).isOnline()) {
			Bukkit.getPlayer(dominion.getLeader()).sendMessage(ChatUtils.chatMessage("&7Your Dominion's balance has been added to your own"));
		}

		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.5F);
		}

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
	 * Determines if the two Dominions are marked as allies of each other.
	 * @param dominion1 The first Dominion.
	 * @param dominion2 The second Dominion.
	 * @return Confirmation if the two Dominions are marked as allies of each other.
	 */
	public static boolean areAllied(Dominion dominion1, Dominion dominion2) {
		return dominion1.getAllied().contains(dominion2.getLeader()) && dominion2.getAllied().contains(dominion1.getLeader());
	}

	/**
	 * Determines if the two Dominions are marked as enemies with each other.
	 * @param dominion1 The first Dominion.
	 * @param dominion2 The second Dominion.
	 * @return Confirmation if the two Dominions are marked as enemies with each other.
	 */
	public static boolean areTruced(Dominion dominion1, Dominion dominion2) {
		return dominion1.getTruced().contains(dominion2.getLeader()) && dominion2.getTruced().contains(dominion1.getLeader());
	}

	/**
	 * Determines if the two Dominions are marked as enemies of each other.
	 * @param dominion1 The first Dominion.
	 * @param dominion2 The second Dominion.
	 * @return Confirmation if the two Dominions are marked as enemies of each other.
	 */
	public static boolean areEnemied(Dominion dominion1, Dominion dominion2) {
		//  Unlike ally and truced, if one of the two is enemied, both are considered enemies
		return dominion1.getEnemied().contains(dominion2.getLeader()) || dominion2.getEnemied().contains(dominion1.getLeader());
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
			updateDominion(dominion);
		}
		if (!isDeleting) {
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
			if (ChatUtils.stripColorFormatting(onlinePlayer.getOpenInventory().getTitle()).endsWith("Food Storage")) {
				onlinePlayer.closeInventory();
			}
		}

		for (Dominion dominion : getDominions()) {
			int totalPower = 0;
			for (ItemStack food : dominion.getFood()) {
				if (food == null || food.getType() == Material.AIR) {
					continue;
				}

				int powerOfFoodItem = getClaimFoodPower(food.getType());
				totalPower += (powerOfFoodItem * food.getAmount());
			}

			int powerBeingConsumed = 0;
			// Consume 100 power per day for <=25 chunks
			if (dominion.getChunks().size() <= 25) {
				powerBeingConsumed = 100;
			} else if (dominion.getChunks().size() <= 100) {
				powerBeingConsumed = 250;
			} else {
				powerBeingConsumed = 500;
			}

			if (totalPower >= powerBeingConsumed) {
				consumeFood(dominion, powerBeingConsumed);
				if (Bukkit.getOfflinePlayer(dominion.getLeader()).isOnline()) {
					Player onlineLeader = Bukkit.getPlayer(dominion.getLeader());
					onlineLeader.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + "'s &7daily food rations have been consumed"));
				}
			} else {
				int result = consumeMoneyOrLand(dominion);
				for (UUID memberUuid : dominion.getMembers()) {
					if (Bukkit.getOfflinePlayer(memberUuid).isOnline()) {
						Player member = Bukkit.getPlayer(memberUuid);
						member.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + " &7did not have enough food in its reserves"));
						// Money was consumed
						if (result == 1) {
							member.sendMessage(ChatUtils.chatMessage("&7Instead, &6$500 &7was consumed &7was sold to pay for the tax"));
						}
						// Land was consumed
						else if (result == 0) {
							member.sendMessage(ChatUtils.chatMessage("&7Instead, a chunk was sold to pay for the tax"));
						}
						// Last chunk was consumed
						else {
							updateDominionLeader(dominion, null, true);
						}
					}
				}
			}


		}
	}

	/**
	 * Determines the claim power that the input item contains.
	 * @param type The Material of item being provided.
	 * @return The claim power that the input item contains.
	 */
	public static int getClaimFoodPower(Material type) {
		if (type == Material.ENCHANTED_GOLDEN_APPLE) {
			return 200;
		} else if (type == Material.CAKE) {
			return 100;
		} else if (type == Material.HAY_BLOCK || type == Material.RABBIT_STEW) {
			return 50;
		} else if (type == Material.MUSHROOM_STEW || type == Material.BEETROOT_SOUP) {
			return 40;
		} else if (type == Material.GOLDEN_APPLE || type == Material.COOKED_PORKCHOP || type == Material.COOKED_MUTTON
				|| type == Material.COOKED_BEEF || type == Material.COOKED_CHICKEN || type == Material.COOKED_RABBIT
				|| type == Material.COOKED_COD || type == Material.COOKED_SALMON) {
			return 32;
		} else if (type == Material.PUMPKIN_PIE) {
			return 25;
		} else if (type == Material.DRIED_KELP_BLOCK) {
			return 20;
		} else if (type == Material.BREAD || type == Material.APPLE) {
			return 16;
		} else if (type == Material.GOLDEN_CARROT) {
			return 10;
		} else if (type == Material.PORKCHOP || type == Material.MUTTON
				|| type == Material.BEEF || type == Material.CHICKEN || type == Material.RABBIT
				|| type == Material.COD || type == Material.SALMON) {
			return 8;
		} else if (type == Material.POISONOUS_POTATO) {
			return 6;
		} else if (type == Material.WHEAT || type == Material.BEETROOT) {
			return 5;
		} else if (type == Material.BAKED_POTATO) {
			return 3;
		} else if (type == Material.CARROTS || type == Material.POTATO || type == Material.DRIED_KELP) {
			return 2;
		} else if (type == Material.MELON_SLICE || type == Material.SWEET_BERRIES || type == Material.GLOW_BERRIES
				|| type == Material.CHORUS_FRUIT || type == Material.COOKIE) {
			return 1;
		}
		return 0;
	}

	/**
	 * Determines whether the input Material is a food item.
	 * @param type The type of item being verified.
	 * @return Confirmation whether the input Material is a food item.
	 */
	public static boolean isFoodItem(Material type) {
		return getClaimFoodPower(type) > 0;
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

			int powerOfItem = getClaimFoodPower(food.getType());
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
	 * @return 1 if consuming money, 0 if consuming a chunk, -1 if disbanding the Dominion.
	 */
	public static int consumeMoneyOrLand(Dominion dominion) {
		if (dominion.getBalance() >= 500) {
			dominion.setBalance(dominion.getBalance() - 500);
			updateDominion(dominion);
			return 1;
		} else {
			Chunk chunkToRemove = null;
			// Only the balance runs out, start unclaiming the outer chunks, where the last unclaimed chunk will be the home
			for (Chunk chunk : dominion.getChunks()) {
				Bukkit.getLogger().info("Iterating over chunk: " + chunk.getX() + "|" + chunk.getZ());
				// Unclaim the dominion home last
				if (dominion.getChunks().size() > 1 && dominion.getDominionHome().getChunk().equals(chunk)) {
					Bukkit.getLogger().info("Home chunk, skipping to next");
					continue;
				}

				if (isAllClaimsConnectedAfterUnclaiming(dominion, chunk)) {
					Bukkit.getLogger().info("All claims will remain connected, can remove this chunk");
					chunkToRemove = chunk;
					break;
				} else {
					Bukkit.getLogger().info("Cannot remove this chunk as claims will not be connected");
				}
			}

			// If unclaiming the last chunk
			if (dominion.getChunks().size() == 1) {
				return -1;
			} else {
				dominion.getChunks().remove(chunkToRemove);
				updateDominion(dominion);
				return 0;
			}
		}
	}

}
