package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

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
			itemMeta.setDisplayName(DominionUtils.getBiomeName(biomes.get(i)));
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

}
