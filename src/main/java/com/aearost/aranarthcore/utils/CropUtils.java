package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.Month;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Handles updating crop seed item lore to display the current month's
 * grow speed and crop yield modifiers.
 */
public class CropUtils {

	public static final Set<Material> CROP_SEED_MATERIALS = EnumSet.of(
			Material.WHEAT_SEEDS,
			Material.BEETROOT_SEEDS,
			Material.CARROT,
			Material.POTATO,
			Material.NETHER_WART,
			Material.CACTUS,
			Material.COCOA_BEANS,
			Material.SUGAR_CANE,
			Material.MELON_SEEDS,
			Material.PUMPKIN_SEEDS,
			Material.SWEET_BERRIES
	);

	/**
	 * Confirms if the input block is at its full maturity.
	 * @param block The block.
	 * @return True if the crop is fully grown.
	 */
	public static boolean getIsMature(Block block) {
		if (block.getBlockData() instanceof Ageable crop) {
			return crop.getMaximumAge() == crop.getAge();
		}
		return false;
	}

	/**
	 * Maps a crop block material to the seed item material used as the key in
	 * getCropYieldMultiplier and as the replanting item.
	 * @param blockType The block material.
	 * @return The corresponding seed material.
	 */
	public static Material getSeedMaterial(Material blockType) {
		return switch (blockType) {
			case WHEAT -> Material.WHEAT_SEEDS;
			case BEETROOTS -> Material.BEETROOT_SEEDS;
			case CARROTS -> Material.CARROT;
			case POTATOES -> Material.POTATO;
			default -> blockType;
		};
	}

	/**
	 * Returns whether the given material is a tracked crop seed type.
	 * @param material The material to check.
	 * @return True if the material is a tracked crop seed.
	 */
	public static boolean isCropSeed(Material material) {
		return CROP_SEED_MATERIALS.contains(material);
	}

	/**
	 * Displays the current month's crop grow speed and crop yield modifiers.
	 * @param item The item.
	 */
	public static void updateSeedLore(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		if (!isCropSeed(item.getType())) {
			return;
		}

		double growthSpeed = getCropGrowthSpeed(AranarthUtils.getMonth(), item.getType());
		double yieldMultiplier = getCropYieldMultiplier(AranarthUtils.getMonth(), item.getType());

		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return;
		}

		List<String> lore = new ArrayList<>();
		if (growthSpeed != 1) {
			lore.add(formatMultiplierLine(growthSpeed, "speed"));
		}
		if (yieldMultiplier != 1) {
			lore.add(formatMultiplierLine(yieldMultiplier, "yield"));
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	/**
	 * Formats a multiplier as a colored lore line showing the percentage change.
	 * Positive values are green, and negative values are red.
	 * @param multiplier The multiplier value (e.g. 1.5 = +50%).
	 * @param multiplierType The type of multiplier to append.
	 * @return The formatted line for the lore.
	 */
	private static String formatMultiplierLine(double multiplier, String multiplierType) {
		int percent = (int) Math.round((multiplier - 1.0) * 100);
		String sign = percent >= 0 ? "+" : "";
		if (percent > 0) {
			return ChatUtils.translateToColor("&a&o" + sign + percent + "% " + multiplierType);
		} else {
			return ChatUtils.translateToColor("&c&o" + sign + percent + "% " + multiplierType);
		}
	}

	/**
	 * Provides the multiplier of the crop growth speed in the given month for the input crop.
	 * @param month The month.
	 * @param type The crop.
	 * @return The multiplier of the crop growth speed in the given month for the input crop.
	 */
	public static double getCropGrowthSpeed(Month month, Material type) {
		if (type == Material.WHEAT_SEEDS) {
			return switch (month) {
				case IGNIVOR   -> 0.75;
				case AQUINVOR  -> 1.5;
				case VENTIVOR  -> 2.0;
				case FLORIVOR  -> 2.5;
				case AESTIVOR  -> 1.75;
				case CALORVOR  -> 1.25;
				case ARDORVOR  -> 1.0;
				case SOLARVOR  -> 1.0;
				case FOLLIVOR -> 0.75;
				case STRIGAVOR -> 0.5;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.BEETROOT_SEEDS) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.5;
				case VENTIVOR  -> 1.0;
				case FLORIVOR  -> 1.5;
				case AESTIVOR  -> 1.25;
				case CALORVOR  -> 1.0;
				case ARDORVOR  -> 0.75;
				case SOLARVOR  -> 0.25;
				case FOLLIVOR -> 0.25;
				case STRIGAVOR -> 0.25;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.CARROT) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.75;
				case VENTIVOR  -> 1.5;
				case FLORIVOR  -> 2.0;
				case AESTIVOR  -> 1.75;
				case CALORVOR  -> 1.5;
				case ARDORVOR  -> 1.0;
				case SOLARVOR  -> 0.5;
				case FOLLIVOR -> 0.25;
				case STRIGAVOR -> 0.25;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.POTATO) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.5;
				case FLORIVOR  -> 1.0;
				case AESTIVOR  -> 1.25;
				case CALORVOR  -> 1.25;
				case ARDORVOR  -> 1.5;
				case SOLARVOR  -> 1.0;
				case FOLLIVOR -> 0.5;
				case STRIGAVOR -> 0.25;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.NETHER_WART) {
			return switch (month) {
				case IGNIVOR   -> 0.1;
				case AQUINVOR  -> 0.1;
				case VENTIVOR  -> 0.1;
				case FLORIVOR  -> 0.1;
				case AESTIVOR  -> 0.1;
				case CALORVOR  -> 0.1;
				case ARDORVOR  -> 0.1;
				case SOLARVOR  -> 0.1;
				case FOLLIVOR -> 0.1;
				case STRIGAVOR -> 0.1;
				case FAUNIVOR  -> 0.1;
				case UMBRAVOR  -> 1.0;
				case GLACIVOR  -> 1.75;
				case FRIGORVOR -> 2.5;
				case OBSCURVOR -> 1.25;
			};
		} else if (type == Material.CACTUS) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.25;
				case FLORIVOR  -> 0.25;
				case AESTIVOR  -> 0.75;
				case CALORVOR  -> 1.5;
				case ARDORVOR  -> 2.0;
				case SOLARVOR  -> 1.5;
				case FOLLIVOR -> 0.75;
				case STRIGAVOR -> 0.25;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.COCOA_BEANS) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.25;
				case FLORIVOR  -> 0.25;
				case AESTIVOR  -> 0.5;
				case CALORVOR  -> 1.0;
				case ARDORVOR  -> 1.5;
				case SOLARVOR  -> 1.25;
				case FOLLIVOR -> 1.0;
				case STRIGAVOR -> 0.75;
				case FAUNIVOR  -> 0.5;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.SUGAR_CANE) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.25;
				case FLORIVOR  -> 0.5;
				case AESTIVOR  -> 1.0;
				case CALORVOR  -> 1.25;
				case ARDORVOR  -> 1.5;
				case SOLARVOR  -> 1.5;
				case FOLLIVOR -> 1.25;
				case STRIGAVOR -> 1.0;
				case FAUNIVOR  -> 0.5;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.MELON_SEEDS) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.25;
				case FLORIVOR  -> 0.25;
				case AESTIVOR  -> 0.5;
				case CALORVOR  -> 1.25;
				case ARDORVOR  -> 1.75;
				case SOLARVOR  -> 1.5;
				case FOLLIVOR -> 1.0;
				case STRIGAVOR -> 0.25;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.PUMPKIN_SEEDS) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.25;
				case FLORIVOR  -> 0.25;
				case AESTIVOR  -> 0.5;
				case CALORVOR  -> 1.0;
				case ARDORVOR  -> 1.5;
				case SOLARVOR  -> 1.25;
				case FOLLIVOR -> 1.0;
				case STRIGAVOR -> 0.75;
				case FAUNIVOR  -> 0.5;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.SWEET_BERRIES) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.75;
				case VENTIVOR  -> 2.5;
				case FLORIVOR  -> 2.0;
				case AESTIVOR  -> 1.5;
				case CALORVOR  -> 0.75;
				case ARDORVOR  -> 0.25;
				case SOLARVOR  -> 0.25;
				case FOLLIVOR -> 1.25;
				case STRIGAVOR -> 0.25;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		}
		return 1;
	}

	/**
	 * Provides the multiplier of the crop yield in the given month for the input crop.
	 * @param month The month.
	 * @param type The crop.
	 * @return The multiplier of the crop yield in the given month for the input crop.
	 */
	public static double getCropYieldMultiplier(Month month, Material type) {
		if (type == Material.WHEAT_SEEDS) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.5;
				case FLORIVOR  -> 0.75;
				case AESTIVOR  -> 1.5;
				case CALORVOR  -> 2.25;
				case ARDORVOR  -> 3.0;
				case SOLARVOR  -> 1.75;
				case FOLLIVOR -> 0.5;
				case STRIGAVOR -> 0.25;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.BEETROOT_SEEDS) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.5;
				case FLORIVOR  -> 0.75;
				case AESTIVOR  -> 1.0;
				case CALORVOR  -> 1.5;
				case ARDORVOR  -> 2.0;
				case SOLARVOR  -> 2.5;
				case FOLLIVOR -> 1.25;
				case STRIGAVOR -> 0.5;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.CARROT) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.5;
				case FLORIVOR  -> 0.75;
				case AESTIVOR  -> 1.25;
				case CALORVOR  -> 1.75;
				case ARDORVOR  -> 2.25;
				case SOLARVOR  -> 3.0;
				case FOLLIVOR -> 1.0;
				case STRIGAVOR -> 0.25;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.POTATO) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.5;
				case FLORIVOR  -> 0.75;
				case AESTIVOR  -> 1.25;
				case CALORVOR  -> 1.75;
				case ARDORVOR  -> 2.25;
				case SOLARVOR  -> 3.0;
				case FOLLIVOR -> 1.5;
				case STRIGAVOR -> 0.5;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.NETHER_WART) {
			return switch (month) {
				case IGNIVOR   -> 1.5;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.1;
				case FLORIVOR  -> 0.1;
				case AESTIVOR  -> 0.1;
				case CALORVOR  -> 0.1;
				case ARDORVOR  -> 0.1;
				case SOLARVOR  -> 0.1;
				case FOLLIVOR -> 0.1;
				case STRIGAVOR -> 0.1;
				case FAUNIVOR  -> 0.5;
				case UMBRAVOR  -> 2.0;
				case GLACIVOR  -> 3.0;
				case FRIGORVOR -> 5.0;
				case OBSCURVOR -> 3.5;
			};
		} else if (type == Material.CACTUS) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.25;
				case FLORIVOR  -> 0.25;
				case AESTIVOR  -> 0.5;
				case CALORVOR  -> 1.25;
				case ARDORVOR  -> 2.0;
				case SOLARVOR  -> 1.25;
				case FOLLIVOR -> 0.5;
				case STRIGAVOR -> 0.25;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.COCOA_BEANS) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.25;
				case FLORIVOR  -> 0.25;
				case AESTIVOR  -> 0.5;
				case CALORVOR  -> 0.75;
				case ARDORVOR  -> 1.0;
				case SOLARVOR  -> 1.5;
				case FOLLIVOR -> 2.0;
				case STRIGAVOR -> 2.5;
				case FAUNIVOR  -> 1.5;
				case UMBRAVOR  -> 0.5;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.SUGAR_CANE) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.25;
				case FLORIVOR  -> 0.5;
				case AESTIVOR  -> 1.0;
				case CALORVOR  -> 1.5;
				case ARDORVOR  -> 2.0;
				case SOLARVOR  -> 2.5;
				case FOLLIVOR -> 3.0;
				case STRIGAVOR -> 1.5;
				case FAUNIVOR  -> 0.5;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.MELON_SEEDS) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.25;
				case FLORIVOR  -> 0.25;
				case AESTIVOR  -> 0.5;
				case CALORVOR  -> 1.25;
				case ARDORVOR  -> 2.0;
				case SOLARVOR  -> 2.5;
				case FOLLIVOR -> 3.0;
				case STRIGAVOR -> 1.5;
				case FAUNIVOR  -> 0.5;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.PUMPKIN_SEEDS) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.25;
				case VENTIVOR  -> 0.25;
				case FLORIVOR  -> 0.25;
				case AESTIVOR  -> 0.5;
				case CALORVOR  -> 1.0;
				case ARDORVOR  -> 1.5;
				case SOLARVOR  -> 2.0;
				case FOLLIVOR -> 2.5;
				case STRIGAVOR -> 3.0;
				case FAUNIVOR  -> 1.5;
				case UMBRAVOR  -> 0.5;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		} else if (type == Material.SWEET_BERRIES) {
			return switch (month) {
				case IGNIVOR   -> 0.25;
				case AQUINVOR  -> 0.75;
				case VENTIVOR  -> 1.5;
				case FLORIVOR  -> 2.25;
				case AESTIVOR  -> 3.0;
				case CALORVOR  -> 1.5;
				case ARDORVOR  -> 0.5;
				case SOLARVOR  -> 0.25;
				case FOLLIVOR -> 1.5;
				case STRIGAVOR -> 0.5;
				case FAUNIVOR  -> 0.25;
				case UMBRAVOR  -> 0.25;
				case GLACIVOR  -> 0.25;
				case FRIGORVOR -> 0.25;
				case OBSCURVOR -> 0.25;
			};
		}
		return 1;
	}

	/**
	 * Scans an inventory and updates the lore on every crop seed stack found.
	 * @param inventory The inventory to refresh.
	 */
	public static void refreshInventory(Inventory inventory) {
		ItemStack[] contents = inventory.getContents();
		for (int i = 0; i < contents.length; i++) {
			if (contents[i] != null && CropUtils.isCropSeed(contents[i].getType())) {
				CropUtils.updateSeedLore(contents[i]);
				inventory.setItem(i, contents[i]);
			}
		}
	}
}
