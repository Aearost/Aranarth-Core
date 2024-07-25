package com.aearost.aranarthcore.objects;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * A comparator that is used to sort the inventory in chests.
 */
public class ChestItemComparator implements Comparator<ItemStack> {

    /**
     * Used to sort the two input items.
     * @param a the first item to be compared.
     * @param b the second item to be compared.
     * @return A value representing which item is to be first.
     */
	@Override
	public int compare(ItemStack a, ItemStack b) {
		
		boolean isBlockA = a.getType().isBlock();
        boolean isBlockB = b.getType().isBlock();

        if (isBlockA && isBlockB) {
            return compareBlocks(a, b);
        } else if (isBlockA) {
            return -1;
        } else if (isBlockB) {
            return 1;
        } else {
            return compareItems(a, b);
        }
	}

    /**
     * Used to sort the order of items prioritizing a specific order of blocks.
     * @param a the first item to be compared.
     * @param b the second item to be compared.
     * @return A value representing which item is to be first.
     */
	private int compareBlocks(ItemStack a, ItemStack b) {
		
		List<Material> order = Arrays.asList(
				Material.GRASS_BLOCK,
                Material.DIRT,
                Material.COARSE_DIRT,
                Material.ROOTED_DIRT,
                Material.PODZOL,
                Material.MYCELIUM,
                Material.MUD,
                Material.CLAY,
                Material.SAND,
                Material.RED_SAND,
                Material.GRAVEL,
                Material.STONE,
                Material.COBBLESTONE,
                Material.MOSSY_COBBLESTONE,
                Material.STONE_BRICKS,
                Material.MOSSY_STONE_BRICKS,
                Material.DEEPSLATE,
                Material.POLISHED_DEEPSLATE,
                Material.DEEPSLATE_BRICKS,
                Material.DEEPSLATE_TILES,
                Material.COBBLED_DEEPSLATE,
                Material.GRANITE,
                Material.POLISHED_GRANITE,
                Material.DIORITE,
                Material.POLISHED_DIORITE,
                Material.ANDESITE,
                Material.POLISHED_ANDESITE,
                Material.CALCITE,
                Material.TUFF,
                Material.POLISHED_TUFF,
                Material.TUFF_BRICKS,
                Material.DRIPSTONE_BLOCK,
                Material.SANDSTONE,
                Material.SMOOTH_SANDSTONE,
                Material.RED_SANDSTONE,
                Material.SMOOTH_RED_SANDSTONE,
                Material.PACKED_MUD,
                Material.MUD_BRICKS,
                Material.OBSIDIAN,
                Material.CRYING_OBSIDIAN,
                Material.AMETHYST_BLOCK,
                Material.ICE,
                Material.PACKED_ICE,
                Material.BLUE_ICE,
                Material.PRISMARINE,
                Material.PRISMARINE_BRICKS,
                Material.DARK_PRISMARINE,
                Material.SEA_LANTERN,
                Material.NETHERRACK,
                Material.NETHER_BRICK,
                Material.RED_NETHER_BRICKS,
                Material.BASALT,
                Material.SMOOTH_BASALT,
                Material.POLISHED_BASALT,
                Material.BLACKSTONE,
                Material.GILDED_BLACKSTONE,
                Material.POLISHED_BLACKSTONE,
                Material.POLISHED_BLACKSTONE_BRICKS,
                Material.SOUL_SAND,
                Material.SOUL_SOIL,
                Material.GLOWSTONE,
                Material.END_STONE,
                Material.END_STONE_BRICKS,
                Material.PURPUR_BLOCK,
                Material.PURPUR_PILLAR,
                Material.OAK_LOG,
                Material.OAK_WOOD,
                Material.BIRCH_LOG,
                Material.BIRCH_WOOD,
                Material.SPRUCE_LOG,
                Material.SPRUCE_WOOD,
                Material.JUNGLE_LOG,
                Material.JUNGLE_WOOD,
                Material.DARK_OAK_LOG,
                Material.DARK_OAK_WOOD,
                Material.ACACIA_LOG,
                Material.ACACIA_WOOD,
                Material.MANGROVE_LOG,
                Material.MANGROVE_WOOD,
                Material.CHERRY_LOG,
                Material.CHERRY_WOOD,
                Material.BAMBOO_BLOCK,
                Material.CRIMSON_STEM,
                Material.CRIMSON_HYPHAE,
                Material.WARPED_STEM,
                Material.WARPED_HYPHAE
        );
		
        int indexA = order.indexOf(a.getType());
        int indexB = order.indexOf(b.getType());

        // If not defined in the list already
        if (indexA == -1) {
        	indexA = Integer.MAX_VALUE;
        }
        if (indexB == -1) {
        	indexB = Integer.MAX_VALUE;
        }

        return Integer.compare(indexA, indexB);
    }

    /**
     * Used to sort the order of items prioritizing a specific order of items.
     * @param a the first item to be compared.
     * @param b the second item to be compared.
     * @return A value representing which item is to be first.
     */
    private int compareItems(ItemStack a, ItemStack b) {
    	List<Material> order = Arrays.asList(
    			Material.GRASS_BLOCK,
                Material.DIRT,
                Material.COARSE_DIRT
        );
    	
    	int indexA = order.indexOf(a.getType());
        int indexB = order.indexOf(b.getType());

        // If not defined in the list already
        if (indexA == -1) {
        	indexA = Integer.MAX_VALUE;
        }
        if (indexB == -1) {
        	indexB = Integer.MAX_VALUE;
        }
        
        return Integer.compare(indexA, indexB);
    }

}
