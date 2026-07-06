package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.BlockSortOrder;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the chest sort logic.
 */
public class ChestSort {

    private static final int TIER_BLOCK_UNLISTED = BlockSortOrder.values().length;
    private static final int TIER_ORES = 1_000;
    private static final int TIER_CLUSTERS = 2_000;
    private static final int TIER_INGOTS = 3_000;
    private static final int TIER_FARMING = 4_000;
    private static final int TIER_FOOD = 5_000;
    private static final int TIER_MOB_DROPS = 6_000;
    private static final int TIER_ARROWHEADS = 7_000;
    private static final int TIER_ARROWS = 8_000;
    private static final int TIER_ARROWS_CUSTOM = 8_100;
    private static final int TIER_SPECIAL = 9_000;
    private static final int TIER_POTIONS = 10_000;
    private static final int TIER_DISCS = 11_000;
    private static final int TIER_WEAPONS = 12_000;
    private static final int TIER_TOOLS = 13_000;
    private static final int TIER_ARMOR = 14_000;
    private static final int TIER_ARMOR_CUSTOM = 14_100;
    private static final int TIER_UNLISTED = Integer.MAX_VALUE;

    private static final Map<Material, Integer> SORT_MAP = new EnumMap<>(Material.class);

    static {
        // --- Ores / Raw Materials --------------------------------------------
        register(TIER_ORES,
                Material.COAL,
                Material.RAW_COPPER, Material.COPPER_NUGGET, Material.COPPER_INGOT,
                Material.RAW_IRON, Material.IRON_NUGGET, Material.IRON_INGOT,
                Material.RAW_GOLD, Material.GOLD_NUGGET, Material.GOLD_INGOT,
                Material.QUARTZ, Material.AMETHYST_SHARD,
                Material.REDSTONE, Material.LAPIS_LAZULI, Material.EMERALD,
                Material.DIAMOND, Material.NETHERITE_SCRAP, Material.NETHERITE_INGOT
        );

        // --- Farming / Seeds -------------------------------------------------
        register(TIER_FARMING,
                Material.WHEAT, Material.WHEAT_SEEDS,
                Material.CARROT, Material.GOLDEN_CARROT,
                Material.POTATO, Material.BAKED_POTATO, Material.POISONOUS_POTATO,
                Material.BEETROOT, Material.BEETROOT_SEEDS,
                Material.PUMPKIN_SEEDS, Material.MELON_SEEDS,
                Material.SUGAR, Material.COCOA_BEANS,
                Material.BONE_MEAL, Material.NETHER_WART
        );

        // --- Food ------------------------------------------------------------
        register(TIER_FOOD,
                Material.APPLE, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE,
                Material.BREAD, Material.COOKIE, Material.PUMPKIN_PIE, Material.CAKE,
                Material.MELON_SLICE, Material.GLISTERING_MELON_SLICE,
                Material.HONEY_BOTTLE,
                Material.MUSHROOM_STEW, Material.RABBIT_STEW,
                Material.BEETROOT_SOUP, Material.SUSPICIOUS_STEW,
                Material.BEEF, Material.PORKCHOP, Material.CHICKEN,
                Material.MUTTON, Material.RABBIT,
                Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH,
                Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
                Material.COOKED_MUTTON, Material.COOKED_RABBIT,
                Material.COOKED_COD, Material.COOKED_SALMON
        );

        // --- Mob Drops -------------------------------------------------------
        register(TIER_MOB_DROPS,
                Material.ROTTEN_FLESH,
                Material.BONE, Material.FEATHER, Material.LEATHER,
                Material.RABBIT_HIDE, Material.RABBIT_FOOT,
                Material.PHANTOM_MEMBRANE, Material.SHULKER_SHELL,
                Material.SPIDER_EYE, Material.FERMENTED_SPIDER_EYE,
                Material.STRING, Material.INK_SAC, Material.GLOW_INK_SAC,
                Material.SLIME_BALL, Material.MAGMA_CREAM,
                Material.GUNPOWDER, Material.BLAZE_ROD, Material.BLAZE_POWDER,
                Material.GHAST_TEAR,
                Material.ENDER_PEARL, Material.ENDER_EYE,
                Material.CHORUS_FRUIT, Material.POPPED_CHORUS_FRUIT
        );

        // --- Vanilla Arrows --------------------------------------------------
        register(TIER_ARROWS,
                Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW
        );

        // --- Special Drops ---------------------------------------------------
        register(TIER_SPECIAL,
                Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.NAUTILUS_SHELL, Material.HEART_OF_THE_SEA,
                Material.NETHER_BRICK, Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS,
                Material.CLAY_BALL, Material.BRICK, Material.HONEYCOMB,
                Material.TURTLE_SCUTE, Material.ARMADILLO_SCUTE,
                Material.NETHER_STAR, Material.TOTEM_OF_UNDYING,
                Material.BREEZE_ROD, Material.WIND_CHARGE, Material.HEAVY_CORE,
                Material.DRAGON_BREATH
        );
        // SCUTE was the pre-1.21 name for TURTLE_SCUTE; map it to the same slot
        registerByName(TIER_SPECIAL + 10, "SCUTE");

        // --- Potions ---------------------------------------------------------
        register(TIER_POTIONS,
                Material.GLASS_BOTTLE,
                Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION,
                Material.OMINOUS_BOTTLE
        );

        // --- Music Discs -----------------------------------------------------
        register(TIER_DISCS,
                Material.MUSIC_DISC_13, Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS,
                Material.MUSIC_DISC_CHIRP, Material.MUSIC_DISC_FAR, Material.MUSIC_DISC_MALL,
                Material.MUSIC_DISC_MELLOHI, Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD,
                Material.MUSIC_DISC_WARD, Material.MUSIC_DISC_11, Material.MUSIC_DISC_WAIT,
                Material.MUSIC_DISC_OTHERSIDE, Material.MUSIC_DISC_5, Material.MUSIC_DISC_PIGSTEP,
                Material.MUSIC_DISC_RELIC, Material.MUSIC_DISC_CREATOR,
                Material.MUSIC_DISC_CREATOR_MUSIC_BOX, Material.MUSIC_DISC_PRECIPICE
        );

        // --- Weapons ---------------------------------------------------------
        register(TIER_WEAPONS,
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
                Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.MACE
        );

        // --- Tools -----------------------------------------------------------
        register(TIER_TOOLS,
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
                Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE,
                Material.SHEARS, Material.FLINT_AND_STEEL, Material.FISHING_ROD,
                Material.CARROT_ON_A_STICK, Material.WARPED_FUNGUS_ON_A_STICK,
                Material.SPYGLASS, Material.BRUSH
        );

        // --- Armor (vanilla) -------------------------------------------------
        register(TIER_ARMOR,
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
                Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
                Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE,
                Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE,
                Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE,
                Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                Material.TURTLE_HELMET, Material.WOLF_ARMOR,
                Material.ELYTRA, Material.SHIELD
        );
    }

    /**
     * Registers materials in order, each at baseTier + their position index.
     */
    private static void register(int baseTier, Material... materials) {
        for (int i = 0; i < materials.length; i++) {
            SORT_MAP.put(materials[i], baseTier + i);
        }
    }

    /**
     * Registers a material by name, silently ignoring unknown names (version compatibility).
     */
    private static void registerByName(int order, String name) {
        try {
            SORT_MAP.put(Material.valueOf(name), order);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void execute(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!AranarthUtils.isContainerBlock(e.getClickedBlock())) {
            return;
        }
        if (e.getPlayer().getGameMode() != GameMode.SURVIVAL || !e.getPlayer().isSneaking()) {
            return;
        }

        Player player = e.getPlayer();
        Block block = e.getClickedBlock();

        if (!player.hasPermission("aranarth.inventory")) {
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to sort chests!"));
            return;
        }

        // Deny sort if the container is locked and the player is not trusted (mirrors attemptOpen logic)
        LockedContainer lockedContainer = AranarthUtils.getLockedContainerAtBlock(block);
        if (lockedContainer != null) {
            UUID playerUuid = player.getUniqueId();
            boolean isTrusted = lockedContainer.getTrusted().contains(playerUuid);
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(playerUuid);
            if (!isTrusted && !aranarthPlayer.isInAdminMode()) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to sort this container!"));
                return;
            }
        }

        BlockState state = block.getState();
        Container container = (Container) state;
        Inventory inventory = container.getInventory();
        if (inventory.getHolder() instanceof DoubleChest doubleChest) {
            inventory = doubleChest.getInventory();
        }

        // Collect clones of all real items (skip null/air slots)
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }

        items = stackItems(items);
        items.sort(Comparator.comparingInt(this::getSortOrder)
                .thenComparing(item -> item.getType().name())
                .thenComparing(this::getPotionSubKey));

        inventory.clear();
        for (int i = 0; i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
        }

        player.sendMessage(ChatUtils.chatMessage("&7The chest has been sorted!"));
        player.playSound(player, Sound.UI_STONECUTTER_TAKE_RESULT, 1F, 1F);
    }

    /**
     * Consolidates partial stacks of the same item type into full stacks.
     * Works on cloned items, so the original inventory is not touched until we write back.
     */
    private List<ItemStack> stackItems(List<ItemStack> items) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack incoming : items) {
            int remaining = incoming.getAmount();
            for (ItemStack existing : result) {
                if (remaining <= 0) {
                    break;
                }
                if (!existing.isSimilar(incoming)) {
                    continue;
                }
                int space = existing.getMaxStackSize() - existing.getAmount();
                if (space <= 0) {
                    continue;
                }
                int transfer = Math.min(space, remaining);
                existing.setAmount(existing.getAmount() + transfer);
                remaining -= transfer;
            }
            if (remaining > 0) {
                ItemStack leftover = incoming.clone();
                leftover.setAmount(remaining);
                result.add(leftover);
            }
        }
        return result;
    }

    /**
     * Returns the primary sort key for an item.
     * <p>
     * Order:
     * 1. BlockSortOrder items (ordinal, 0-based)
     * 2. Unlisted blocks
     * 3. Custom clusters  (PDC: CustomKeys.CLUSTER)
     * 4. Aranarthium ingots (PDC: CustomKeys.ARANARTHIUM_INGOT)
     * 5. Vanilla items per SORT_MAP (ores → farming → food → mob drops →
     * arrows → special → potions → discs → weapons → tools → armor)
     * 6. Custom arrowheads (PDC: CustomKeys.ARROW_HEAD)
     * 7. Custom arrows     (PDC: CustomKeys.ARROW)
     * 8. Custom armor      (PDC: CustomKeys.ARMOR_TYPE)
     * 9. Everything else (unlisted)
     */
    private int getSortOrder(ItemStack item) {
        Material mat = item.getType();

        // --- Block-tier items ------------------------------------------------
        try {
            return BlockSortOrder.valueOf(mat.name()).ordinal();
        } catch (IllegalArgumentException ignored) {
        }
        if (mat.isBlock()) {
            return TIER_BLOCK_UNLISTED;
        }

        // --- Custom items identified by PDC keys -----------------------------
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(CustomKeys.CLUSTER, PersistentDataType.STRING)) {
                return TIER_CLUSTERS;
            }
            if (pdc.has(CustomKeys.ARANARTHIUM_INGOT, PersistentDataType.STRING)) {
                return TIER_INGOTS;
            }
            if (pdc.has(CustomKeys.ARROW_HEAD, PersistentDataType.STRING)) {
                return TIER_ARROWHEADS;
            }
            if (pdc.has(CustomKeys.ARROW, PersistentDataType.STRING)) {
                return TIER_ARROWS_CUSTOM;
            }
            if (pdc.has(CustomKeys.ARMOR_TYPE, PersistentDataType.STRING)) {
                return TIER_ARMOR_CUSTOM;
            }
        }

        // --- Vanilla items ---------------------------------------------------
        Integer order = SORT_MAP.get(mat);
        if (order != null) {
            return order;
        }

        return TIER_UNLISTED;
    }

    /**
     * Returns a sub-ordering key used as the third comparator level.
     * Empty string for non-potions; otherwise groups potions by their first effect.
     * Uses string keys (e.g. "strength", "speed") since PotionEffectType is not an enum.
     */
    private String getPotionSubKey(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof PotionMeta potionMeta)) {
            return "";
        }
        // Vanilla base type (covers the vast majority of potions)
        PotionType base = potionMeta.getBasePotionType();
        if (base != null) {
            return base.getKey().getKey();
        }
        // Custom-effect potions: sort by first effect type then amplifier
        List<PotionEffect> effects = potionMeta.getCustomEffects();
        if (effects.isEmpty()) {
            return "";
        }
        PotionEffect first = effects.get(0);
        return first.getType().getKey().getKey() + first.getAmplifier();
    }
}
