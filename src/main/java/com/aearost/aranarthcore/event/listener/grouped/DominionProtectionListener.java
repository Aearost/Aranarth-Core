package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.MountUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces Dominion block and entity protection rules with configurable per-rank/relation permissions.
 */
public class DominionProtectionListener implements Listener {

    private static final Logger log = LoggerFactory.getLogger(DominionProtectionListener.class);
    private static final long DENY_MESSAGE_COOLDOWN_MS = 1000L;
    private final Map<UUID, Long> lastDenyMessageTime = new HashMap<>();

    public DominionProtectionListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Prevents players from placing blocks in another Dominion.
     */
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
        if (!aranarthPlayer.isInAdminMode()) {
            if (applyLogic(e.getPlayer(), e.getBlock(), null, DominionPermission.BUILD)) {
                e.setCancelled(true);
            }
        }
    }

    /**
     * Prevents players from emptying buckets in another Dominion.
     */
    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (!applyBucketLogic(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents players from picking up liquids (water, lava, powder snow) in another Dominion.
     */
    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent e) {
        Material blockType = e.getBlock().getType();
        if (blockType != Material.WATER && blockType != Material.LAVA && blockType != Material.POWDER_SNOW) {
            return;
        }
        if (!applyBucketLogic(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
        }
    }

    /**
     * Shared bucket logic for both empty and fill events.
     */
    private boolean applyBucketLogic(Player player, Block block) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.isInAdminMode()) {
            return true;
        }
        Dominion chunkDominion = DominionUtils.getDominionOfChunk(block.getChunk());
        if (chunkDominion == null) {
            return true;
        }
        if (DominionUtils.hasPermission(player, chunkDominion, DominionPermission.BUILD)) {
            return true;
        }
        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (playerDominion != null && playerDominion.isEnemied(chunkDominion)) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long last = lastDenyMessageTime.get(player.getUniqueId());
        if (last == null || now - last >= DENY_MESSAGE_COOLDOWN_MS) {
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to do this in &e" + chunkDominion.getName()));
            lastDenyMessageTime.put(player.getUniqueId(), now);
        }
        return false;
    }

    /**
     * Prevents players from breaking blocks in another Dominion.
     */
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
        if (!aranarthPlayer.isInAdminMode()) {
            // CropHarvest already processed this so it would be air, no message to be sent
            if (e.getBlock().getType() == Material.AIR) {
                return;
            }
            // Crops can be destroyed by enemies
            if (isEnemyHarvestableCrop(e.getBlock().getType())) {
                Dominion blockDominion = DominionUtils.getDominionOfChunk(e.getBlock().getChunk());
                if (blockDominion != null) {
                    Dominion playerDominion = DominionUtils.getPlayerDominion(e.getPlayer().getUniqueId());
                    if (playerDominion != null && playerDominion.isEnemied(blockDominion)) {
                        return;
                    }
                }
            }
            if (applyLogic(e.getPlayer(), e.getBlock(), null, DominionPermission.BUILD)) {
                e.setCancelled(true);
            }
        }
    }

    /**
     * Returns true if the material is a crop or stem that enemied dominion members are permitted to harvest.
     */
    private boolean isEnemyHarvestableCrop(Material type) {
        return AranarthUtils.isBlockCrop(type)
                || type == Material.MELON_STEM
                || type == Material.ATTACHED_MELON_STEM
                || type == Material.PUMPKIN_STEM
                || type == Material.ATTACHED_PUMPKIN_STEM;
    }

    /**
     * Prevents players from interacting with armor stands in another Dominion.
     * Placing an armor stand is handled by onPlaceEntity (EntityPlaceEvent) as BUILD.
     */
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() == null) {
            return;
        }
        EntityType type = e.getRightClicked().getType();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
        if (aranarthPlayer.isInAdminMode()) {
            return;
        }

        if (type == EntityType.ARMOR_STAND) {
            if (applyLogic(e.getPlayer(), null, e.getRightClicked(), DominionPermission.ARMOR_STAND)) {
                e.setCancelled(true);
            }
        } else if (e.getRightClicked() instanceof Villager) {
            if (applyLogic(e.getPlayer(), null, e.getRightClicked(), DominionPermission.VILLAGER)) {
                e.setCancelled(true);
            }
        } else if (!type.isAlive()) {
            // Other non-alive entities (item frames handled separately in HangingPlaceEvent)
            if (applyLogic(e.getPlayer(), null, e.getRightClicked(), DominionPermission.ITEM_FRAME)) {
                e.setCancelled(true);
            }
        }
    }

    /**
     * Prevents players from placing non-alive entities (including armor stands) in another Dominion.
     * Placing counts as BUILD.
     */
    @EventHandler
    public void onPlaceEntity(EntityPlaceEvent e) {
        if (e.getEntity() == null) {
            return;
        }
        EntityType type = e.getEntity().getType();
        if (!type.isAlive() || type == EntityType.ARMOR_STAND) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
            if (!aranarthPlayer.isInAdminMode()) {
                if (applyLogic(e.getPlayer(), null, e.getEntity(), DominionPermission.BUILD)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    /**
     * Cancels fall damage for players in any dominion's claimed chunks.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }
        if (DominionUtils.getDominionOfChunk(player.getLocation().getChunk()) != null) {
            e.setCancelled(true);
        }
    }

    /**
     * Handles entity damage in Dominion land.
     * Armor stand attacks are treated as BUILD (destruction). PvP is controlled by the PVP permission.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAttackEntity(EntityDamageEvent e) {
        String name = e.getEntity().getLocation().getWorld().getName();
        if (!name.startsWith("world") && !AranarthUtils.isSmpWorld(name) && !name.startsWith("resource")) {
            return;
        }
        if (e.getEntity() == null) {
            return;
        }

        EntityType type = e.getEntity().getType();

        // Armor stands: attacking counts as destroying (BUILD)
        if (type == EntityType.ARMOR_STAND) {
            if (e.getDamageSource().getCausingEntity() instanceof Player player) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                if (!aranarthPlayer.isInAdminMode()) {
                    if (applyLogic(player, null, e.getEntity(), DominionPermission.BUILD)) {
                        e.setCancelled(true);
                    }
                }
            }
        }
        // Other non-alive entities
        else if (!type.isAlive()) {
            if (e.getDamageSource().getCausingEntity() instanceof Player player) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                if (!aranarthPlayer.isInAdminMode()) {
                    if (applyLogic(player, null, e.getEntity(), DominionPermission.BUILD)) {
                        e.setCancelled(true);
                    }
                }
            }
        }
        // PvP against players (including damage dealt by mounts on behalf of their rider)
        else if (e.getEntity() instanceof Player target) {
            Player attacker = null;
            Entity causingEntity = e.getDamageSource().getCausingEntity();
            if (causingEntity instanceof Player p) {
                attacker = p;
            } else if (causingEntity != null && MountUtils.isActiveMount(causingEntity.getUniqueId())) {
                String[] info = MountUtils.getActiveMountInfo(causingEntity.getUniqueId());
                if (info != null) {
                    Entity rider = Bukkit.getEntity(java.util.UUID.fromString(info[0]));
                    if (rider instanceof Player p) {
                        attacker = p;
                    }
                }
            }
            if (attacker != null) {
                handlePvP(attacker, target, e);
            }
        }
        // Tamed pets
        else if (e.getEntity() instanceof Tameable pet && pet.isTamed()) {
            if (e.getDamageSource().getCausingEntity() instanceof Player attacker) {
                handlePetDamage(attacker, pet, e);
            }
        }
        // Active mounts (treated as their rider/owner for relation purposes)
        else if (MountUtils.isActiveMount(e.getEntity().getUniqueId())) {
            if (e.getDamageSource().getCausingEntity() instanceof Player attacker) {
                String[] info = MountUtils.getActiveMountInfo(e.getEntity().getUniqueId());
                if (info != null) {
                    Entity rider = Bukkit.getEntity(java.util.UUID.fromString(info[0]));
                    if (rider instanceof Player owner) {
                        handleMountDamage(attacker, owner, e);
                    }
                }
            }
        }
    }

    /**
     * Handles PvP between players with relation-based and chunk-based rules.
     */
    private void handlePvP(Player attacker, Player target, EntityDamageEvent e) {
        if (AranarthUtils.isSpawnLocation(target.getLocation())) {
            return;
        }

        Dominion attackerDominion = DominionUtils.getPlayerDominion(attacker.getUniqueId());
        Dominion targetDominion = DominionUtils.getPlayerDominion(target.getUniqueId());
        AranarthPlayer aranarthTarget = AranarthUtils.getPlayer(target.getUniqueId());

        // Same dominion — check the single dominion-wide PvP flag
        if (attackerDominion != null && targetDominion != null
                && attackerDominion.isSameDominion(targetDominion)) {
            if (!attackerDominion.isMemberPvpEnabled()) {
                e.setCancelled(true);
                attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthTarget.getNickname()
                        + " &7as PvP is disabled in &e" + attackerDominion.getName()));
            }
            return;
        }

        // Both players belong to different dominions
        if (attackerDominion != null && targetDominion != null) {
            DominionRank relation = DominionUtils.getRelationKey(attackerDominion, targetDominion);
            Dominion chunkDominion = DominionUtils.getDominionOfChunk(target.getLocation().getChunk());

            if (relation == DominionRank.ALLIED || relation == DominionRank.TRUCED) {
                // Either dominion having PvP enabled for this relation allows the attack
                boolean attackerPvp = attackerDominion.getDominionPermissions().hasPermission(relation, DominionPermission.PVP);
                boolean targetPvp = targetDominion.getDominionPermissions().hasPermission(relation, DominionPermission.PVP);
                if (!attackerPvp && !targetPvp) {
                    e.setCancelled(true);
                    attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthTarget.getNickname()
                            + " &7as you are " + DominionUtils.getFormattedRankName(relation)));
                }
                return;
            }

            if (relation == DominionRank.NEUTRAL) {
                // Blocked only when the target is in their own dominion's land
                if (chunkDominion != null && chunkDominion.isSameDominion(targetDominion)) {
                    e.setCancelled(true);
                    attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthTarget.getNickname()
                            + " &7in their lands as you are " + DominionUtils.getFormattedRankName(DominionRank.NEUTRAL)));
                }
                return;
            }

            if (relation == DominionRank.ENEMIED) {
                // Always allowed
                return;
            }
            return;
        }

        // Attacker has a dominion, target is a wanderer — always allowed
        if (attackerDominion != null) {
            return;
        }

        // Attacker is a wanderer, target has a dominion — blocked in target's own land
        if (targetDominion != null) {
            Dominion chunkDominion = DominionUtils.getDominionOfChunk(target.getLocation().getChunk());
            if (chunkDominion != null && chunkDominion.isSameDominion(targetDominion)) {
                e.setCancelled(true);
                attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthTarget.getNickname()
                        + " &7in their dominion's lands as a wanderer"));
            }
        }
    }

    /**
     * Handles damage to tamed pets using PvP permission and relation-based defaults.
     */
    private void handlePetDamage(Player attacker, Tameable pet, EntityDamageEvent e) {
        Dominion attackerDominion = DominionUtils.getPlayerDominion(attacker.getUniqueId());
        boolean petHasOwner = pet.getOwner() != null;
        Dominion targetDominion = petHasOwner ? DominionUtils.getPlayerDominion(pet.getOwner().getUniqueId()) : null;

        if (attackerDominion == null || targetDominion == null) {
            return;
        }

        AranarthPlayer aranarthOwner = AranarthUtils.getPlayer(pet.getOwner().getUniqueId());

        if (AranarthUtils.isSpawnLocation(pet.getLocation())) {
            return;
        }
        if (pet.getOwner().getUniqueId().equals(attacker.getUniqueId())) {
            return;
        }

        Dominion chunkDominion = DominionUtils.getDominionOfChunk(pet.getLocation().getChunk());
        if (chunkDominion == null) {
            return;
        }

        DominionRank attackerRelation;
        if (attackerDominion.isSameDominion(chunkDominion)) {
            attackerRelation = chunkDominion.getMemberRank(attacker.getUniqueId());
            if (attackerRelation == null) {
                attackerRelation = DominionRank.NEWCOMER;
            }
        } else {
            attackerRelation = DominionUtils.getRelationKey(attackerDominion, chunkDominion);
        }

        boolean pvpAllowed = chunkDominion.getDominionPermissions().hasPermission(attackerRelation, DominionPermission.PVP);

        if (!pvpAllowed) {
            e.setCancelled(true);
            String petType = ChatUtils.getFormattedItemName(pet.getType().name());
            String relationName = DominionUtils.getFormattedRankName(attackerRelation);
            if (attackerDominion.isSameDominion(chunkDominion)) {
                attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthOwner.getNickname() + "'s &e" + petType + " &7as you are both in &e" + chunkDominion.getName()));
            } else {
                attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthOwner.getNickname() + "'s &e" + petType + " &7in their lands as you are " + relationName));
            }
        }
    }

    /**
     * Handles damage to active mounts using the same relation-based rules as PvP.
     * The mount's rider is treated as the owner for all relation checks.
     */
    private void handleMountDamage(Player attacker, Player mountOwner, EntityDamageEvent e) {
        if (attacker.getUniqueId().equals(mountOwner.getUniqueId())) {
            return;
        }
        if (AranarthUtils.isSpawnLocation(e.getEntity().getLocation())) {
            return;
        }

        Dominion attackerDominion = DominionUtils.getPlayerDominion(attacker.getUniqueId());
        Dominion ownerDominion = DominionUtils.getPlayerDominion(mountOwner.getUniqueId());
        AranarthPlayer aranarthOwner = AranarthUtils.getPlayer(mountOwner.getUniqueId());
        String[] mountInfo = MountUtils.getActiveMountInfo(e.getEntity().getUniqueId());
        String mountTypeName = mountInfo != null
                ? MountUtils.getDisplayName(mountOwner.getUniqueId(), mountInfo[1])
                : ChatUtils.getFormattedItemName(e.getEntity().getType().name());

        // Same dominion — check the dominion-wide PvP flag
        if (attackerDominion != null && ownerDominion != null
                && attackerDominion.isSameDominion(ownerDominion)) {
            if (!attackerDominion.isMemberPvpEnabled()) {
                e.setCancelled(true);
                attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthOwner.getNickname()
                        + "'s &e" + mountTypeName + " &7as PvP is disabled in &e" + attackerDominion.getName()));
            }
            return;
        }

        // Both players belong to different dominions
        if (attackerDominion != null && ownerDominion != null) {
            DominionRank relation = DominionUtils.getRelationKey(attackerDominion, ownerDominion);
            Dominion chunkDominion = DominionUtils.getDominionOfChunk(e.getEntity().getLocation().getChunk());

            if (relation == DominionRank.ALLIED || relation == DominionRank.TRUCED) {
                // Either dominion having PvP enabled for this relation allows the attack
                boolean attackerPvp = attackerDominion.getDominionPermissions().hasPermission(relation, DominionPermission.PVP);
                boolean ownerPvp = ownerDominion.getDominionPermissions().hasPermission(relation, DominionPermission.PVP);
                if (!attackerPvp && !ownerPvp) {
                    e.setCancelled(true);
                    attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthOwner.getNickname()
                            + "'s &e" + mountTypeName + " &7as you are " + DominionUtils.getFormattedRankName(relation)));
                }
                return;
            }

            if (relation == DominionRank.NEUTRAL) {
                // Blocked only when the mount owner is in their own dominion's land
                if (chunkDominion != null && chunkDominion.isSameDominion(ownerDominion)) {
                    e.setCancelled(true);
                    attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthOwner.getNickname()
                            + "'s &e" + mountTypeName + " &7in their lands as you are " + DominionUtils.getFormattedRankName(DominionRank.NEUTRAL)));
                }
                return;
            }

            if (relation == DominionRank.ENEMIED) {
                // Always allowed
                return;
            }
            return;
        }

        // Attacker has a dominion, mount owner is a wanderer
        if (attackerDominion != null) {
            return;
        }

        // Attacker is a wanderer, mount owner has a dominion
        if (ownerDominion != null) {
            Dominion chunkDominion = DominionUtils.getDominionOfChunk(e.getEntity().getLocation().getChunk());
            if (chunkDominion != null && chunkDominion.isSameDominion(ownerDominion)) {
                e.setCancelled(true);
                attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthOwner.getNickname()
                        + "'s &e" + mountTypeName + " &7in their dominion's lands as a wanderer"));
            }
        }
    }

    /**
     * Handles block interaction events, routing to the appropriate specific permission.
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        if (block == null) {
            return;
        }

        DominionPermission perm = getInteractionPermission(block);
        if (perm == null) {
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.isInAdminMode()) {
            return;
        }

        // Containers: only restrict unclaimed land access; claimed land still uses chest locking
        if (perm == DominionPermission.CONTAINER) {
            Dominion chunkDominion = DominionUtils.getDominionOfChunk(block.getChunk());
            if (chunkDominion == null) {
                return; // unclaimed land — no restriction here
            }
            // Only show error if it is not a shop
            if (ShopUtils.getShopFromLocation(block.getLocation()) != null) {
                return;
            }
            if (applyLogic(player, block, null, DominionPermission.CONTAINER)) {
                e.setCancelled(true);
            }
            return;
        }

        // For all other interaction types
        if (ShopUtils.getShopFromLocation(block.getLocation()) != null) {
            return;
        }
        if (applyLogic(player, block, null, perm)) {
            e.setCancelled(true);
        }
    }

    /**
     * Returns the DominionPermission that governs interaction with the given block,
     * or null if the block is not Dominion-protected.
     */
    private DominionPermission getInteractionPermission(Block block) {
        Material type = block.getType();
        String name = type.name();

        if (name.endsWith("_DOOR")) {
            return DominionPermission.DOOR;
        }
        if (name.endsWith("_BUTTON")) {
            return DominionPermission.BUTTON;
        }
        if (name.endsWith("_GATE")) {
            return DominionPermission.FENCE_GATE;
        }
        if (name.endsWith("_TRAPDOOR")) {
            return DominionPermission.TRAPDOOR;
        }
        if (type == Material.LEVER) {
            return DominionPermission.LEVER;
        }
        if (name.endsWith("_PRESSURE_PLATE")) {
            return DominionPermission.PRESSURE_PLATE;
        }

        // Containers (claimed-land only restriction)
        if (isContainerBlock(block)) {
            return DominionPermission.CONTAINER;
        }

        // Miscellaneous interactables
        if (name.endsWith("_SIGN")
                || type == Material.NOTE_BLOCK
                || name.endsWith("_SHELF")
                || type == Material.FLOWER_POT
                || type == Material.SWEET_BERRY_BUSH
                || type == Material.CAVE_VINES
                || type == Material.CAVE_VINES_PLANT
                || type == Material.DECORATED_POT) {
            return DominionPermission.MISC_INTERACT;
        }

        return null;
    }

    /**
     * Returns true if the block is a container that should be protected.
     */
    private boolean isContainerBlock(Block block) {
        Material type = block.getType();
        return AranarthUtils.isContainerBlock(block)
                || type == Material.FURNACE
                || type == Material.BLAST_FURNACE
                || type == Material.SMOKER
                || type == Material.CRAFTER
                || type == Material.HOPPER
                || type == Material.JUKEBOX
                || type == Material.CHISELED_BOOKSHELF;
    }

    /**
     * Prevents players from placing item frames and paintings in another Dominion.
     */
    @EventHandler
    public void onHangingPlace(HangingPlaceEvent e) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
        if (!aranarthPlayer.isInAdminMode()) {
            if (applyLogic(e.getPlayer(), e.getBlock(), null, DominionPermission.ITEM_FRAME)) {
                e.setCancelled(true);
            }
        }
    }

    /**
     * Prevents players from breaking paintings and item frames in another Dominion.
     * Destroying counts as BUILD.
     */
    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent e) {
        if (e.getRemover() instanceof Player player) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            if (!aranarthPlayer.isInAdminMode()) {
                if (applyLogic(player, null, e.getEntity(), DominionPermission.BUILD)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    /**
     * Controls mob spawning in Dominions using the dominion-wide mob spawning toggle.
     */
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent e) {
        if (e.getEntityType() == EntityType.ARMOR_STAND) {
            return;
        }
        // Only prevent natural spawning of hostile mobs
        boolean isNaturalSpawn = e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL
                || e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.JOCKEY;
        if (!isNaturalSpawn || e.getEntity().getSpawnCategory() != SpawnCategory.MONSTER) {
            return;
        }
        // Only hostile mobs will make it this far
        Dominion chunkDominion = DominionUtils.getDominionOfChunk(e.getLocation().getChunk());
        if (chunkDominion != null) {
            if (!chunkDominion.isMobSpawningEnabled()) {
                e.setCancelled(true);
            }
        }
    }

    /**
     * Prevents players from taking books from lecterns in another Dominion while still allowing them to view the contents.
     */
    @EventHandler
    public void onTakeLecternBook(PlayerTakeLecternBookEvent e) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
        if (aranarthPlayer.isInAdminMode()) {
            return;
        }
        if (applyLogic(e.getPlayer(), e.getLectern().getBlock(), null, DominionPermission.CONTAINER)) {
            e.setCancelled(true);
        }
    }

    /**
     * All validation logic for interacting with another Dominion.
     *
     * @param player     The player performing the action.
     * @param block      The block being interacted with (may be null).
     * @param entity     The entity being interacted with (may be null).
     * @param permission The permission required for this action.
     * @return True if the action should be prevented.
     */
    private boolean applyLogic(Player player, Block block, Entity entity, DominionPermission permission) {
        Dominion dominion = null;
        if (block != null) {
            dominion = DominionUtils.getDominionOfChunk(block.getChunk());
        } else if (entity != null) {
            dominion = DominionUtils.getDominionOfChunk(entity.getLocation().getChunk());
        }

        if (dominion != null) {
            if (!DominionUtils.hasPermission(player, dominion, permission)) {
                long now = System.currentTimeMillis();
                Long last = lastDenyMessageTime.get(player.getUniqueId());
                if (last == null || now - last >= DENY_MESSAGE_COOLDOWN_MS) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to do this in &e" + dominion.getName()));
                    lastDenyMessageTime.put(player.getUniqueId(), now);
                }
                return true;
            }
        }
        return false;
    }
}
