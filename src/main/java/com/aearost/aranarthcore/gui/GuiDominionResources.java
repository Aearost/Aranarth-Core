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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiDominionResources {

	public static final int CONTENT_PER_PAGE = 36; // max 4 content rows per page
	public static final Map<UUID, Integer> playerPage = new HashMap<>();

	private final Player player;
	private final int page;
	private final Inventory initializedGui;

	public GuiDominionResources(Player player) {
		this(player, 0);
	}

	public GuiDominionResources(Player player, int page) {
		this.player = player;
		this.page = page;
		this.initializedGui = initializeGui(player, page);
	}

	public void openGui() {
		playerPage.put(player.getUniqueId(), page);
		player.closeInventory();
		player.openInventory(initializedGui);
	}

	private Inventory initializeGui(Player player, int page) {
		Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		List<Biome> biomes = DominionUtils.getResourceClaimTypes(dominion);

		int totalBiomes = biomes.size();
		int totalPages = Math.max(1, (int) Math.ceil((double) totalBiomes / CONTENT_PER_PAGE));
		int safePage = Math.min(page, totalPages - 1);

		// Determine content rows and total size
		int contentRows;
		if (totalPages > 1) {
			// Paginated - always use 4 content rows (54 total)
			contentRows = 4;
		} else {
			contentRows = Math.max(1, (int) Math.ceil((double) totalBiomes / 9.0));
		}
		int size = contentRows * 9 + 18;

		Inventory gui = Bukkit.getServer().createInventory(player, size,
				ChatUtils.translateToColor("&e" + dominion.getName() + "'s &rResources"));

		ItemStack filler = makeFiller();

		// Top filler row
		for (int i = 0; i < 9; i++) {
			gui.setItem(i, filler);
		}
		// Bottom filler row
		for (int i = size - 9; i < size; i++) {
			gui.setItem(i, filler);
		}

		// Biome items in content area
		int start = safePage * CONTENT_PER_PAGE;
		int end = Math.min(start + CONTENT_PER_PAGE, totalBiomes);
		int slot = 9;
		for (int i = start; i < end; i++) {
			Biome biome = biomes.get(i);
			ItemStack item = new ItemStack(getIconByBiome(biome));
			ItemMeta itemMeta = item.getItemMeta();
			itemMeta.setDisplayName(ChatUtils.translateToColor("&l" + DominionUtils.getBiomeName(biome)));
			itemMeta.setLore(Arrays.asList(
					ChatUtils.translateToColor("&7Right-click to &epreview &7resources"),
					ChatUtils.translateToColor("&7Left-click to &eclaim &7resources")
			));
			item.setItemMeta(itemMeta);
			gui.setItem(slot++, item);
		}

		// Pagination buttons in bottom row
		if (totalPages > 1) {
			if (safePage > 0) {
				gui.setItem(size - 9, makeNavButton(Material.ARROW, "&aPrevious Page",
						"&7Page " + safePage + " / " + totalPages));
			}
			if (safePage < totalPages - 1) {
				gui.setItem(size - 1, makeNavButton(Material.ARROW, "&aNext Page",
						"&7Page " + (safePage + 2) + " / " + totalPages));
			}
		}

		return gui;
	}

	private ItemStack makeFiller() {
		ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = pane.getItemMeta();
		meta.setDisplayName(" ");
		pane.setItemMeta(meta);
		return pane;
	}

	private ItemStack makeNavButton(Material mat, String name, String loreText) {
		ItemStack item = new ItemStack(mat);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatUtils.translateToColor(name));
		meta.setLore(List.of(ChatUtils.translateToColor(loreText)));
		item.setItemMeta(meta);
		return item;
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
