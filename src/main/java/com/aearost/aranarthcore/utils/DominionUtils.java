package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.Dominion;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

		Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		if (playerDominion != null) {
			if (playerDominion.getLeader().equals(player.getUniqueId())) {
				if (dominionOfChunk == null) {
					if (playerDominion.getBalance() >= 100) {
						if (playerDominion.getChunks().size() < (playerDominion.getMembers().size() * 25)) {
							if (isConnectedToClaims(playerDominion, chunkToClaim)) {
								double newBalance = playerDominion.getBalance() - 100;
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
			Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
			if (playerDominion != null) {
				if (playerDominion.getLeader().equals(dominionOfChunk.getLeader())) {
					Chunk homeChunk = playerDominion.getDominionHome().getChunk();
					if (chunk.getX() == homeChunk.getX() && chunk.getZ() == homeChunk.getZ()) {
						return "&cYou cannot unclaim the chunk that the home is in!";
					} else {
						playerDominion.getChunks().remove(chunk);
						updateDominion(playerDominion);
						return "&7This chunk has been unclaimed successfully";
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
		dominions.remove(dominion);
	}

	/**
	 * Determines if the input chunk is connected to the rest of the claims of the dominion.
	 * @param dominion The Dominion.
	 * @param chunk The chunk.
	 * @return Confirmation if the input chunk is connected to the rest of the claims of the dominion.
	 */
	public static boolean isConnectedToClaims(Dominion dominion, Chunk chunk) {
		List<Chunk> dominionChunks = dominion.getChunks();
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
	 * @param chunk The chunk attempting to be unclaimed.
	 * @return Confirmation if unclaiming the input claim would result in an unconnected claim.
	 */
	public static boolean isAllClaimsConnectedAfterUnclaiming(Dominion dominion, Chunk chunk) {
		for (Chunk dominionChunk : dominion.getChunks()) {
			List<Chunk> chunksCopy = new ArrayList<>();
			chunksCopy.addAll(dominion.getChunks());
			chunksCopy.remove(dominionChunk);

			for (Chunk copiedChunk : chunksCopy) {
				if (!isConnectedToClaims(dominion, copiedChunk)) {
					Bukkit.getLogger().info("Chunk is no longer claimed after unclaiming!!!");
					return false;
				}
			}
		}
		return true;
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
	 * Consumes contents of a Dominion's designated food inventory.
	 */
	public static void reEvaluateFoodInventory() {
		// Close all inventories before evaluating
		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			if (ChatUtils.stripColorFormatting(onlinePlayer.getOpenInventory().getTitle()).endsWith("Food Storage")) {
				onlinePlayer.closeInventory();
			}
		}

		for (Dominion dominion : DominionUtils.getDominions()) {
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
				boolean wasMoneyConsumed = consumeMoneyOrLand(dominion);
				if (Bukkit.getOfflinePlayer(dominion.getLeader()).isOnline()) {
					Player onlineLeader = Bukkit.getPlayer(dominion.getLeader());
					onlineLeader.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + " &7did not have enough food in its reserves"));
					String moneyOrLand = "&6$500 &7was consumed";
					if (!wasMoneyConsumed) {
						moneyOrLand = "a chunk was sold";
					}
					onlineLeader.sendMessage(ChatUtils.chatMessage("&7Instead, " + moneyOrLand + " to pay for the tax"));
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
	 * @return True if money was consumed, false if land was consumed.
	 */
	public static boolean consumeMoneyOrLand(Dominion dominion) {
		if (dominion.getBalance() >= 500) {
			Bukkit.getLogger().info("Balance consumed");
			dominion.setBalance(dominion.getBalance() - 500);
			updateDominion(dominion);
			return true;
		} else {
			// Only the balance runs out, start unclaiming the outer chunks, where the last unclaimed chunk will be the home
			for (Chunk chunk : dominion.getChunks()) {
				// Unclaim the dominion home last
				if (dominion.getChunks().size() > 1 && dominion.getDominionHome().getChunk().equals(chunk)) {
					Bukkit.getLogger().info("Skipping since the Dominion Home is in this chunk");
					continue;
				}

				if (isAllClaimsConnectedAfterUnclaiming(dominion, chunk)) {
					Bukkit.getLogger().info("Chunk unclaimed successfully since no money and no food");
					dominion.getChunks().remove(chunk);
					updateDominion(dominion);
				}
			}

			return false;
		}
	}

}
