package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Set;

public class RecipeSawmill {

	private static final Set<Material> WOOD_BLOCKS = EnumSet.of(
		Material.OAK_LOG,          Material.OAK_WOOD,          Material.STRIPPED_OAK_LOG,     Material.STRIPPED_OAK_WOOD,
		Material.SPRUCE_LOG,       Material.SPRUCE_WOOD,        Material.STRIPPED_SPRUCE_LOG,  Material.STRIPPED_SPRUCE_WOOD,
		Material.BIRCH_LOG,        Material.BIRCH_WOOD,         Material.STRIPPED_BIRCH_LOG,   Material.STRIPPED_BIRCH_WOOD,
		Material.JUNGLE_LOG,       Material.JUNGLE_WOOD,        Material.STRIPPED_JUNGLE_LOG,  Material.STRIPPED_JUNGLE_WOOD,
		Material.ACACIA_LOG,       Material.ACACIA_WOOD,        Material.STRIPPED_ACACIA_LOG,  Material.STRIPPED_ACACIA_WOOD,
		Material.DARK_OAK_LOG,     Material.DARK_OAK_WOOD,      Material.STRIPPED_DARK_OAK_LOG,Material.STRIPPED_DARK_OAK_WOOD,
		Material.MANGROVE_LOG,     Material.MANGROVE_WOOD,      Material.STRIPPED_MANGROVE_LOG,Material.STRIPPED_MANGROVE_WOOD,
		Material.CHERRY_LOG,       Material.CHERRY_WOOD,        Material.STRIPPED_CHERRY_LOG,  Material.STRIPPED_CHERRY_WOOD,
		Material.PALE_OAK_LOG,     Material.PALE_OAK_WOOD,      Material.STRIPPED_PALE_OAK_LOG,Material.STRIPPED_PALE_OAK_WOOD,
		Material.CRIMSON_STEM,     Material.CRIMSON_HYPHAE,     Material.STRIPPED_CRIMSON_STEM,Material.STRIPPED_CRIMSON_HYPHAE,
		Material.WARPED_STEM,      Material.WARPED_HYPHAE,      Material.STRIPPED_WARPED_STEM, Material.STRIPPED_WARPED_HYPHAE,
		Material.BAMBOO_BLOCK,     Material.STRIPPED_BAMBOO_BLOCK
	);

	public static boolean isWoodBlock(Material material) {
		return WOOD_BLOCKS.contains(material);
	}

	public RecipeSawmill(Plugin plugin) {
		registerAll(plugin);
	}

	private void registerAll(Plugin plugin) {
		Object[][] woodTypes = {
			{Material.OAK_LOG, Material.OAK_WOOD, Material.STRIPPED_OAK_LOG, Material.STRIPPED_OAK_WOOD, Material.OAK_PLANKS, Material.OAK_STAIRS, Material.OAK_SLAB, Material.OAK_DOOR, Material.OAK_TRAPDOOR, Material.OAK_PRESSURE_PLATE, Material.OAK_BUTTON},
			{Material.SPRUCE_LOG, Material.SPRUCE_WOOD, Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_SPRUCE_WOOD, Material.SPRUCE_PLANKS, Material.SPRUCE_STAIRS, Material.SPRUCE_SLAB, Material.SPRUCE_DOOR, Material.SPRUCE_TRAPDOOR, Material.SPRUCE_PRESSURE_PLATE, Material.SPRUCE_BUTTON},
			{Material.BIRCH_LOG, Material.BIRCH_WOOD, Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_BIRCH_WOOD, Material.BIRCH_PLANKS, Material.BIRCH_STAIRS, Material.BIRCH_SLAB, Material.BIRCH_DOOR, Material.BIRCH_TRAPDOOR, Material.BIRCH_PRESSURE_PLATE, Material.BIRCH_BUTTON},
			{Material.JUNGLE_LOG, Material.JUNGLE_WOOD, Material.STRIPPED_JUNGLE_LOG, Material.STRIPPED_JUNGLE_WOOD, Material.JUNGLE_PLANKS, Material.JUNGLE_STAIRS, Material.JUNGLE_SLAB, Material.JUNGLE_DOOR, Material.JUNGLE_TRAPDOOR, Material.JUNGLE_PRESSURE_PLATE, Material.JUNGLE_BUTTON},
			{Material.ACACIA_LOG, Material.ACACIA_WOOD, Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_ACACIA_WOOD, Material.ACACIA_PLANKS, Material.ACACIA_STAIRS, Material.ACACIA_SLAB, Material.ACACIA_DOOR, Material.ACACIA_TRAPDOOR, Material.ACACIA_PRESSURE_PLATE, Material.ACACIA_BUTTON},
			{Material.DARK_OAK_LOG, Material.DARK_OAK_WOOD, Material.STRIPPED_DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_WOOD, Material.DARK_OAK_PLANKS, Material.DARK_OAK_STAIRS, Material.DARK_OAK_SLAB, Material.DARK_OAK_DOOR, Material.DARK_OAK_TRAPDOOR, Material.DARK_OAK_PRESSURE_PLATE, Material.DARK_OAK_BUTTON},
			{Material.MANGROVE_LOG, Material.MANGROVE_WOOD, Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_MANGROVE_WOOD, Material.MANGROVE_PLANKS, Material.MANGROVE_STAIRS, Material.MANGROVE_SLAB, Material.MANGROVE_DOOR, Material.MANGROVE_TRAPDOOR, Material.MANGROVE_PRESSURE_PLATE, Material.MANGROVE_BUTTON},
			{Material.CHERRY_LOG, Material.CHERRY_WOOD, Material.STRIPPED_CHERRY_LOG, Material.STRIPPED_CHERRY_WOOD, Material.CHERRY_PLANKS, Material.CHERRY_STAIRS, Material.CHERRY_SLAB, Material.CHERRY_DOOR, Material.CHERRY_TRAPDOOR, Material.CHERRY_PRESSURE_PLATE, Material.CHERRY_BUTTON},
			{Material.PALE_OAK_LOG, Material.PALE_OAK_WOOD, Material.STRIPPED_PALE_OAK_LOG, Material.STRIPPED_PALE_OAK_WOOD, Material.PALE_OAK_PLANKS, Material.PALE_OAK_STAIRS, Material.PALE_OAK_SLAB, Material.PALE_OAK_DOOR, Material.PALE_OAK_TRAPDOOR, Material.PALE_OAK_PRESSURE_PLATE, Material.PALE_OAK_BUTTON},
			{Material.CRIMSON_STEM, Material.CRIMSON_HYPHAE, Material.STRIPPED_CRIMSON_STEM, Material.STRIPPED_CRIMSON_HYPHAE, Material.CRIMSON_PLANKS, Material.CRIMSON_STAIRS, Material.CRIMSON_SLAB, Material.CRIMSON_DOOR, Material.CRIMSON_TRAPDOOR, Material.CRIMSON_PRESSURE_PLATE, Material.CRIMSON_BUTTON},
			{Material.WARPED_STEM, Material.WARPED_HYPHAE, Material.STRIPPED_WARPED_STEM, Material.STRIPPED_WARPED_HYPHAE, Material.WARPED_PLANKS, Material.WARPED_STAIRS, Material.WARPED_SLAB, Material.WARPED_DOOR, Material.WARPED_TRAPDOOR, Material.WARPED_PRESSURE_PLATE, Material.WARPED_BUTTON},
		};

		for (Object[] wood : woodTypes) {
			Material log       = (Material) wood[0];
			Material woodMat   = (Material) wood[1];
			Material stripLog  = (Material) wood[2];
			Material stripWood = (Material) wood[3];
			Material planks    = (Material) wood[4];
			Material stair     = (Material) wood[5];
			Material slab      = (Material) wood[6];
			Material door      = (Material) wood[7];
			Material trapdoor  = (Material) wood[8];
			Material plate     = (Material) wood[9];
			Material button    = (Material) wood[10];

			String prefix = log.name().toLowerCase();

			// Log as input - crafting planks, stripped log, shelf
			add(plugin, prefix + "_planks",     planks, 6, log);
			add(plugin, prefix + "_striplog",   stripLog, 1, log);
			add(plugin, prefix + "_shelf",      Material.CHISELED_BOOKSHELF, 1, log);

			// Wood as input - crafting planks, stripped wood, shelf
			add(plugin, woodMat.name().toLowerCase() + "_planks",    planks, 6, woodMat);
			add(plugin, woodMat.name().toLowerCase() + "_stripwood", stripWood, 1, woodMat);
			add(plugin, woodMat.name().toLowerCase() + "_shelf",     Material.CHISELED_BOOKSHELF, 1, woodMat);

			// Stripped log as input - crafting planks, shelf
			add(plugin, stripLog.name().toLowerCase() + "_planks", planks, 6, stripLog);
			add(plugin, stripLog.name().toLowerCase() + "_shelf",  Material.CHISELED_BOOKSHELF, 1, stripLog);

			// Stripped wood as input - crafting planks, shelf
			add(plugin, stripWood.name().toLowerCase() + "_planks", planks, 6, stripWood);
			add(plugin, stripWood.name().toLowerCase() + "_shelf",  Material.CHISELED_BOOKSHELF, 1, stripWood);

			// Planks as input - crafting wood products
			add(plugin, planks.name().toLowerCase() + "_stair",    stair, 1, planks);
			add(plugin, planks.name().toLowerCase() + "_slab",     slab, 2, planks);
			add(plugin, planks.name().toLowerCase() + "_door",     door, 1, planks);
			add(plugin, planks.name().toLowerCase() + "_trapdoor", trapdoor, 1, planks);
			add(plugin, planks.name().toLowerCase() + "_plate",    plate, 1, planks);
			add(plugin, planks.name().toLowerCase() + "_button",   button, 2, planks);
		}

		// Bamboo
		add(plugin, "bamboo_block_planks",    Material.BAMBOO_PLANKS, 6, Material.BAMBOO_BLOCK);
		add(plugin, "bamboo_block_stripblock",Material.STRIPPED_BAMBOO_BLOCK, 1, Material.BAMBOO_BLOCK);
		add(plugin, "bamboo_block_shelf",     Material.CHISELED_BOOKSHELF, 1, Material.BAMBOO_BLOCK);
		add(plugin, "stripped_bamboo_planks", Material.BAMBOO_PLANKS, 6, Material.STRIPPED_BAMBOO_BLOCK);
		add(plugin, "stripped_bamboo_shelf",  Material.CHISELED_BOOKSHELF, 1, Material.STRIPPED_BAMBOO_BLOCK);
		add(plugin, "bamboo_planks_stair",    Material.BAMBOO_STAIRS, 1, Material.BAMBOO_PLANKS);
		add(plugin, "bamboo_planks_slab",     Material.BAMBOO_SLAB, 2, Material.BAMBOO_PLANKS);
		add(plugin, "bamboo_planks_door",     Material.BAMBOO_DOOR, 1, Material.BAMBOO_PLANKS);
		add(plugin, "bamboo_planks_trapdoor", Material.BAMBOO_TRAPDOOR, 1, Material.BAMBOO_PLANKS);
		add(plugin, "bamboo_planks_plate",    Material.BAMBOO_PRESSURE_PLATE, 1, Material.BAMBOO_PLANKS);
		add(plugin, "bamboo_planks_button",   Material.BAMBOO_BUTTON, 2, Material.BAMBOO_PLANKS);
	}

	private void add(Plugin plugin, String key, Material output, int count, Material input) {
		StonecuttingRecipe recipe = new StonecuttingRecipe(
			new NamespacedKey(plugin, "sawmill_" + key),
			new ItemStack(output, count),
			input
		);
		Bukkit.addRecipe(recipe);
	}

}
