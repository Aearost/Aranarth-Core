package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GuiDominionResources {

	private final Player player;
	private final Inventory initializedGui;

	public GuiDominionResources(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player) {
		Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		List<Biome> biomes = DominionUtils.getResourceClaimTypes(dominion);

		int size = 0;
		if (biomes.size() <= 9) {
			size = 9;
		} else if (biomes.size() <= 18) {
			size = 18;
		} else if (biomes.size() <= 27) {
			size = 27;
		} else if (biomes.size() <= 36) {
			size = 36;
		} else if (biomes.size() <= 45) {
			size = 45;
		} else {
			size = 54;
		}

		Inventory gui = Bukkit.getServer().createInventory(player, size, ChatUtils.translateToColor("&e" + dominion.getName() +"'s &rResources"));
		for (int i = 0; i < biomes.size(); i++) {
			ItemStack item = new ItemStack(getIconByBiome(biomes.get(i)));
			ItemMeta itemMeta = item.getItemMeta();
			itemMeta.setDisplayName(getBiomeName(biomes.get(i)));
			item.setItemMeta(itemMeta);
			gui.setItem(i, item);
		}
		return gui;
	}

	/**
	 * Provides the icon associated to the biome.
	 * @param biome The biome.
	 * @return The icon.
	 */
	private Material getIconByBiome(Biome biome) {
		if (biome == Biome.OCEAN) {
			return Material.WATER_BUCKET;
		} else if (biome == Biome.PLAINS) {
			return Material.GRASS_BLOCK;
		} else if (biome == Biome.DESERT) {
			return Material.SAND;
		} else if (biome == Biome.WINDSWEPT_HILLS) {
			return Material.STONE;
		} else if (biome == Biome.FOREST) {
			return Material.GRASS_BLOCK;
		} else if (biome == Biome.TAIGA) {
			return Material.SPRUCE_LEAVES;
		} else if (biome == Biome.SWAMP) {
			return Material.VINE;
		} else if (biome == Biome.MANGROVE_SWAMP) {
			return Material.MANGROVE_LOG;
		} else if (biome == Biome.RIVER) {
			return Material.WATER_BUCKET;
		} else if (biome == Biome.NETHER_WASTES) {
			return Material.NETHERRACK;
		} else if (biome == Biome.THE_END) {
			return Material.END_STONE;
		} else if (biome == Biome.FROZEN_OCEAN) {
			return Material.PACKED_ICE;
		} else if (biome == Biome.FROZEN_RIVER) {
			return Material.ICE;
		} else if (biome == Biome.SNOWY_PLAINS) {
			return Material.SNOW;
		} else if (biome == Biome.MUSHROOM_FIELDS) {
			return Material.MYCELIUM;
		} else if (biome == Biome.BEACH) {
			return Material.SAND;
		} else if (biome == Biome.JUNGLE) {
			return Material.JUNGLE_LOG;
		} else if (biome == Biome.SPARSE_JUNGLE) {
			return Material.JUNGLE_SAPLING;
		} else if (biome == Biome.DEEP_OCEAN) {
			return Material.SEAGRASS;
		} else if (biome == Biome.STONY_SHORE) {
			return Material.STONE;
		} else if (biome == Biome.SNOWY_BEACH) {
			return Material.SAND;
		} else if (biome == Biome.BIRCH_FOREST) {
			return Material.BIRCH_LOG;
		} else if (biome == Biome.DARK_FOREST) {
			return Material.DARK_OAK_LOG;
		} else if (biome == Biome.PALE_GARDEN) {
			return Material.PALE_OAK_LOG;
		} else if (biome == Biome.SNOWY_TAIGA) {
			return Material.SPRUCE_LOG;
		} else if (biome == Biome.OLD_GROWTH_PINE_TAIGA) {
			return Material.MOSSY_COBBLESTONE;
		} else if (biome == Biome.WINDSWEPT_FOREST) {
			return Material.OAK_LEAVES;
		} else if (biome == Biome.SAVANNA) {
			return Material.SHORT_GRASS;
		} else if (biome == Biome.SAVANNA_PLATEAU) {
			return Material.ACACIA_LOG;
		} else if (biome == Biome.BADLANDS) {
			return Material.TERRACOTTA;
		} else if (biome == Biome.WOODED_BADLANDS) {
			return Material.OAK_LOG;
		} else if (biome == Biome.SMALL_END_ISLANDS) {
			return Material.END_STONE;
		} else if (biome == Biome.END_MIDLANDS) {
			return Material.END_STONE;
		} else if (biome == Biome.END_HIGHLANDS) {
			return Material.CHORUS_FLOWER;
		} else if (biome == Biome.END_BARRENS) {
			return Material.END_STONE;
		} else if (biome == Biome.WARM_OCEAN) {
			return Material.BRAIN_CORAL;
		} else if (biome == Biome.LUKEWARM_OCEAN) {
			return Material.COD;
		} else if (biome == Biome.COLD_OCEAN) {
			return Material.SALMON;
		} else if (biome == Biome.DEEP_LUKEWARM_OCEAN) {
			return Material.WATER_BUCKET;
		} else if (biome == Biome.DEEP_COLD_OCEAN) {
			return Material.WATER_BUCKET;
		} else if (biome == Biome.DEEP_FROZEN_OCEAN) {
			return Material.PACKED_ICE;
		} else if (biome == Biome.SUNFLOWER_PLAINS) {
			return Material.SUNFLOWER;
		} else if (biome == Biome.WINDSWEPT_GRAVELLY_HILLS) {
			return Material.GRAVEL;
		} else if (biome == Biome.FLOWER_FOREST) {
			return Material.ROSE_BUSH;
		} else if (biome == Biome.ICE_SPIKES) {
			return Material.PACKED_ICE;
		} else if (biome == Biome.OLD_GROWTH_BIRCH_FOREST) {
			return Material.BIRCH_LOG;
		} else if (biome == Biome.OLD_GROWTH_SPRUCE_TAIGA) {
			return Material.SPRUCE_LEAVES;
		} else if (biome == Biome.WINDSWEPT_SAVANNA) {
			return Material.STONE;
		} else if (biome == Biome.ERODED_BADLANDS) {
			return Material.RED_SAND;
		} else if (biome == Biome.BAMBOO_JUNGLE) {
			return Material.BAMBOO;
		} else if (biome == Biome.SOUL_SAND_VALLEY) {
			return Material.SOUL_SAND;
		} else if (biome == Biome.CRIMSON_FOREST) {
			return Material.CRIMSON_STEM;
		} else if (biome == Biome.WARPED_FOREST) {
			return Material.WARPED_STEM;
		} else if (biome == Biome.BASALT_DELTAS) {
			return Material.BASALT;
		} else if (biome == Biome.MEADOW) {
			return Material.ALLIUM;
		} else if (biome == Biome.GROVE) {
			return Material.SPRUCE_LEAVES;
		} else if (biome == Biome.SNOWY_SLOPES) {
			return Material.SNOW;
		} else if (biome == Biome.FROZEN_PEAKS) {
			return Material.PACKED_ICE;
		} else if (biome == Biome.JAGGED_PEAKS) {
			return Material.STONE;
		} else if (biome == Biome.STONY_PEAKS) {
			return Material.STONE;
		} else if (biome == Biome.CHERRY_GROVE) {
			return Material.CHERRY_LEAVES;
		}
		// Assume the void or new biomes
		else {
			return Material.BEDROCK;
		}
	}

	/**
	 * Provides the name of the biome.
	 * @param biome The biome.
	 * @return The name of the biome.
	 */
	private String getBiomeName(Biome biome) {
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
	private List<ItemStack> getResourcesByDominionAndBiome(Dominion dominion, Biome biome) {
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
			items.add(new ItemStack(Material.PODZOL, 32));
			items.add(new ItemStack(Material.GRASS_BLOCK, 32));
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
			if (item.getType() == Material.NAUTILUS_SHELL || item.getType() == Material.NETHERITE_SCRAP
					|| item.getType() == Material.TURTLE_SCUTE || item.getType() == Material.ARMADILLO_SCUTE
					|| item.getType() == Material.GHAST_TEAR || item.getType() == Material.DIAMOND
					|| item.isSimilar(new GodAppleFragment().getItem())) {
				continue;
			}

			if (dominionSize == 2) {
				multiplierItems.add(item);
				multiplierItems.add(item);
			} else if (dominionSize == 3) {
				multiplierItems.add(item);
				multiplierItems.add(item);
				multiplierItems.add(item);
			}
		}

		return multiplierItems;
	}

}
