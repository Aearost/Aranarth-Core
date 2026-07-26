package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.JobUtils;
import com.dre.brewery.api.events.brew.BrewModifyEvent;
import com.gmail.nossr50.mcMMO;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JobEventListener implements Listener {

    // Materials that count as logs for lumberjack
    private static final Set<Material> LOG_MATERIALS = Set.of(
        Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
        Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
        Material.MANGROVE_LOG, Material.CHERRY_LOG,
        Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD,
        Material.JUNGLE_WOOD, Material.ACACIA_WOOD, Material.DARK_OAK_WOOD,
        Material.MANGROVE_WOOD, Material.CHERRY_WOOD,
        Material.CRIMSON_STEM, Material.WARPED_STEM
    );

    private static final Set<Material> STRIPPED_LOG_MATERIALS = Set.of(
        Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_BIRCH_LOG,
        Material.STRIPPED_JUNGLE_LOG, Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
        Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG,
        Material.STRIPPED_OAK_WOOD, Material.STRIPPED_SPRUCE_WOOD, Material.STRIPPED_BIRCH_WOOD,
        Material.STRIPPED_JUNGLE_WOOD, Material.STRIPPED_ACACIA_WOOD, Material.STRIPPED_DARK_OAK_WOOD,
        Material.STRIPPED_MANGROVE_WOOD, Material.STRIPPED_CHERRY_WOOD,
        Material.STRIPPED_CRIMSON_STEM, Material.STRIPPED_WARPED_STEM,
        Material.STRIPPED_BAMBOO_BLOCK
    );

    private static final Set<Material> LEAF_MATERIALS = Set.of(
        Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES,
        Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
        Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES, Material.AZALEA_LEAVES,
        Material.FLOWERING_AZALEA_LEAVES
    );

    private static final Set<Material> MUSHROOM_BLOCK_MATERIALS = Set.of(
        Material.BROWN_MUSHROOM_BLOCK, Material.RED_MUSHROOM_BLOCK, Material.MUSHROOM_STEM
    );

    // Passive mob types for farmer/hunter
    private static final Set<EntityType> PASSIVE_MOB_TYPES = Set.of(
        EntityType.COW, EntityType.MOOSHROOM, EntityType.PIG, EntityType.SHEEP,
        EntityType.CHICKEN, EntityType.RABBIT, EntityType.HORSE, EntityType.DONKEY,
        EntityType.MULE, EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.FOX,
        EntityType.GOAT, EntityType.CAMEL, EntityType.AXOLOTL, EntityType.CAT,
        EntityType.WOLF, EntityType.OCELOT, EntityType.PARROT, EntityType.STRIDER,
        EntityType.FROG, EntityType.SNIFFER, EntityType.ALLAY, EntityType.BEE,
        EntityType.TURTLE, EntityType.GLOW_SQUID, EntityType.SQUID, EntityType.PANDA,
        EntityType.POLAR_BEAR, EntityType.ARMADILLO, EntityType.BOGGED
    );

    // Alchemist: track brewing stand ownership
    private static final Map<Location, UUID> activeBrewing = new HashMap<>();

    // Excavator: track suspicious blocks that have already been rewarded this session
    // Key = block location key, Value = expiry time (ms). Cleared when the block changes type.
    private static final Map<Long, Long> rewardedSuspiciousBlocks = new HashMap<>();
    private static final long SUSPICIOUS_REWARD_COOLDOWN_MS = 60_000L; // 1 minute TTL

    // Explorer: track last position for travel rewards
    private static final Map<UUID, Location> lastTravelPos = new HashMap<>();

    // Explorer: PDC key for claimed chests
    private static final org.bukkit.NamespacedKey EXPLORER_CHEST_CLAIMED_KEY =
        new org.bukkit.NamespacedKey(AranarthCore.getInstance(), "explorer_chest_claimed");

    public JobEventListener(AranarthCore plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private boolean hasChunkPermission(Player player, Chunk chunk, DominionPermission permission) {
        Dominion dominion = DominionUtils.getDominionOfChunk(chunk);
        if (dominion == null) return true;
        return DominionUtils.hasPermission(player, dominion, permission);
    }

    // -------------------------------------------------------------------------
    // MINER
    // -------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBreakMiner(BlockBreakEvent e) {
        Player player = e.getPlayer();
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) return;
        if (!hasChunkPermission(player, e.getBlock().getChunk(), DominionPermission.BUILD)) return;

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) return;
        if (ap.getPlentifulBlocksToDestroy() > 0) return;

        JobData jobData = ap.getJobData();
        Material type = e.getBlock().getType();

        // MINER rewards
        if (jobData.hasJob(JobType.MINER)) {
            boolean eligible = mcMMO.getChunkManager().isEligible(e.getBlock());
            if (eligible) {
                double pay = getMinerPay(type);
                if (pay > 0) {
                    JobUtils.awardJob(player, JobType.MINER, pay);
                }
            }
        }

        // EXCAVATOR rewards
        if (jobData.hasJob(JobType.EXCAVATOR)) {
            boolean eligible = mcMMO.getChunkManager().isEligible(e.getBlock());
            if (eligible) {
                double pay = getExcavatorBlockBreakPay(type);
                if (pay > 0) {
                    JobUtils.awardJob(player, JobType.EXCAVATOR, pay);
                }
            }
        }

        // LUMBERJACK rewards (block break)
        if (jobData.hasJob(JobType.LUMBERJACK)) {
            boolean eligible = mcMMO.getChunkManager().isEligible(e.getBlock());
            if (eligible) {
                double pay = getLumberjackBreakPay(type);
                if (pay > 0) {
                    JobUtils.awardJob(player, JobType.LUMBERJACK, pay);
                }
            }
        }

        // FARMER rewards (block break - crops)
        if (jobData.hasJob(JobType.FARMER)) {
            boolean eligible = mcMMO.getChunkManager().isEligible(e.getBlock());
            if (eligible) {
                double pay = getFarmerBreakPay(e.getBlock());
                if (pay > 0) {
                    JobUtils.awardJob(player, JobType.FARMER, pay);
                }
            }
        }
    }

    private double getMinerPay(Material type) {
        return switch (type) {
            case STONE, ANDESITE, GRANITE, DIORITE -> 0.03;
            case DEEPSLATE, COBBLESTONE, COBBLED_DEEPSLATE -> 0.03;
            case TUFF, CALCITE, BASALT, BLACKSTONE, NETHERRACK -> 0.02;
            case DRIPSTONE_BLOCK, MUD -> 0.02;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> 0.04;
            case COAL_ORE, DEEPSLATE_COAL_ORE -> 0.04;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> 0.08;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> 0.12;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> 0.06;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> 0.05;
            case NETHER_QUARTZ_ORE -> 0.05;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> 0.80;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> 0.50;
            case ANCIENT_DEBRIS -> 3.00;
            case LARGE_AMETHYST_BUD -> 0.10;
            default -> 0;
        };
    }

    private double getExcavatorBlockBreakPay(Material type) {
        return switch (type) {
            case DIRT, COARSE_DIRT, ROOTED_DIRT, PODZOL, MYCELIUM -> 0.02;
            case SAND, RED_SAND -> 0.02;
            case GRAVEL -> 0.02;
            case CLAY -> 0.04;
            case MUD -> 0.02;
            case SOUL_SAND, SOUL_SOIL -> 0.03;
            case SNOW_BLOCK -> 0.02;
            default -> 0;
        };
    }

    private double getLumberjackBreakPay(Material type) {
        if (LOG_MATERIALS.contains(type)) return 0.08;
        if (STRIPPED_LOG_MATERIALS.contains(type)) return 0.05;
        if (type == Material.BAMBOO_BLOCK) return 0.04;
        if (MUSHROOM_BLOCK_MATERIALS.contains(type)) return 0.06;
        if (LEAF_MATERIALS.contains(type)) return 0.01;
        return 0;
    }

    private double getFarmerBreakPay(Block block) {
        Material type = block.getType();
        return switch (type) {
            case WHEAT -> isMature(block) ? 0.10 : 0;
            case CARROTS -> isMature(block) ? 0.05 : 0;
            case POTATOES -> isMature(block) ? 0.05 : 0;
            case BEETROOTS -> isMature(block) ? 0.12 : 0;
            case NETHER_WART -> isMature(block) ? 0.15 : 0;
            case COCOA -> isMature(block) ? 0.10 : 0;
            case MELON -> 0.20;
            case PUMPKIN -> 0.20;
            case SWEET_BERRY_BUSH -> isMature(block) ? 0.08 : 0;
            case CAVE_VINES, CAVE_VINES_PLANT -> hasGlowBerries(block) ? 0.06 : 0;
            case SUGAR_CANE -> mcMMO.getChunkManager().isEligible(block) ? 0.04 : 0;
            case CACTUS -> mcMMO.getChunkManager().isEligible(block) ? 0.04 : 0;
            default -> 0;
        };
    }

    private boolean isMature(Block block) {
        if (block.getType() == Material.MELON || block.getType() == Material.PUMPKIN) return true;
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    private boolean hasGlowBerries(Block block) {
        if (block.getBlockData() instanceof org.bukkit.block.data.type.CaveVinesPlant cvp) {
            return cvp.isBerries();
        }
        if (block.getBlockData() instanceof org.bukkit.block.data.type.CaveVines cv) {
            return cv.isBerries();
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // BUILDER
    // -------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) return;

        Block block = e.getBlockPlaced();
        if (!hasChunkPermission(player, block.getChunk(), DominionPermission.BUILD)) return;
        long locationKey = JobUtils.toLocationKey(block.getX(), block.getY(), block.getZ());

        // Check BEFORE tracking whether this location was recently placed by anyone
        boolean wasRecentlyPlaced = JobUtils.isRecentlyPlaced(locationKey);
        // Track for all players (needed for anti-exploit regardless of job)
        JobUtils.trackPlacedBlock(player.getUniqueId(), locationKey);

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) return;
        if (!ap.getJobData().hasJob(JobType.BUILDER)) return;
        if (wasRecentlyPlaced) return;

        Material mat = block.getType();
        if (isBuildableBlock(mat)) {
            JobUtils.awardJob(player, JobType.BUILDER, 0.10);
        }
    }

    private boolean isBuildableBlock(Material mat) {
        return mat.isBlock() && mat.isSolid() && !mat.isAir()
            && mat != Material.BEDROCK
            && mat != Material.BARRIER
            && mat != Material.STRUCTURE_BLOCK
            && mat != Material.STRUCTURE_VOID
            && mat != Material.COMMAND_BLOCK
            && mat != Material.CHAIN_COMMAND_BLOCK
            && mat != Material.REPEATING_COMMAND_BLOCK
            && mat != Material.JIGSAW;
    }

    // -------------------------------------------------------------------------
    // FARMER - PlayerInteract (honey)
    // -------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) return;
        if (e.getClickedBlock() == null) return;

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) return;

        Block clickedBlock = e.getClickedBlock();
        Material blockType = clickedBlock.getType();
        ItemStack inHand = e.getItem();
        if (inHand == null) return;

        // Farmer: collect honey (bottle) or honeycomb (shears) from beehives
        if ((blockType == Material.BEEHIVE || blockType == Material.BEE_NEST)
                && mcMMO.getChunkManager().isEligible(clickedBlock)
                && ap.getJobData().hasJob(JobType.FARMER)
                && hasChunkPermission(player, clickedBlock.getChunk(), DominionPermission.MISC_INTERACT)) {
            if (inHand.getType() == Material.GLASS_BOTTLE) {
                JobUtils.awardJob(player, JobType.FARMER, 0.50);
            } else if (inHand.getType() == Material.SHEARS) {
                JobUtils.awardJob(player, JobType.FARMER, 0.40);
            }
        }

        // Excavator: brushing suspicious blocks (one reward per block, 60s cooldown)
        if ((blockType == Material.SUSPICIOUS_SAND || blockType == Material.SUSPICIOUS_GRAVEL)
                && inHand.getType() == Material.BRUSH
                && mcMMO.getChunkManager().isEligible(clickedBlock)
                && ap.getJobData().hasJob(JobType.EXCAVATOR)
                && hasChunkPermission(player, clickedBlock.getChunk(), DominionPermission.MISC_INTERACT)) {
            long key = JobUtils.toLocationKey(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
            long now = System.currentTimeMillis();
            Long lastRewarded = rewardedSuspiciousBlocks.get(key);
            if (lastRewarded == null || now - lastRewarded >= SUSPICIOUS_REWARD_COOLDOWN_MS) {
                rewardedSuspiciousBlocks.put(key, now);
                JobUtils.awardJob(player, JobType.EXCAVATOR, 2.50);
            }
        }
    }

    // -------------------------------------------------------------------------
    // FARMER / HUNTER - Entity Death
    // -------------------------------------------------------------------------

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        Player killer = getPlayerKiller(entity);
        if (killer == null) return;

        String worldName = killer.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) return;

        AranarthPlayer ap = AranarthUtils.getPlayer(killer.getUniqueId());
        if (ap == null) return;
        JobData jobData = ap.getJobData();

        // Passive mob: both FARMER and HUNTER get $0.10
        if (entity instanceof Animals || PASSIVE_MOB_TYPES.contains(entity.getType())) {
            if (jobData.hasJob(JobType.FARMER)) {
                JobUtils.awardJob(killer, JobType.FARMER, 0.10);
            }
            if (jobData.hasJob(JobType.HUNTER)) {
                JobUtils.awardJob(killer, JobType.HUNTER, 0.10);
            }
            return;
        }

        // Player kill
        if (entity instanceof Player) {
            if (jobData.hasJob(JobType.HUNTER)) {
                JobUtils.awardJob(killer, JobType.HUNTER, 5.00);
            }
            return;
        }

        // Hostile mobs - HUNTER
        if (!jobData.hasJob(JobType.HUNTER)) return;

        double pay = getHunterMobPay(entity.getType());
        if (pay > 0) {
            JobUtils.awardJob(killer, JobType.HUNTER, pay);
        }
    }

    private double getHunterMobPay(EntityType type) {
        return switch (type) {
            case ZOMBIE, ZOMBIE_VILLAGER, HUSK, DROWNED -> 0.20;
            case SKELETON, STRAY -> 0.25;
            case SPIDER, CAVE_SPIDER -> 0.20;
            case CREEPER -> 0.35;
            case WITCH -> 0.50;
            case BLAZE -> 0.80;
            case GHAST -> 1.00;
            case ENDERMAN -> 0.15;
            case PIGLIN, ZOMBIFIED_PIGLIN, PIGLIN_BRUTE -> 0.20;
            case WITHER_SKELETON -> 1.50;
            case GUARDIAN, ELDER_GUARDIAN -> 0.75;
            case SHULKER -> 0.60;
            case BREEZE -> 1.20;
            case WARDEN -> 15.00;
            default -> 0;
        };
    }

    private Player getPlayerKiller(LivingEntity entity) {
        Player directKiller = entity.getKiller();
        if (directKiller != null) return directKiller;
        EntityDamageEvent lastDamage = entity.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent dmgByEntity) {
            Entity damager = dmgByEntity.getDamager();
            if (damager instanceof Player p) return p;
            if (damager instanceof Projectile proj) {
                ProjectileSource shooter = proj.getShooter();
                if (shooter instanceof Player p) return p;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // HUNTER - Fishing
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = e.getPlayer();
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) return;

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null || !ap.getJobData().hasJob(JobType.HUNTER)) return;

        if (!(e.getCaught() instanceof Item item)) return;

        ItemStack caught = item.getItemStack();
        Material mat = caught.getType();

        if (isTreasureItem(mat)) {
            JobUtils.awardJob(player, JobType.HUNTER, 1.50);
        } else if (mat == Material.COD || mat == Material.SALMON) {
            JobUtils.awardJob(player, JobType.HUNTER, 0.20);
        } else if (mat == Material.PUFFERFISH) {
            JobUtils.awardJob(player, JobType.HUNTER, 0.35);
        } else if (mat == Material.TROPICAL_FISH) {
            JobUtils.awardJob(player, JobType.HUNTER, 0.25);
        }
    }

    private boolean isTreasureItem(Material mat) {
        return mat == Material.ENCHANTED_BOOK
            || mat == Material.BOW
            || mat == Material.FISHING_ROD
            || mat == Material.SADDLE
            || mat == Material.NAME_TAG;
    }

    // -------------------------------------------------------------------------
    // LUMBERJACK - Crafting
    // -------------------------------------------------------------------------

    @EventHandler
    public void onCraftItem(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) return;

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) return;
        JobData jobData = ap.getJobData();

        ItemStack result = e.getRecipe().getResult();
        if (result == null || result.getType() == Material.AIR) return;
        Material mat = result.getType();
        int amount = result.getAmount();

        // LUMBERJACK crafting
        if (jobData.hasJob(JobType.LUMBERJACK)) {
            double pay = getLumberjackCraftPay(mat, amount);
            if (pay > 0) {
                JobUtils.awardJob(player, JobType.LUMBERJACK, pay);
            }
        }

        // SMITH crafting
        if (jobData.hasJob(JobType.SMITH)) {
            double pay = getSmithCraftPay(mat, amount);
            if (pay > 0) {
                JobUtils.awardJob(player, JobType.SMITH, pay);
            }
        }
    }

    private double getLumberjackCraftPay(Material mat, int amount) {
        if (isWoodenPlanks(mat)) return 0.02 * amount;
        if (isWoodenStairs(mat)) return 0.04 * amount;
        if (isWoodenSlab(mat)) return 0.02 * amount;
        if (isWoodenDoor(mat)) return 0.08;
        if (isWoodenTrapdoor(mat)) return 0.06;
        if (isFence(mat)) return 0.05;
        if (isFenceGate(mat)) return 0.05;
        if (isWoodenPressurePlate(mat)) return 0.04;
        if (isWoodenButton(mat)) return 0.03;
        if (mat == Material.CHISELED_BOOKSHELF) return 0.10;
        return 0;
    }

    private boolean isWoodenPlanks(Material mat) {
        return mat.name().endsWith("_PLANKS");
    }

    private boolean isWoodenStairs(Material mat) {
        String name = mat.name();
        return name.endsWith("_STAIRS") && isWoodType(name);
    }

    private boolean isWoodenSlab(Material mat) {
        String name = mat.name();
        return name.endsWith("_SLAB") && isWoodType(name);
    }

    private boolean isWoodenDoor(Material mat) {
        String name = mat.name();
        return name.endsWith("_DOOR") && isWoodType(name);
    }

    private boolean isWoodenTrapdoor(Material mat) {
        String name = mat.name();
        return name.endsWith("_TRAPDOOR") && isWoodType(name);
    }

    private boolean isFence(Material mat) {
        String name = mat.name();
        return name.endsWith("_FENCE") && !name.endsWith("_FENCE_GATE");
    }

    private boolean isFenceGate(Material mat) {
        return mat.name().endsWith("_FENCE_GATE");
    }

    private boolean isWoodenPressurePlate(Material mat) {
        String name = mat.name();
        return name.endsWith("_PRESSURE_PLATE") && isWoodType(name);
    }

    private boolean isWoodenButton(Material mat) {
        String name = mat.name();
        return name.endsWith("_BUTTON") && isWoodType(name);
    }

    private boolean isWoodType(String name) {
        return name.startsWith("OAK") || name.startsWith("SPRUCE") || name.startsWith("BIRCH")
            || name.startsWith("JUNGLE") || name.startsWith("ACACIA") || name.startsWith("DARK_OAK")
            || name.startsWith("MANGROVE") || name.startsWith("CHERRY") || name.startsWith("BAMBOO")
            || name.startsWith("CRIMSON") || name.startsWith("WARPED");
    }

    private double getSmithCraftPay(Material mat, int amount) {
        if (isIronTool(mat)) return 0.50;
        if (isIronArmor(mat)) return 0.60;
        if (isGoldTool(mat)) return 0.35;
        if (isGoldArmor(mat)) return 0.40;
        if (isDiamondTool(mat)) return 2.00;
        if (isDiamondArmor(mat)) return 2.50;
        if (isChainArmor(mat)) return 0.80;
        if (mat == Material.COPPER_BLOCK) return 0.15;
        if (mat == Material.IRON_BLOCK) return 0.25;
        if (mat == Material.GOLD_BLOCK) return 0.30;
        if (mat.name().endsWith("_CHAIN") || mat == Material.LANTERN || mat == Material.SOUL_LANTERN
                || mat == Material.IRON_BARS) return 0.20;
        return 0;
    }

    private boolean isIronTool(Material mat) {
        return mat == Material.IRON_PICKAXE || mat == Material.IRON_SHOVEL
            || mat == Material.IRON_HOE || mat == Material.IRON_SWORD || mat == Material.IRON_AXE;
    }

    private boolean isIronArmor(Material mat) {
        return mat == Material.IRON_HELMET || mat == Material.IRON_CHESTPLATE
            || mat == Material.IRON_LEGGINGS || mat == Material.IRON_BOOTS;
    }

    private boolean isGoldTool(Material mat) {
        return mat == Material.GOLDEN_PICKAXE || mat == Material.GOLDEN_SHOVEL
            || mat == Material.GOLDEN_HOE || mat == Material.GOLDEN_SWORD || mat == Material.GOLDEN_AXE;
    }

    private boolean isGoldArmor(Material mat) {
        return mat == Material.GOLDEN_HELMET || mat == Material.GOLDEN_CHESTPLATE
            || mat == Material.GOLDEN_LEGGINGS || mat == Material.GOLDEN_BOOTS;
    }

    private boolean isDiamondTool(Material mat) {
        return mat == Material.DIAMOND_PICKAXE || mat == Material.DIAMOND_SHOVEL
            || mat == Material.DIAMOND_HOE || mat == Material.DIAMOND_SWORD || mat == Material.DIAMOND_AXE;
    }

    private boolean isDiamondArmor(Material mat) {
        return mat == Material.DIAMOND_HELMET || mat == Material.DIAMOND_CHESTPLATE
            || mat == Material.DIAMOND_LEGGINGS || mat == Material.DIAMOND_BOOTS;
    }

    private boolean isChainArmor(Material mat) {
        return mat == Material.CHAINMAIL_HELMET || mat == Material.CHAINMAIL_CHESTPLATE
            || mat == Material.CHAINMAIL_LEGGINGS || mat == Material.CHAINMAIL_BOOTS;
    }

    // -------------------------------------------------------------------------
    // SMITH - Smithing table (InventoryClickEvent)
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClickSmith(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) return;

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) return;
        JobData jobData = ap.getJobData();

        if (e.getView().getType() == InventoryType.SMITHING && e.getRawSlot() == 3) {
            ItemStack result = e.getCurrentItem();
            if (result == null || result.getType() == Material.AIR) return;
            if (jobData.hasJob(JobType.SMITH)) {
                // Check if netherite upgrade or armor trim
                ItemStack template = e.getView().getTopInventory().getItem(0);
                if (template != null && template.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
                    JobUtils.awardJob(player, JobType.SMITH, 5.00);
                } else if (isArmorTrimTemplate(template)) {
                    JobUtils.awardJob(player, JobType.SMITH, 2.00);
                }
            }
        }

        // ALCHEMIST - Grindstone
        if (e.getView().getType() == InventoryType.GRINDSTONE && e.getRawSlot() == 2) {
            ItemStack result = e.getCurrentItem();
            if (result == null || result.getType() == Material.AIR) return;
            if (jobData.hasJob(JobType.ALCHEMIST)) {
                // Check if any input item had enchantments
                ItemStack input1 = e.getView().getTopInventory().getItem(0);
                ItemStack input2 = e.getView().getTopInventory().getItem(1);
                boolean hadEnchants = (input1 != null && !input1.getEnchantments().isEmpty())
                    || (input2 != null && !input2.getEnchantments().isEmpty());
                if (hadEnchants) {
                    JobUtils.awardJob(player, JobType.ALCHEMIST, 0.30);
                }
            }
        }

        // ALCHEMIST - Anvil
        if (e.getView().getType() == InventoryType.ANVIL && e.getRawSlot() == 2) {
            ItemStack result = e.getCurrentItem();
            if (result == null || result.getType() == Material.AIR) return;
            if (jobData.hasJob(JobType.ALCHEMIST)) {
                ItemStack sacrifice = e.getView().getTopInventory().getItem(1);
                if (sacrifice != null && sacrifice.getType() == Material.ENCHANTED_BOOK) {
                    JobUtils.awardJob(player, JobType.ALCHEMIST, 0.40);
                } else if (!result.getEnchantments().isEmpty()) {
                    JobUtils.awardJob(player, JobType.ALCHEMIST, 0.40);
                } else {
                    // Rename only
                    JobUtils.awardJob(player, JobType.ALCHEMIST, 0.10);
                }
            }
        }
    }

    private boolean isArmorTrimTemplate(ItemStack template) {
        if (template == null) return false;
        String name = template.getType().name();
        return name.endsWith("_SMITHING_TEMPLATE")
            && !name.equals("NETHERITE_UPGRADE_SMITHING_TEMPLATE");
    }

    // -------------------------------------------------------------------------
    // ALCHEMIST - Enchanting table
    // -------------------------------------------------------------------------

    @EventHandler
    public void onEnchantItem(EnchantItemEvent e) {
        Player player = e.getEnchanter();
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) return;

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null || !ap.getJobData().hasJob(JobType.ALCHEMIST)) return;

        JobUtils.awardJob(player, JobType.ALCHEMIST, 0.50);
    }

    // -------------------------------------------------------------------------
    // ALCHEMIST - Brewing stand tracking
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (e.getInventory().getType() != InventoryType.BREWING) return;

        Location loc = e.getInventory().getLocation();
        if (loc == null) return;
        activeBrewing.put(loc, player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getType() != InventoryType.BREWING) return;
        Location loc = e.getInventory().getLocation();
        if (loc != null) activeBrewing.remove(loc);
    }

    @EventHandler
    public void onBrew(BrewEvent e) {
        BrewerInventory brewer = e.getContents();
        Location loc = brewer.getLocation();
        if (loc == null) return;
        if (loc.getWorld() == null || !AranarthUtils.isSurvivalWorld(loc.getWorld().getName())) return;

        UUID brewerUuid = activeBrewing.get(loc);
        if (brewerUuid == null) return;

        Player player = org.bukkit.Bukkit.getPlayer(brewerUuid);
        if (player == null) return;

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null || !ap.getJobData().hasJob(JobType.ALCHEMIST)) return;

        // Count filled output slots (slots 0-2)
        int bottles = 0;
        for (int slot = 0; slot <= 2; slot++) {
            ItemStack item = brewer.getItem(slot);
            if (item != null && item.getType() != Material.AIR) bottles++;
        }
        for (int i = 0; i < bottles; i++) {
            JobUtils.awardJob(player, JobType.ALCHEMIST, 0.80);
        }
    }

    // -------------------------------------------------------------------------
    // ALCHEMIST - BreweryX brewed alcohol
    // -------------------------------------------------------------------------

    @EventHandler
    public void onBrewModify(BrewModifyEvent e) {
        if (e.getType() != BrewModifyEvent.Type.FILL) return;

        Player player = e.getPlayer();
        if (player == null) return;
        if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) return;

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null || !ap.getJobData().hasJob(JobType.ALCHEMIST)) return;

        JobUtils.awardJob(player, JobType.ALCHEMIST, 1.00);
    }

    // -------------------------------------------------------------------------
    // EXPLORER - Travel
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        Player player = e.getPlayer();
        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) return;

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null || !ap.getJobData().hasJob(JobType.EXPLORER)) return;

        // Must be on ground, not in water, not flying
        if (!player.isOnGround()) return;
        if (player.isInWater()) return;
        if (player.isFlying() || player.isGliding()) return;

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof AbstractHorse) {
            JobUtils.awardJob(player, JobType.EXPLORER, 0.004);
        } else if (vehicle == null) {
            JobUtils.awardJob(player, JobType.EXPLORER, 0.003);
        }
    }

    // -------------------------------------------------------------------------
    // EXPLORER - Chest opening
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryOpenExplorer(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (e.getInventory().getType() != InventoryType.CHEST
                && e.getInventory().getType() != InventoryType.BARREL) return;

        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) return;

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null || !ap.getJobData().hasJob(JobType.EXPLORER)) return;

        Location loc = e.getInventory().getLocation();
        if (loc == null) return;

        Block block = loc.getBlock();
        if (!mcMMO.getChunkManager().isEligible(block)) return;
        if (!hasChunkPermission(player, block.getChunk(), DominionPermission.CONTAINER)) return;
        BlockState state = block.getState();

        // Check if claimed already via PDC
        if (state instanceof org.bukkit.block.TileState tileState) {
            if (tileState.getPersistentDataContainer().has(EXPLORER_CHEST_CLAIMED_KEY, PersistentDataType.BYTE)) {
                return;
            }
            // Tag as claimed
            tileState.getPersistentDataContainer().set(EXPLORER_CHEST_CLAIMED_KEY, PersistentDataType.BYTE, (byte) 1);
            tileState.update();
        } else {
            return;
        }

        // Determine dimension for pay
        String world = worldName.toLowerCase();
        double pay;
        if (world.contains("nether")) {
            pay = 5.00;
        } else if (world.contains("end") || world.contains("the_end")) {
            pay = 8.00;
        } else {
            pay = 3.00;
        }

        JobUtils.awardJob(player, JobType.EXPLORER, pay);
    }
}
