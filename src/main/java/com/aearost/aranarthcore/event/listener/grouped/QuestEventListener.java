package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.QuestTaskType;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.QuestUtils;
import com.gmail.nossr50.mcMMO;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tracks all in-game events needed to advance quest progress.
 * Progress only counts in survival worlds (or arena for player-kill quests).
 */
public class QuestEventListener implements Listener {

    // Materials that count as logs
    private static final Set<Material> LOG_MATERIALS = Set.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG,
            Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_JUNGLE_LOG,
            Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
            Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG,
            Material.CRIMSON_STEM, Material.WARPED_STEM,
            Material.STRIPPED_CRIMSON_STEM, Material.STRIPPED_WARPED_STEM,
            Material.BAMBOO_BLOCK, Material.STRIPPED_BAMBOO_BLOCK,
            Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD,
            Material.JUNGLE_WOOD, Material.ACACIA_WOOD, Material.DARK_OAK_WOOD,
            Material.MANGROVE_WOOD, Material.CHERRY_WOOD,
            Material.STRIPPED_OAK_WOOD, Material.STRIPPED_SPRUCE_WOOD,
            Material.STRIPPED_BIRCH_WOOD, Material.STRIPPED_JUNGLE_WOOD,
            Material.STRIPPED_ACACIA_WOOD, Material.STRIPPED_DARK_OAK_WOOD,
            Material.STRIPPED_MANGROVE_WOOD, Material.STRIPPED_CHERRY_WOOD
    );

    // Materials that count as stone for "mine stone" tasks
    private static final Set<Material> STONE_MATERIALS = Set.of(
            Material.STONE, Material.DEEPSLATE, Material.COBBLESTONE,
            Material.COBBLED_DEEPSLATE
    );

    // Coal ore materials
    private static final Set<Material> COAL_ORE_MATERIALS = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE
    );

    // Iron ore materials
    private static final Set<Material> IRON_ORE_MATERIALS = Set.of(
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE
    );

    // Gold ore materials
    private static final Set<Material> GOLD_ORE_MATERIALS = Set.of(
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE
    );

    // Diamond ore materials
    private static final Set<Material> DIAMOND_ORE_MATERIALS = Set.of(
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE
    );

    // Crop materials that count as a harvest when broken
    private static final Set<Material> HARVESTABLE_CROP_MATERIALS = Set.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.NETHER_WART, Material.COCOA,
            Material.MELON, Material.PUMPKIN, Material.SWEET_BERRY_BUSH,
            Material.CAVE_VINES_PLANT, Material.CAVE_VINES
    );

    // Crop/seed materials placed on farmland that count as planting
    private static final Set<Material> PLANTABLE_CROP_MATERIALS = Set.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.NETHER_WART
    );

    // Material name suffixes for melee weapon detection
    private static final String SWORD_SUFFIX = "_SWORD";
    private static final String AXE_SUFFIX = "_AXE";

    // Cooked food materials
    private static final Set<Material> COOKED_FOOD_MATERIALS = Set.of(
            Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
            Material.COOKED_SALMON, Material.COOKED_COD, Material.BAKED_POTATO,
            Material.COOKED_MUTTON, Material.COOKED_RABBIT, Material.DRIED_KELP
    );

    // Plank materials
    private static final Set<Material> PLANK_MATERIALS = Set.of(
            Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
            Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
            Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS, Material.BAMBOO_PLANKS,
            Material.CRIMSON_PLANKS, Material.WARPED_PLANKS
    );

    public QuestEventListener(AranarthCore plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // -------------------------------------------------------------------------
    // Block Break — logs, stone, ores, sand, dirt, gravel, crops, ancient debris
    // -------------------------------------------------------------------------

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) {
            return;
        }

        Material type = e.getBlock().getType();

        // Ensures the block wasn't placed by the player
        boolean isEligible = mcMMO.getChunkManager().isEligible(e.getBlock());
        if (!isEligible) {
            return;
        }

        // Logs
        if (LOG_MATERIALS.contains(type)) {
            QuestUtils.updateProgress(player, QuestTaskType.BREAK_LOG, 1);
            return;
        }

        // Stone
        if (STONE_MATERIALS.contains(type)) {
            QuestUtils.updateProgress(player, QuestTaskType.MINE_STONE, 1);
            return;
        }

        // Coal ore
        if (COAL_ORE_MATERIALS.contains(type)) {
            QuestUtils.updateProgress(player, QuestTaskType.MINE_COAL_ORE, 1);
            return;
        }

        // Iron ore
        if (IRON_ORE_MATERIALS.contains(type)) {
            QuestUtils.updateProgress(player, QuestTaskType.MINE_IRON_ORE, 1);
            return;
        }

        // Gold ore
        if (GOLD_ORE_MATERIALS.contains(type)) {
            QuestUtils.updateProgress(player, QuestTaskType.MINE_GOLD_ORE, 1);
            return;
        }

        // Diamond ore
        if (DIAMOND_ORE_MATERIALS.contains(type)) {
            QuestUtils.updateProgress(player, QuestTaskType.MINE_DIAMOND, 1);
            return;
        }

        // Ancient debris
        if (type == Material.ANCIENT_DEBRIS) {
            QuestUtils.updateProgress(player, QuestTaskType.MINE_ANCIENT_DEBRIS, 1);
            return;
        }

        // Sand
        if (type == Material.SAND || type == Material.RED_SAND) {
            QuestUtils.updateProgress(player, QuestTaskType.BREAK_SAND, 1);
            return;
        }

        // Dirt variants
        if (type == Material.DIRT || type == Material.COARSE_DIRT || type == Material.ROOTED_DIRT
                || type == Material.PODZOL || type == Material.MYCELIUM) {
            QuestUtils.updateProgress(player, QuestTaskType.BREAK_DIRT, 1);
            return;
        }

        // Gravel
        if (type == Material.GRAVEL) {
            QuestUtils.updateProgress(player, QuestTaskType.BREAK_GRAVEL, 1);
            return;
        }

        // Crop harvest — check if mature
        if (HARVESTABLE_CROP_MATERIALS.contains(type)) {
            if (isMatureCrop(e.getBlock())) {
                QuestUtils.updateProgress(player, QuestTaskType.HARVEST_CROPS, 1);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Block Place — planting crops
    // -------------------------------------------------------------------------

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) {
            return;
        }

        Material placed = e.getBlockPlaced().getType();
        if (PLANTABLE_CROP_MATERIALS.contains(placed)) {
            QuestUtils.updateProgress(player, QuestTaskType.PLANT_CROPS, 1);
        }
    }

    // -------------------------------------------------------------------------
    // Entity Death — mob kills
    // -------------------------------------------------------------------------

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();

        // Player kill quest
        if (entity instanceof Player) {
            Player killer = getPlayerKiller(entity);
            if (killer != null && QuestUtils.isAllowedKillWorld(killer.getWorld().getName())) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_PLAYER, 1);
            }
            return;
        }

        Player killer = getPlayerKiller(entity);
        if (killer == null) {
            return;
        }

        String worldName = killer.getWorld().getName();
        if (!QuestUtils.isAllowedKillWorld(worldName)) {
            return;
        }

        // Hostile mob kills
        if (entity instanceof Enemy) {
            QuestUtils.updateProgress(killer, QuestTaskType.KILL_HOSTILE_MOB, 1);

            // Specific mob kills
            EntityType type = entity.getType();
            if (type == EntityType.SKELETON || type == EntityType.STRAY || type == EntityType.WITHER_SKELETON) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_SKELETON, 1);
            } else if (type == EntityType.ZOMBIE || type == EntityType.ZOMBIE_VILLAGER
                    || type == EntityType.HUSK || type == EntityType.DROWNED) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_ZOMBIE, 1);
            } else if (type == EntityType.CREEPER) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_CREEPER, 1);
            } else if (type == EntityType.ENDERMAN) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_ENDERMAN, 1);
            } else if (type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_SPIDER, 1);
            } else if (type == EntityType.WITCH) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_WITCH, 1);
            } else if (type == EntityType.BLAZE) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_BLAZE, 1);
            } else if (type == EntityType.GHAST) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_GHAST, 1);
            }
        }

        // Passive mob kills
        if (entity instanceof Animals) {
            QuestUtils.updateProgress(killer, QuestTaskType.KILL_PASSIVE_MOB, 1);
            EntityType passiveType = entity.getType();
            if (passiveType == EntityType.COW || passiveType == EntityType.MOOSHROOM) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_COW, 1);
            } else if (passiveType == EntityType.PIG) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_PIG, 1);
            } else if (passiveType == EntityType.CHICKEN) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_CHICKEN, 1);
            } else if (passiveType == EntityType.SHEEP) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_SHEEP, 1);
            } else if (passiveType == EntityType.RABBIT) {
                QuestUtils.updateProgress(killer, QuestTaskType.KILL_RABBIT, 1);
            }
        }

        // Weapon-specific kills
        if (isRangedKill(entity)) {
            QuestUtils.updateProgress(killer, QuestTaskType.KILL_WITH_RANGED, 1);
        } else if (isMeleeKill(entity, killer)) {
            QuestUtils.updateProgress(killer, QuestTaskType.KILL_WITH_MELEE, 1);
        }
    }

    // -------------------------------------------------------------------------
    // Fishing
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = e.getPlayer();
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) {
            return;
        }
        QuestUtils.updateProgress(player, QuestTaskType.FISH, 1);
    }

    // -------------------------------------------------------------------------
    // Cooking — track via furnace result slot click
    // -------------------------------------------------------------------------

    @EventHandler
    public void onFurnaceResultTake(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryType topType = e.getView().getTopInventory().getType();
        if (topType != InventoryType.FURNACE && topType != InventoryType.SMOKER) {
            return;
        }
        // Result slot is raw slot 2
        if (e.getRawSlot() != 2) {
            return;
        }

        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType().isAir()) {
            return;
        }
        if (!COOKED_FOOD_MATERIALS.contains(result.getType())) {
            return;
        }

        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) {
            return;
        }

        Material type = result.getType();

        if (e.isShiftClick()) {
            // Snapshot before to correctly count what actually fits into the inventory
            Map<Material, Integer> before = countRelevantItems(player, type);
            Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
                Map<Material, Integer> after = countRelevantItems(player, type);
                int gained = after.getOrDefault(type, 0) - before.getOrDefault(type, 0);
                if (gained > 0) {
                    QuestUtils.updateProgress(player, QuestTaskType.COOK_FOOD, gained);
                }
            }, 1L);
        } else {
            QuestUtils.updateProgress(player, QuestTaskType.COOK_FOOD, result.getAmount());
        }
    }

    // -------------------------------------------------------------------------
    // Crafting — uses before/after inventory snapshot to count shift-click correctly
    // -------------------------------------------------------------------------

    @EventHandler
    public void onCraftItem(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        String worldName = player.getWorld().getName();
        if (!QuestUtils.isCraftingAllowedWorld(worldName)) {
            return;
        }

        ItemStack result = e.getRecipe().getResult();
        Material type = result.getType();

        QuestTaskType taskType = null;
        if (PLANK_MATERIALS.contains(type)) {
            taskType = QuestTaskType.CRAFT_PLANKS;
        } else if (type == Material.TORCH || type == Material.SOUL_TORCH) {
            taskType = QuestTaskType.CRAFT_TORCHES;
        } else if (type == Material.BREAD) {
            taskType = QuestTaskType.CRAFT_BREAD;
        } else if (type == Material.GLASS || type == Material.GLASS_PANE) {
            taskType = QuestTaskType.CRAFT_GLASS;
        } else if (type == Material.IRON_INGOT) {
            taskType = QuestTaskType.CRAFT_IRON_INGOTS;
        } else if (type == Material.GOLDEN_APPLE) {
            taskType = QuestTaskType.CRAFT_GOLDEN_APPLE;
        }

        if (taskType == null) {
            return;
        }

        // Snapshot inventory before craft completes, then compare after 1 tick
        // to accurately count how many items were crafted (handles shift-click)
        final QuestTaskType finalTaskType = taskType;
        final Map<Material, Integer> before = countRelevantItems(player, type);

        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            Map<Material, Integer> after = countRelevantItems(player, type);
            int gained = after.getOrDefault(type, 0) - before.getOrDefault(type, 0);
            if (gained > 0) {
                QuestUtils.updateProgress(player, finalTaskType, gained);
            }
        }, 1L);
    }

    /**
     * Counts how many of the given material (and same-type variants for glass panes) the player has.
     */
    private Map<Material, Integer> countRelevantItems(Player player, Material target) {
        Map<Material, Integer> counts = new HashMap<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }
            if (item.getType() == target) {
                counts.merge(target, item.getAmount(), Integer::sum);
            }
        }
        return counts;
    }

    // -------------------------------------------------------------------------
    // Animal Breeding
    // -------------------------------------------------------------------------

    @EventHandler
    public void onEntityBreed(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player player)) {
            return;
        }
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) {
            return;
        }
        QuestUtils.updateProgress(player, QuestTaskType.BREED_ANIMALS, 1);
    }

    // -------------------------------------------------------------------------
    // Travel
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        // Only count when the player moves to a new block position
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        Player player = e.getPlayer();
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) {
            return;
        }

        QuestUtils.updateProgress(player, QuestTaskType.TRAVEL_BLOCKS, 1);
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Attempts to find the player who killed the entity.
     * Handles both direct melee kills and arrow/projectile kills.
     */
    private Player getPlayerKiller(LivingEntity entity) {
        Player directKiller = entity.getKiller();
        if (directKiller != null) {
            return directKiller;
        }

        EntityDamageEvent lastDamage = entity.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent dmgByEntity) {
            Entity damager = dmgByEntity.getDamager();
            if (damager instanceof Player p) {
                return p;
            }
            if (damager instanceof Projectile proj) {
                ProjectileSource shooter = proj.getShooter();
                if (shooter instanceof Player p) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Returns true if the entity's killing blow was delivered by a ranged attack:
     * bow/crossbow arrow, spectral arrow, thrown trident, or thrown potion.
     */
    private boolean isRangedKill(LivingEntity entity) {
        EntityDamageEvent lastDamage = entity.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent dmgByEntity) {
            Entity damager = dmgByEntity.getDamager();
            return damager instanceof AbstractArrow
                    || damager instanceof Trident
                    || damager instanceof ThrownPotion;
        }
        return false;
    }

    /**
     * Returns true if the entity's killing blow was delivered by a melee weapon:
     * sword, axe, mace, or trident (used in melee, not thrown).
     */
    private boolean isMeleeKill(LivingEntity entity, Player killer) {
        EntityDamageEvent lastDamage = entity.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent dmgByEntity) {
            Entity damager = dmgByEntity.getDamager();
            // Thrown trident or any projectile = not melee
            if (damager instanceof Projectile) {
                return false;
            }
        }
        String matName = killer.getInventory().getItemInMainHand().getType().name();
        return matName.endsWith(SWORD_SUFFIX)
                || matName.endsWith(AXE_SUFFIX)
                || matName.equals("MACE")
                || matName.equals("TRIDENT");
    }

    /**
     * Returns true if the block is a mature crop.
     */
    private boolean isMatureCrop(Block block) {
        Material type = block.getType();
        // Melon and pumpkin blocks don't have Ageable — they are always harvestable
        if (type == Material.MELON || type == Material.PUMPKIN) {
            return true;
        }

        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }
}
