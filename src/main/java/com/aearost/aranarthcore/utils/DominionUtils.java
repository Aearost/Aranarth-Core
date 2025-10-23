package com.aearost.aranarthcore.utils;

import org.bukkit.Material;

/**
 * Provides a large variety of utility methods for everything related to land claiming.
 */
public class DominionUtils {

	public static void createNewClaim() {

	}

	public static void addClaim() {

	}

	public static void removeClaim() {

	}

	public static void abandonClaim() {

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
