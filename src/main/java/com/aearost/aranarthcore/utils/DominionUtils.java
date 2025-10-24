package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.Dominion;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;

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
	 * @param player The player.
	 * @return The Dominion that the player is in.
	 */
	public static Dominion getPlayerDominion(Player player) {
		Dominion playerDominion = null;
		for (Dominion dominion : dominions) {
			for (UUID uuid : dominion.getMembers()) {
				if (player.getUniqueId().equals(uuid)){
					playerDominion = dominion;
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
			if (dominions.get(i).getOwner().equals(dominion.getOwner())) {
				break;
			}
			i++;
		}
		dominions.set(i, dominion);
	}

	/**
	 * Claims the chunk for the dominion.
	 * @param dominion The dominion attempting to claim the chunk.
	 * @param chunk The chunk attempting to be claimed.
	 * @return The message of whether the chunk was claimed.
	 */
	public static String claimChunk(Dominion dominion, Chunk chunk) {
		Dominion dominionOfChunk = getDominionOfChunk(chunk);
		if (dominionOfChunk == null) {
			if (dominion.getBalance() < 100) {
				double newBalance = dominion.getBalance() - 100;
				dominion.setBalance(newBalance);
				List<Chunk> chunks = dominion.getChunks();
				chunks.add(chunk);

				updateDominion(dominion);
				return ChatUtils.chatMessage("&7This chunk has been claimed for " + dominion.getName());
			} else {
				return ChatUtils.chatMessage("&cYour dominion cannot afford this!");
			}
		} else {
			return ChatUtils.chatMessage("&cThis chunk is already claimed by " + dominionOfChunk.getName() + "!");
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
	 * @param dominion The dominion that is attempting to unclaim the chunk.
	 * @param chunk The chunk that is attempting to be unclaimed.
	 * @return The message of whether the chunk was unclaimed.
	 */
	public static String unclaimChunk(Dominion dominion, Chunk chunk) {
		Dominion dominionOfChunk = getDominionOfChunk(chunk);
		if (dominionOfChunk != null) {
			if (dominion.getOwner().equals(dominionOfChunk.getOwner())) {
				dominion.getChunks().remove(chunk);
				updateDominion(dominion);
				return ChatUtils.chatMessage("&7This chunk has been unclaimed successfully");
			} else {
				return ChatUtils.chatMessage("&cThis chunk not claimed by " + dominionOfChunk.getName() + "!");
			}
		} else {
			return ChatUtils.chatMessage("&cThis chunk is not claimed!");
		}
	}

	public static void disbandDominion(Dominion dominion) {

	}

	public static void isClaimable() {

	}

	public static void reEvaluateFoodChests() {

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
			return 75;
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

}
