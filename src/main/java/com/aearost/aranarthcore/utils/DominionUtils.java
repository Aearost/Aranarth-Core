package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import org.bukkit.*;
import org.bukkit.block.Biome;
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
							member.sendMessage(ChatUtils.chatMessage("&7Instead, &6$250 &7was consumed &7was sold to pay for the tax"));
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
		if (type == Material.ENCHANTED_GOLDEN_APPLE || type == Material.CAKE) {
			return 500;
		} else if (type == Material.MUSHROOM_STEW || type == Material.BEETROOT_SOUP) {
			return 150;
		} else if (type == Material.HAY_BLOCK || type == Material.RABBIT_STEW) {
			return 50;
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
		} else if (type == Material.CARROT || type == Material.POTATO || type == Material.DRIED_KELP) {
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
		if (dominion.getBalance() >= 250) {
			dominion.setBalance(dominion.getBalance() - 250);
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
					if (!biomes.contains(biome)) {
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
			items.add(new ItemStack(Material.QUARTZ, 32));
			items.add(new ItemStack(Material.GOLD_NUGGET, 32));

			int tearOdds = 4;
			int oddsAmount = 25;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				tearOdds = tearOdds - 1;
				oddsAmount = oddsAmount - 5;
			} else if (dominionSize == 3) {
				tearOdds = tearOdds - 2;
				oddsAmount = oddsAmount - 10;
			}

			if (random.nextInt(tearOdds) == 0) {
				items.add(new ItemStack(Material.GHAST_TEAR, 1));
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
			}

			// Simulates a fortress
			if (random.nextInt(10) == 0) {
				items.add(new ItemStack(Material.NETHER_BRICK, 32));
				items.add(new ItemStack(Material.NETHER_WART, 8));
			}
			// Simulates a bastion
			if (random.nextInt(20) == 0) {
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 32));
				items.add(new ItemStack(Material.GILDED_BLACKSTONE, 16));
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

			int tearOdds = 4;
			int oddsAmount = 25;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				tearOdds = tearOdds - 1;
				oddsAmount = oddsAmount - 5;
			} else if (dominionSize == 3) {
				tearOdds = tearOdds - 2;
				oddsAmount = oddsAmount - 10;
			}
			if (random.nextInt(tearOdds) == 0) {
				items.add(new ItemStack(Material.GHAST_TEAR, 1));
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
			}

			// Simulates a fortress
			if (random.nextInt(10) == 0) {
				items.add(new ItemStack(Material.NETHER_BRICK, 32));
				items.add(new ItemStack(Material.NETHER_WART, 8));
			}
			// Simulates a bastion
			if (random.nextInt(20) == 0) {
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 32));
				items.add(new ItemStack(Material.GILDED_BLACKSTONE, 16));
				items.add(new ItemStack(Material.GOLDEN_CARROT, 32));
				if (random.nextInt(3) == 0) {
					items.add(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
				}
			}
		}
		else if (biome == Biome.CRIMSON_FOREST) {
			items.add(new ItemStack(Material.CRIMSON_NYLIUM, 32));
			items.add(new ItemStack(Material.NETHERRACK, 64));
			items.add(new ItemStack(Material.BLACKSTONE, 32));
			items.add(new ItemStack(Material.CRIMSON_STEM, 32));
			items.add(new ItemStack(Material.SHROOMLIGHT, 8));
			items.add(new ItemStack(Material.WEEPING_VINES, 8));
			items.add(new ItemStack(Material.CRIMSON_FUNGUS, 8));
			items.add(new ItemStack(Material.QUARTZ, 16));
			items.add(new ItemStack(Material.GOLD_NUGGET, 16));
			items.add(new ItemStack(Material.PORKCHOP, 16));

			int oddsAmount = 25;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				oddsAmount = oddsAmount - 5;
			} else if (dominionSize == 3) {
				oddsAmount = oddsAmount - 10;
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
			}

			// Simulates a fortress
			if (random.nextInt(10) == 0) {
				items.add(new ItemStack(Material.NETHER_BRICK, 32));
				items.add(new ItemStack(Material.NETHER_WART, 8));
			}
			// Simulates a bastion
			if (random.nextInt(20) == 0) {
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 32));
				items.add(new ItemStack(Material.GILDED_BLACKSTONE, 16));
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

			int pearlOdds = 5;
			int oddsAmount = 25;

			// Increases the odds based on the size
			if (dominionSize == 2) {
				pearlOdds = pearlOdds - 1;
				oddsAmount = oddsAmount - 5;
			} else if (dominionSize == 3) {
				pearlOdds = pearlOdds - 2;
				oddsAmount = oddsAmount - 10;
			}
			if (random.nextInt(pearlOdds) == 0) {
				items.add(new ItemStack(Material.ENDER_PEARL, 1));
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
			}

			// Simulates a fortress
			if (random.nextInt(10) == 0) {
				items.add(new ItemStack(Material.NETHER_BRICK, 32));
				items.add(new ItemStack(Material.NETHER_WART, 8));
			}
			// Simulates a bastion
			if (random.nextInt(20) == 0) {
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 32));
				items.add(new ItemStack(Material.GILDED_BLACKSTONE, 16));
				items.add(new ItemStack(Material.GOLDEN_CARROT, 32));
				if (random.nextInt(3) == 0) {
					items.add(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
				}
			}
		}
		else if (biome == Biome.BASALT_DELTAS) {
			items.add(new ItemStack(Material.NETHERRACK, 16));
			items.add(new ItemStack(Material.BASALT, 64));
			items.add(new ItemStack(Material.BASALT, 64));
			items.add(new ItemStack(Material.BLACKSTONE, 16));
			items.add(new ItemStack(Material.MAGMA_BLOCK, 8));
			items.add(new ItemStack(Material.QUARTZ, 16));
			items.add(new ItemStack(Material.GOLD_NUGGET, 16));
			items.add(new ItemStack(Material.MAGMA_CREAM, 2));

			int tearOdds = 6; // Lower odds than other biomes
			int oddsAmount = 25;
			// Increases the odds based on the size
			if (dominionSize == 2) {
				tearOdds = tearOdds - 1;
				oddsAmount = oddsAmount - 5;
			} else if (dominionSize == 3) {
				tearOdds = tearOdds - 2;
				oddsAmount = oddsAmount - 10;
			}

			if (random.nextInt(tearOdds) == 0) {
				items.add(new ItemStack(Material.GHAST_TEAR, 1));
			}
			if (random.nextInt(oddsAmount) == 0) {
				items.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
			}

			// Simulates a fortress
			if (random.nextInt(10) == 0) {
				items.add(new ItemStack(Material.NETHER_BRICK, 32));
				items.add(new ItemStack(Material.NETHER_WART, 8));
			}
			// Simulates a bastion
			if (random.nextInt(20) == 0) {
				items.add(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 32));
				items.add(new ItemStack(Material.GILDED_BLACKSTONE, 16));
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
			items.add(new ItemStack(Material.OBSIDIAN, 8));
			items.add(new ItemStack(Material.CHORUS_PLANT, 16));
			items.add(new ItemStack(Material.CHORUS_FLOWER, 4));

			// Simulates an end city
			if (random.nextInt(5) == 0) {
				items.add(new ItemStack(Material.END_STONE_BRICKS, 64));
				items.add(new ItemStack(Material.END_STONE_BRICKS, 64));
				items.add(new ItemStack(Material.PURPUR_BLOCK, 64));
				items.add(new ItemStack(Material.PURPUR_PILLAR, 64));
				items.add(new ItemStack(Material.END_ROD, 16));
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
	 * Increases the amount of resources that can be claimed by the Dominion.
	 */
	public static void increaseClaimableResources() {
		for (Dominion dominion : getDominions()) {
			int claimableAmount = dominion.getClaimableResources();
			OfflinePlayer player = Bukkit.getOfflinePlayer(dominion.getLeader());
			boolean isIncreasedAmount = false;

			if (claimableAmount < 16) {
				claimableAmount++;
				dominion.setClaimableResources(claimableAmount);
				updateDominion(dominion);
				isIncreasedAmount = true;
			}

			if (player.isOnline()) {
				if (isIncreasedAmount) {
					player.getPlayer().sendMessage(ChatUtils.chatMessage("&7It is a new week - Dominion resources may be claimed"));
				} else {
					player.getPlayer().sendMessage(ChatUtils.chatMessage("&7Your Dominion must claim its available resources!"));
				}
			}
		}
	}
}
