package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.enums.QuestTrigger;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.*;
import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent;
import com.gmail.nossr50.events.skills.repair.McMMOPlayerRepairCheckEvent;
import com.gmail.nossr50.mcMMO;
import com.projectkorra.projectkorra.event.EntityBendingDeathEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrushableBlock;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BoundingBox;

import java.util.*;

/**
 * Tracks the in-game actions that progress quests and hands them to QuestUtils.progress.
 */
public class QuestEventListener implements Listener {

    private static final Set<Material> PLANTABLE = Set.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART,
            Material.MELON_STEM, Material.PUMPKIN_STEM, Material.TORCHFLOWER_CROP, Material.PITCHER_CROP);
    private static final Set<Material> FISH = Set.of(Material.COD, Material.SALMON, Material.PUFFERFISH, Material.TROPICAL_FISH);
    private static final Set<Material> TREASURE = Set.of(Material.ENCHANTED_BOOK, Material.BOW, Material.FISHING_ROD,
            Material.NAME_TAG, Material.NAUTILUS_SHELL, Material.SADDLE);
    private static final Set<Material> POTION_UPGRADES = Set.of(Material.REDSTONE, Material.GLOWSTONE_DUST,
            Material.GUNPOWDER, Material.DRAGON_BREATH);
    // Pays Farmer and Hunter for taming these animals
    private static final Set<EntityType> JOB_TAMEABLE = Set.of(EntityType.HORSE, EntityType.CAT, EntityType.WOLF);
    private static final double TAME_JOB_PAY = 15.00;
    private static final long PLACE_REPEAT_MS = 10L * 60L * 1000L;

    // Location key to when it was last placed, so placing and breaking in one spot counts once
    private final Map<String, Long> recentPlacements = new HashMap<>();
    // Animal to when it can next be milked for quests
    private final Map<UUID, Long> milkCooldowns = new HashMap<>();
    // Entities just killed by bending, so their death counts as a bending kill
    private final Set<UUID> bendingKills = new HashSet<>();
    // Distance travelled that has not yet made a full block, per player and travel mode
    private static final Map<UUID, double[]> travelRemainders = new HashMap<>();
    // Suspicious blocks being brushed: location to the brushing player and the item inside
    private final Map<Location, BrushState> brushing = new HashMap<>();

    private record BrushState(UUID player, Material item, long startedAt) {
    }

    public QuestEventListener(AranarthCore plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Biomes and structures are checked every 2 seconds rather than on every move
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkSurroundings, 40L, 40L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkBrushing, 5L, 5L);
    }

    // MONITOR so a break cancelled by any protection or crop listener never counts
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent e) {
        // Player-placed blocks never count
        if (!mcMMO.getChunkManager().isEligible(e.getBlock())) {
            return;
        }
        QuestUtils.progress(e.getPlayer(), QuestTrigger.BREAK, e.getBlock().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlockPlaced();
        Material type = block.getType();

        // Placing (or planting) and breaking in the same spot only counts once every 10 minutes
        String locationKey = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        long now = System.currentTimeMillis();
        Long last = recentPlacements.get(locationKey);
        if (last != null && now - last < PLACE_REPEAT_MS) {
            return;
        }
        if (recentPlacements.size() > 50_000) {
            recentPlacements.values().removeIf(time -> now - time >= PLACE_REPEAT_MS);
        }
        recentPlacements.put(locationKey, now);

        if (PLANTABLE.contains(type)) {
            QuestUtils.progress(player, QuestTrigger.PLANT, type.name(), 1);
            return;
        }
        if (Tag.SAPLINGS.isTagged(type) || type == Material.MANGROVE_PROPAGULE) {
            QuestUtils.progress(player, QuestTrigger.PLANT, type.name(), 1);
            // Remembers who planted the sapling, for the Grow Trees quest
            block.getChunk().getPersistentDataContainer().set(saplingKey(block), PersistentDataType.STRING,
                    player.getUniqueId().toString());
            return;
        }

        Set<String> tags = new HashSet<>();
        if (isBuildable(type)) {
            tags.add("solid");
        }
        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (playerDominion != null) {
            Dominion chunkDominion = DominionUtils.getDominionOfChunk(block.getChunk());
            if (chunkDominion != null && chunkDominion.getId().equals(playerDominion.getId())) {
                tags.add("dominion");
            }
            Outpost outpost = OutpostUtils.getOutpostOfChunk(block.getChunk());
            if (outpost != null && playerDominion.getId().equals(outpost.getDominionId())) {
                tags.add("outpost");
            }
        }
        // Each block type counts once for Build with Different Block Types
        QuestUtils.progress(player, QuestTrigger.PLACE, type.name(), 1, tags, type.name());
    }

    private boolean isBuildable(Material mat) {
        return mat.isBlock() && mat.isSolid() && !mat.isAir()
                && mat != Material.BEDROCK && mat != Material.BARRIER && mat != Material.STRUCTURE_BLOCK
                && mat != Material.STRUCTURE_VOID && mat != Material.COMMAND_BLOCK && mat != Material.CHAIN_COMMAND_BLOCK
                && mat != Material.REPEATING_COMMAND_BLOCK && mat != Material.JIGSAW;
    }

    private NamespacedKey saplingKey(Block block) {
        return new NamespacedKey("aranarthcore", "quest_sapling_" + (block.getX() & 15) + "_" + block.getY() + "_" + (block.getZ() & 15));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onStructureGrow(StructureGrowEvent e) {
        Block block = e.getLocation().getBlock();
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        NamespacedKey key = saplingKey(block);
        String owner = pdc.get(key, PersistentDataType.STRING);
        if (owner == null) {
            return;
        }
        pdc.remove(key);
        Player player = Bukkit.getPlayer(UUID.fromString(owner));
        if (player != null) {
            QuestUtils.progress(player, QuestTrigger.TREE_GROW, e.getSpecies().name(), 1);
        }
    }

    @EventHandler
    public void onEntityBendingDeath(EntityBendingDeathEvent e) {
        bendingKills.add(e.getEntity().getUniqueId());
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> bendingKills.remove(e.getEntity().getUniqueId()), 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        // Slimes and Magma Cubes split off from a larger one never count, only the original
        if (entity.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.SLIME_SPLIT) {
            return;
        }
        Player killer = getPlayerKiller(entity);
        if (killer == null || !AranarthUtils.isSurvivalWorld(killer.getWorld().getName())) {
            return;
        }

        if (entity instanceof Player victim) {
            if (!victim.getUniqueId().equals(killer.getUniqueId()) && QuestUtils.canCountPlayerKill(killer, victim)) {
                QuestUtils.progress(killer, QuestTrigger.PLAYER_KILL, "PLAYER", 1);
            }
            return;
        }

        Set<String> tags = new HashSet<>();
        tags.add("mob");
        if (entity instanceof Enemy) {
            tags.add("hostile");
        }
        if (bendingKills.remove(entity.getUniqueId())) {
            tags.add("bending");
        } else if (isRangedKill(entity)) {
            tags.add("ranged");
        } else if (isMeleeKill(entity, killer)) {
            tags.add("melee");
        }
        CreatureSpawnEvent.SpawnReason reason = entity.getEntitySpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.BREEDING || reason == CreatureSpawnEvent.SpawnReason.EGG) {
            tags.add("bred");
        }
        Dominion killerDominion = DominionUtils.getPlayerDominion(killer.getUniqueId());
        if (killerDominion != null) {
            Dominion chunkDominion = DominionUtils.getDominionOfChunk(entity.getLocation().getChunk());
            if (chunkDominion != null && chunkDominion.getId().equals(killerDominion.getId())) {
                tags.add("home");
            }
        }
        QuestUtils.progress(killer, QuestTrigger.KILL, entity.getType().name(), 1, tags);
    }

    /**
     * Attempts to find the player who killed the entity: directly, with a projectile, or with a spear lunge.
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
        if (lastDamage != null && lastDamage.getDamageSource().getCausingEntity() instanceof Player p) {
            return p;
        }
        return null;
    }

    private boolean isRangedKill(LivingEntity entity) {
        if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent dmgByEntity) {
            Entity damager = dmgByEntity.getDamager();
            return damager instanceof AbstractArrow || damager instanceof Trident || damager instanceof ThrownPotion;
        }
        return false;
    }

    private boolean isMeleeKill(LivingEntity entity, Player killer) {
        if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent dmgByEntity
                && dmgByEntity.getDamager() instanceof Projectile) {
            return false;
        }
        String matName = killer.getInventory().getItemInMainHand().getType().name();
        return matName.endsWith("_SWORD") || matName.endsWith("_AXE") || matName.endsWith("_SPEAR")
                || matName.equals("MACE") || matName.equals("TRIDENT");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(e.getCaught() instanceof Item item)) {
            return;
        }
        Material type = item.getItemStack().getType();
        Set<String> tags = new HashSet<>();
        if (FISH.contains(type)) {
            tags.add("fish");
        } else if (TREASURE.contains(type)) {
            tags.add("treasure");
        }
        QuestUtils.progress(e.getPlayer(), QuestTrigger.FISH, type.name(), 1, tags);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityBreed(EntityBreedEvent e) {
        if (e.getBreeder() instanceof Player player) {
            QuestUtils.progress(player, QuestTrigger.BREED, e.getEntity().getType().name(), 1);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMilk(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        if (!(entity instanceof Cow || entity instanceof Goat) || entity instanceof MushroomCow) {
            return;
        }
        if (entity instanceof Ageable ageable && !ageable.isAdult()) {
            return;
        }
        ItemStack inHand = e.getPlayer().getInventory().getItem(e.getHand());
        if (inHand == null || inHand.getType() != Material.BUCKET) {
            return;
        }
        // Each animal only counts once every few seconds, as with the Farmer job
        long now = System.currentTimeMillis();
        Long until = milkCooldowns.get(entity.getUniqueId());
        if (until != null && until > now) {
            return;
        }
        if (milkCooldowns.size() > 5_000) {
            milkCooldowns.values().removeIf(time -> time < now);
        }
        milkCooldowns.put(entity.getUniqueId(), now + QuestConfig.getMilkCooldownSeconds() * 1000L);
        QuestUtils.progress(e.getPlayer(), QuestTrigger.MILK, entity.getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onShear(PlayerShearEntityEvent e) {
        QuestUtils.progress(e.getPlayer(), QuestTrigger.SHEAR, e.getEntity().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTame(EntityTameEvent e) {
        if (!(e.getOwner() instanceof Player player)) {
            return;
        }
        QuestUtils.progress(player, QuestTrigger.TAME, e.getEntity().getType().name(), 1);
        // Taming horses, cats and wolves pays both Farmers and Hunters
        if (JOB_TAMEABLE.contains(e.getEntity().getType())) {
            JobUtils.awardJob(player, JobType.FARMER, TAME_JOB_PAY);
            JobUtils.awardJob(player, JobType.HUNTER, TAME_JOB_PAY);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || e.getItem() == null) {
            return;
        }
        Block block = e.getClickedBlock();
        Material held = e.getItem().getType();

        // Honey and honeycomb from a full hive
        if ((block.getType() == Material.BEEHIVE || block.getType() == Material.BEE_NEST)
                && block.getBlockData() instanceof Beehive hive && hive.getHoneyLevel() >= hive.getMaximumHoneyLevel()) {
            if (held == Material.GLASS_BOTTLE) {
                QuestUtils.progress(e.getPlayer(), QuestTrigger.HONEY, Material.HONEY_BOTTLE.name(), 1);
            } else if (held == Material.SHEARS) {
                QuestUtils.progress(e.getPlayer(), QuestTrigger.HONEY, Material.HONEYCOMB.name(), 1);
            }
            return;
        }

        // Brushing is tracked until the block turns back to sand or gravel
        if (held == Material.BRUSH && (block.getType() == Material.SUSPICIOUS_SAND || block.getType() == Material.SUSPICIOUS_GRAVEL)) {
            brushing.put(block.getLocation(), new BrushState(e.getPlayer().getUniqueId(), Material.AIR, System.currentTimeMillis()));
        }
    }

    private void checkBrushing() {
        if (brushing.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Location, BrushState>> it = brushing.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, BrushState> entry = it.next();
            BrushState state = entry.getValue();
            Block block = entry.getKey().getBlock();
            if (block.getType() == Material.SUSPICIOUS_SAND || block.getType() == Material.SUSPICIOUS_GRAVEL) {
                if (now - state.startedAt() > 30_000L) {
                    it.remove();
                    continue;
                }
                // The item is only rolled once brushing starts, so keep reading it until the block finishes
                BlockState blockState = block.getState();
                if (blockState instanceof BrushableBlock brushable && brushable.getItem() != null
                        && brushable.getItem().getType() != Material.AIR) {
                    entry.setValue(new BrushState(state.player(), brushable.getItem().getType(), state.startedAt()));
                }
                continue;
            }
            it.remove();
            Player player = Bukkit.getPlayer(state.player());
            if (player != null && (block.getType() == Material.SAND || block.getType() == Material.GRAVEL)) {
                QuestUtils.progress(player, QuestTrigger.BRUSH, state.item().name(), 1);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        // The Fletching Table reuses a crafting inventory and is counted by its own quest instead
        if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Fletching Table")) {
            return;
        }
        // The item actually in the result slot, since crafting overrides can replace the recipe's default result
        ItemStack result = e.getCurrentItem() != null ? e.getCurrentItem() : e.getRecipe().getResult();
        if (result.getType() == Material.AIR) {
            return;
        }
        int crafts = 1;
        if (e.isShiftClick()) {
            crafts = Integer.MAX_VALUE;
            for (ItemStack ingredient : e.getInventory().getMatrix()) {
                if (ingredient != null && ingredient.getType() != Material.AIR) {
                    crafts = Math.min(crafts, ingredient.getAmount());
                }
            }
            if (crafts == Integer.MAX_VALUE) {
                crafts = 1;
            }
            // Never more than the player's inventory can hold
            crafts = Math.min(crafts, getFreeSpace(player, result) / Math.max(1, result.getAmount()));
        } else if (e.getCursor() != null && e.getCursor().getType() != Material.AIR && !e.getCursor().isSimilar(result)) {
            return;
        }
        if (crafts > 0) {
            QuestUtils.progress(player, QuestTrigger.CRAFT, result.getType().name(), crafts * result.getAmount());
        }
    }

    /**
     * @return How many of the item still fit in the player's inventory.
     */
    private int getFreeSpace(Player player, ItemStack item) {
        int space = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                space += item.getMaxStackSize();
            } else if (slot.isSimilar(item)) {
                space += item.getMaxStackSize() - slot.getAmount();
            }
        }
        return space;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFurnaceExtract(FurnaceExtractEvent e) {
        QuestUtils.progress(e.getPlayer(), QuestTrigger.SMELT, e.getItemType().name(), e.getItemAmount());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onStonecut(InventoryClickEvent e) {
        if (e.getView().getType() != InventoryType.STONECUTTER || e.getRawSlot() != 1
                || !(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }
        int amount = result.getAmount();
        if (e.isShiftClick()) {
            ItemStack input = e.getView().getTopInventory().getItem(0);
            amount = input == null ? 0 : input.getAmount() * result.getAmount();
            amount = Math.min(amount, getFreeSpace(player, result));
        } else if (e.getCursor() != null && e.getCursor().getType() != Material.AIR && !e.getCursor().isSimilar(result)) {
            return;
        }
        // A stonecutter placed on wood is a Sawmill, identified by its title
        String title = ChatColor.stripColor(e.getView().getTitle());
        boolean isSawmill = title.equals(ChatColor.stripColor(Lang.getFor(player, "gui.recipes.station_sawmill")));
        QuestUtils.progress(player, isSawmill ? QuestTrigger.SAWMILL : QuestTrigger.STONECUT, result.getType().name(), amount);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEnchant(EnchantItemEvent e) {
        Set<String> tags = e.getExpLevelCost() >= 30 ? Set.of("level30") : Set.of();
        QuestUtils.progress(e.getEnchanter(), QuestTrigger.ENCHANT, e.getItem().getType().name(), 1, tags);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAnvilOrGrindstone(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player) || e.getCurrentItem() == null
                || e.getCurrentItem().getType() == Material.AIR) {
            return;
        }
        InventoryType type = e.getView().getType();
        // The result is only taken onto an empty cursor or by shift-clicking
        boolean isCursorFree = e.getCursor() == null || e.getCursor().getType() == Material.AIR;
        if (!isCursorFree && !e.isShiftClick()) {
            return;
        }
        if (type == InventoryType.ANVIL && e.getRawSlot() == 2) {
            // Without enough levels the anvil keeps the result
            if (e.getView() instanceof org.bukkit.inventory.view.AnvilView anvil && player.getGameMode() != GameMode.CREATIVE
                    && player.getLevel() < anvil.getRepairCost()) {
                return;
            }
            ItemStack first = e.getView().getTopInventory().getItem(0);
            ItemStack second = e.getView().getTopInventory().getItem(1);
            if (first == null || second == null) {
                return;
            }
            if (second.getType() == Material.ENCHANTED_BOOK) {
                QuestUtils.progress(player, QuestTrigger.ANVIL_BOOK, first.getType().name(), 1);
            } else if (isDamaged(first)) {
                QuestUtils.progress(player, QuestTrigger.REPAIR, first.getType().name(), 1);
            }
        } else if (type == InventoryType.GRINDSTONE && e.getRawSlot() == 2) {
            ItemStack first = e.getView().getTopInventory().getItem(0);
            ItemStack second = e.getView().getTopInventory().getItem(1);
            if (hasRemovableEnchants(first) || hasRemovableEnchants(second)) {
                QuestUtils.progress(player, QuestTrigger.GRINDSTONE, e.getCurrentItem().getType().name(), 1);
            }
        }
    }

    private boolean isDamaged(ItemStack item) {
        return item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable && damageable.getDamage() > 0;
    }

    private boolean hasRemovableEnchants(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        Map<Enchantment, Integer> enchants = meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta storage
                ? storage.getStoredEnchants() : meta.getEnchants();
        for (Enchantment enchantment : enchants.keySet()) {
            if (!enchantment.isCursed()) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMcMMORepair(McMMOPlayerRepairCheckEvent e) {
        QuestUtils.progress(e.getPlayer(), QuestTrigger.REPAIR, e.getRepairedObject().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAbilityActivate(McMMOPlayerAbilityActivateEvent e) {
        QuestUtils.progress(e.getPlayer(), QuestTrigger.SUPER_ABILITY, e.getAbility().name(), 1);
        if (e.getAbility() == SuperAbilityType.TREE_FELLER) {
            QuestUtils.progress(e.getPlayer(), QuestTrigger.TREE_FELLER, e.getAbility().name(), 1);
        }
    }

    /**
     * Counts a finished brew for the player who loaded the stand.
     *
     * @param player The player who loaded the brewing stand.
     * @param ingredient The ingredient that was brewed.
     * @param results The potions in the stand after brewing.
     */
    public static void onBrewed(Player player, Material ingredient, List<ItemStack> results) {
        if (POTION_UPGRADES.contains(ingredient)) {
            return;
        }
        for (ItemStack potion : results) {
            if (potion == null || potion.getType() == Material.AIR || potion.getType() == Material.GLASS_BOTTLE) {
                continue;
            }
            String name = getPotionKey(potion);
            if (name != null) {
                QuestUtils.progress(player, QuestTrigger.BREW, name, 1);
            }
        }
    }

    /**
     * @return The plain potion name used by quests.yml, such as SWIFTNESS or HASTE, for vanilla and mcMMO potions.
     */
    public static String getPotionKey(ItemStack potion) {
        if (potion.hasItemMeta() && potion.getItemMeta().hasCustomName()) {
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(Objects.requireNonNull(potion.getItemMeta().customName()));
            name = name.replaceAll("(?i)^(extended |splash |lingering )*", "")
                    .replaceAll("(?i)^potion of ", "")
                    .replaceAll("(?i) (ii|iii|iv)$", "")
                    .trim();
            return name.isEmpty() ? null : name.toUpperCase(Locale.ROOT).replace(' ', '_');
        }
        if (!(potion.getItemMeta() instanceof PotionMeta meta) || meta.getBasePotionType() == null) {
            return null;
        }
        String type = meta.getBasePotionType().name().replaceFirst("^(LONG_|STRONG_)", "");
        return switch (type) {
            case "WIND_CHARGED" -> "WIND_CHARGING";
            case "INFESTED" -> "INFESTATION";
            case "WATER", "MUNDANE", "THICK", "AWKWARD" -> null;
            default -> type;
        };
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCure(EntityTransformEvent e) {
        if (e.getTransformReason() != EntityTransformEvent.TransformReason.CURED
                || !(e.getEntity() instanceof ZombieVillager zombieVillager)) {
            return;
        }
        OfflinePlayer curer = zombieVillager.getConversionPlayer();
        if (curer != null && curer.getPlayer() != null) {
            QuestUtils.progress(curer.getPlayer(), QuestTrigger.CURE, "ZOMBIE_VILLAGER", 1);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (from.getX() == to.getX() && from.getZ() == to.getZ()) {
            return;
        }
        Player player = e.getPlayer();
        String worldName = player.getWorld().getName();
        // Travel quests never count in spawn or the shops world
        if (!AranarthUtils.isSurvivalWorld(worldName) || worldName.equals("spawn") || worldName.equals("shops")) {
            return;
        }
        // Teleports do not count towards travel
        if (!AranarthUtils.isActiveTravel(player, from, to)) {
            return;
        }

        if ((from.getBlockX() >> 4) != (to.getBlockX() >> 4) || (from.getBlockZ() >> 4) != (to.getBlockZ() >> 4)) {
            checkDominionVisit(player, to);
        }

        String mode = getTravelMode(player);
        if (mode == null) {
            return;
        }
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        addTravel(player, mode, Math.sqrt(dx * dx + dz * dz));
    }

    private static final Map<UUID, Location> lastMountLocations = new HashMap<>();

    /**
     * Called every tick by the /mount movement loop, since bending mounts move without the rider's move events.
     *
     * @param rider The player riding the mount.
     * @param mount The mount entity.
     * @param hasInput Whether the rider is pressing a movement key.
     */
    public static void onMountTick(Player rider, LivingEntity mount, boolean hasInput) {
        Location current = mount.getLocation();
        Location last = lastMountLocations.put(rider.getUniqueId(), current);
        String worldName = current.getWorld().getName();
        if (!hasInput || last == null || last.getWorld() != current.getWorld()
                || !AranarthUtils.isSurvivalWorld(worldName) || worldName.equals("spawn") || worldName.equals("shops")) {
            return;
        }
        double dx = current.getX() - last.getX();
        double dz = current.getZ() - last.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        // A single tick of more than 2 blocks is a teleport, not travel
        if (distance > 0 && distance <= 2) {
            addTravel(rider, "MOUNT", distance);
        }
    }

    private static void addTravel(Player player, String mode, double distance) {
        // Ignore jumps that cannot be real movement
        if (distance > 10) {
            return;
        }
        int index = switch (mode) {
            case "WALK" -> 0;
            case "HORSE" -> 1;
            case "BOAT" -> 2;
            case "GLIDE" -> 3;
            default -> 4;
        };
        double[] remainders = travelRemainders.computeIfAbsent(player.getUniqueId(), k -> new double[5]);
        remainders[index] += distance;
        int blocks = (int) remainders[index];
        if (blocks > 0) {
            remainders[index] -= blocks;
            QuestUtils.progress(player, QuestTrigger.TRAVEL, mode, blocks);
        }
    }

    /**
     * @return How the player is travelling, or null if the movement does not count.
     */
    private String getTravelMode(Player player) {
        if (player.isFlying()) {
            return null;
        }
        if (player.isGliding()) {
            return "GLIDE";
        }
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof Boat) {
            return "BOAT";
        }
        if (vehicle instanceof AbstractHorse horse) {
            return horse.isOnGround() && !horse.isInWater() ? "HORSE" : null;
        }
        if (vehicle == null && player.isOnGround() && !player.isInWater()) {
            return "WALK";
        }
        return null;
    }

    private void checkDominionVisit(Player player, Location to) {
        Dominion dominion = DominionUtils.getDominionOfChunk(to.getChunk());
        if (dominion == null) {
            return;
        }
        Dominion own = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (own != null && own.getId().equals(dominion.getId())) {
            return;
        }
        QuestUtils.progress(player, QuestTrigger.DOMINION_VISIT, "DOMINION", 1, Set.of(), dominion.getId().toString());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLootGenerate(LootGenerateEvent e) {
        if (!(e.getEntity() instanceof Player player) || e.getInventoryHolder() == null
                || e.getInventoryHolder() instanceof Entity) {
            return;
        }
        QuestUtils.progress(player, QuestTrigger.LOOT, player.getWorld().getEnvironment().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTrade(PlayerTradeEvent e) {
        if (e.getVillager() instanceof WanderingTrader trader) {
            QuestUtils.progress(e.getPlayer(), QuestTrigger.TRADER, "WANDERING_TRADER", 1, Set.of(), trader.getUniqueId().toString());
        }
    }

    /**
     * Checks the biome and structures each player is in, for the biome and structure quests.
     */
    private void checkSurroundings() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String worldName = player.getWorld().getName();
            if (!AranarthUtils.isSurvivalWorld(worldName) || worldName.equals("spawn") || worldName.equals("shops")) {
                continue;
            }
            Location location = player.getLocation();
            String biome = location.getBlock().getBiome().getKey().getKey().toUpperCase(Locale.ROOT);
            QuestUtils.progress(player, QuestTrigger.BIOME, biome, 1);

            for (GeneratedStructure structure : location.getChunk().getStructures()) {
                BoundingBox box = structure.getBoundingBox();
                if (!box.contains(location.toVector())) {
                    continue;
                }
                NamespacedKey key = Registry.STRUCTURE.getKey(structure.getStructure());
                if (key == null) {
                    continue;
                }
                String id = key + "@" + worldName + ":" + (int) box.getMinX() + ":" + (int) box.getMinY() + ":" + (int) box.getMinZ();
                QuestUtils.progress(player, QuestTrigger.STRUCTURE, key.toString(), 1, Set.of(), id);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        travelRemainders.remove(e.getPlayer().getUniqueId());
        lastMountLocations.remove(e.getPlayer().getUniqueId());
    }
}
