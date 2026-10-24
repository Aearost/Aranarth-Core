package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.*;
import com.aearost.aranarthcore.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Enforces Dominion block and entity protection rules with configurable per-rank/relation permissions.
 */
public class DominionProtectionListener implements Listener {

    private static final Logger log = LoggerFactory.getLogger(DominionProtectionListener.class);
    private static final long DENY_MESSAGE_COOLDOWN_MS = 1000L;
    private final Map<UUID, Long> lastDenyMessageTime = new HashMap<>();

    /**
     * Players who have been standing on the ground inside a dominion since last entering one.
     */

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
        Dominion chunkDominion = DominionUtils.getDominionOfChunkAnywhere(block.getChunk());
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
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.no_permission_in", "name", chunkDominion.getName())));
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
                Dominion blockDominion = DominionUtils.getDominionOfChunkAnywhere(e.getBlock().getChunk());
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
        } else if (isProtectedCreature(e.getRightClicked())) {
            // Riding, leashing, shearing, bucketing, opening chested animals and taking items from allays
            // Players can always use their own tamed animals
            if (e.getRightClicked() instanceof Tameable tameable && tameable.getOwner() != null
                    && tameable.getOwner().getUniqueId().equals(e.getPlayer().getUniqueId())) {
                return;
            }
            if (applyLogic(e.getPlayer(), null, e.getRightClicked(), DominionPermission.MISC_INTERACT)) {
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
     * Returns true if the entity is a non-hostile creature that players should not be able to use in
     * another Dominion.
     */
    private boolean isProtectedCreature(Entity entity) {
        return entity instanceof Animals
                || entity instanceof WaterMob
                || entity instanceof Allay
                || entity instanceof Snowman;
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
                    Entity rider = Bukkit.getEntity(UUID.fromString(info[0]));
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
                    Entity rider = Bukkit.getEntity(UUID.fromString(info[0]));
                    if (rider instanceof Player owner) {
                        handleMountDamage(attacker, owner, e);
                    }
                }
            }
        }
        // Passive mobs in dominion land
        else if (e.getEntity() instanceof Animals) {
            if (e.getDamageSource().getCausingEntity() instanceof Player attacker) {
                handlePassiveMobDamage(attacker, e);
            }
        }
    }

    /**
     * Handles damage to passive mobs in dominion land.
     * Only dominion members and enemied dominions may harm passive mobs in claimed land.
     */
    private void handlePassiveMobDamage(Player attacker, EntityDamageEvent e) {
        Dominion chunkDominion = DominionUtils.getDominionOfChunkAnywhere(e.getEntity().getLocation().getChunk());
        if (chunkDominion == null) {
            return;
        }

        AranarthPlayer aranarthAttacker = AranarthUtils.getPlayer(attacker.getUniqueId());
        if (aranarthAttacker.isInAdminMode()) {
            return;
        }

        Dominion attackerDominion = DominionUtils.getPlayerDominion(attacker.getUniqueId());

        // Same dominion - always allowed
        if (attackerDominion != null && attackerDominion.isSameDominion(chunkDominion)) {
            return;
        }

        // Enemied dominion - allowed
        if (attackerDominion != null && attackerDominion.isEnemied(chunkDominion)) {
            return;
        }

        // All other outsiders (neutral, allied, truced, wanderers) - blocked
        e.setCancelled(true);
        long now = System.currentTimeMillis();
        Long last = lastDenyMessageTime.get(attacker.getUniqueId());
        if (last == null || now - last >= DENY_MESSAGE_COOLDOWN_MS) {
            attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "dominion.cannot_harm_passive", "name", chunkDominion.getName())));
            lastDenyMessageTime.put(attacker.getUniqueId(), now);
        }
    }

    /**
     * Handles PvP between players with relation-based and chunk-based rules.
     */
    private void handlePvP(Player attacker, Player target, EntityDamageEvent e) {
        String denyMessage = DominionUtils.getPvpDenyMessage(attacker, target);
        if (denyMessage != null) {
            e.setCancelled(true);
            attacker.sendMessage(ChatUtils.chatMessage(denyMessage));
        }
    }

    /**
     * Handles damage to tamed pets using PvP permission and relation-based defaults.
     */
    private void handlePetDamage(Player attacker, Tameable pet, EntityDamageEvent e) {
        Dominion attackerDominion = DominionUtils.getPlayerDominion(attacker.getUniqueId());
        boolean petHasOwner = pet.getOwner() != null;
        Dominion targetDominion = petHasOwner ? DominionUtils.getPlayerDominion(pet.getOwner().getUniqueId()) : null;

        // Attacker has a dominion, pet owner is a wanderer - blocked if owner is not a barbarian
        if (attackerDominion != null && targetDominion == null) {
            if (petHasOwner) {
                AranarthPlayer aranarthOwnerPlayer = AranarthUtils.getPlayer(pet.getOwner().getUniqueId());
                if (!aranarthOwnerPlayer.isBarbarian()) {
                    e.setCancelled(true);
                    String petType = ChatUtils.getFormattedItemName(pet.getType().name());
                    attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "dominion.cannot_harm_pet_relation", "owner", aranarthOwnerPlayer.getNickname(), "pet", petType, "relation", "a wanderer")));
                }
            }
            return;
        }

        // Attacker is a wanderer, pet owner has a dominion - blocked if attacker is not a barbarian
        if (attackerDominion == null && targetDominion != null) {
            AranarthPlayer aranarthAttacker = AranarthUtils.getPlayer(attacker.getUniqueId());
            if (!aranarthAttacker.isBarbarian()) {
                e.setCancelled(true);
                return;
            }
            // Barbarian wanderer attacking a dominion member's pet - fall through to relation-based logic below
        }

        // Both are wanderers (neither has a dominion) - apply barbarian rules
        if (attackerDominion == null && targetDominion == null) {
            AranarthPlayer aranarthAttacker = AranarthUtils.getPlayer(attacker.getUniqueId());
            if (!aranarthAttacker.isBarbarian()) {
                e.setCancelled(true);
                return;
            }
            if (petHasOwner) {
                AranarthPlayer aranarthOwnerPlayer = AranarthUtils.getPlayer(pet.getOwner().getUniqueId());
                if (!aranarthOwnerPlayer.isBarbarian()) {
                    e.setCancelled(true);
                    String petType = ChatUtils.getFormattedItemName(pet.getType().name());
                    attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "dominion.cannot_harm_pet_relation", "owner", aranarthOwnerPlayer.getNickname(), "pet", petType, "relation", "a wanderer")));
                }
            }
            return;
        }

        AranarthPlayer aranarthOwner = AranarthUtils.getPlayer(pet.getOwner().getUniqueId());

        if (AranarthUtils.isSpawnLocation(pet.getLocation())) {
            return;
        }
        if (pet.getOwner().getUniqueId().equals(attacker.getUniqueId())) {
            return;
        }

        Dominion chunkDominion = DominionUtils.getDominionOfChunkAnywhere(pet.getLocation().getChunk());
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
            String relationName = DominionUtils.getFormattedRankName(attacker, attackerRelation);
            if (attackerDominion.isSameDominion(chunkDominion)) {
                attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "dominion.cannot_harm_pet", "owner", aranarthOwner.getNickname(), "pet", petType, "name", chunkDominion.getName())));
            } else {
                attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "dominion.cannot_harm_pet_relation", "owner", aranarthOwner.getNickname(), "pet", petType, "relation", relationName)));
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

        // Same dominion - check per-area PvP flag (outpost overrides dominion flag)
        if (attackerDominion != null && ownerDominion != null
                && attackerDominion.isSameDominion(ownerDominion)) {
            Outpost fightOutpost = OutpostUtils.getOutpostOfChunk(attacker.getLocation().getChunk());
            boolean pvpEnabled = (fightOutpost != null && fightOutpost.getDominionId().equals(attackerDominion.getId()))
                    ? fightOutpost.isMemberPvpEnabled()
                    : attackerDominion.isMemberPvpEnabled();
            if (!pvpEnabled) {
                e.setCancelled(true);
                attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "dominion.cannot_harm_pet", "owner", aranarthOwner.getNickname(), "pet", mountTypeName, "name", attackerDominion.getName())));
            }
            return;
        }

        // Both players belong to different dominions
        if (attackerDominion != null && ownerDominion != null) {
            DominionRank relation = DominionUtils.getRelationKey(attackerDominion, ownerDominion);
            Dominion chunkDominion = DominionUtils.getDominionOfChunkAnywhere(e.getEntity().getLocation().getChunk());

            if (relation == DominionRank.ALLIED || relation == DominionRank.TRUCED) {
                // Either dominion having PvP enabled for this relation allows the attack
                boolean attackerPvp = attackerDominion.getDominionPermissions().hasPermission(relation, DominionPermission.PVP);
                boolean ownerPvp = ownerDominion.getDominionPermissions().hasPermission(relation, DominionPermission.PVP);
                if (!attackerPvp && !ownerPvp) {
                    e.setCancelled(true);
                    attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "dominion.cannot_harm_pet_relation", "owner", aranarthOwner.getNickname(), "pet", mountTypeName, "relation", DominionUtils.getFormattedRankName(relation))));
                }
                return;
            }

            if (relation == DominionRank.NEUTRAL) {
                // Blocked only when the mount owner is in their own dominion's land
                if (chunkDominion != null && chunkDominion.isSameDominion(ownerDominion)) {
                    e.setCancelled(true);
                    attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "dominion.cannot_harm_pet_relation", "owner", aranarthOwner.getNickname(), "pet", mountTypeName, "relation", DominionUtils.getFormattedRankName(DominionRank.NEUTRAL))));
                }
                return;
            }

            if (relation == DominionRank.ENEMIED) {
                // Always allowed
                return;
            }
            return;
        }

        // Attacker has a dominion, mount owner is a wanderer - blocked if owner is not a barbarian
        if (attackerDominion != null) {
            if (!aranarthOwner.isBarbarian()) {
                e.setCancelled(true);
                attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "dominion.cannot_harm_pet_relation", "owner", aranarthOwner.getNickname(), "pet", mountTypeName, "relation", "a wanderer")));
            }
            return;
        }

        // Attacker is a wanderer, mount owner has a dominion
        if (ownerDominion != null) {
            AranarthPlayer aranarthAttacker = AranarthUtils.getPlayer(attacker.getUniqueId());
            if (!aranarthAttacker.isBarbarian()) {
                e.setCancelled(true);
                return;
            }
            Dominion chunkDominion = DominionUtils.getDominionOfChunkAnywhere(e.getEntity().getLocation().getChunk());
            if (chunkDominion != null && chunkDominion.isSameDominion(ownerDominion)) {
                e.setCancelled(true);
                attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "dominion.cannot_harm_pet_relation", "owner", aranarthOwner.getNickname(), "pet", mountTypeName, "relation", "a wanderer in their dominion's lands")));
            }
            return;
        }

        // Both are wanderers (neither has a dominion)
        AranarthPlayer aranarthAttacker = AranarthUtils.getPlayer(attacker.getUniqueId());
        if (!aranarthAttacker.isBarbarian()) {
            e.setCancelled(true);
            return;
        }
        if (!aranarthOwner.isBarbarian()) {
            e.setCancelled(true);
            attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "dominion.cannot_harm_pet_relation", "owner", aranarthOwner.getNickname(), "pet", mountTypeName, "relation", "a wanderer")));
        }
        // Both are barbarians - allowed
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
            Dominion chunkDominion = DominionUtils.getDominionOfChunkAnywhere(block.getChunk());
            if (chunkDominion == null) {
                return; // unclaimed land - no restriction here
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
                || name.startsWith("POTTED_")
                || type == Material.SWEET_BERRY_BUSH
                || type == Material.CAVE_VINES
                || type == Material.CAVE_VINES_PLANT
                || type == Material.DECORATED_POT
                || name.endsWith("_BED")
                || type == Material.RESPAWN_ANCHOR) {
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
        // Projectiles count as the player who shot them
        Entity remover = e.getRemover();
        if (remover instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            remover = shooter;
        }
        if (remover instanceof Player player) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            if (!aranarthPlayer.isInAdminMode()) {
                if (applyLogic(player, null, e.getEntity(), DominionPermission.BUILD)) {
                    e.setCancelled(true);
                }
            }
        } else if (DominionUtils.getDominionOfChunkAnywhere(e.getEntity().getLocation().getChunk()) != null) {
            // Mobs and other non-player removers cannot break item frames or paintings in claimed land
            e.setCancelled(true);
        }
    }

    /**
     * Prevents water and lava from flowing into another Dominion's land.
     */
    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (isEnteringOtherDominion(e.getBlock(), e.getToBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (isPistonCrossingDominion(e.getBlock(), e.getBlocks(), e.getDirection())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (isPistonCrossingDominion(e.getBlock(), e.getBlocks(), e.getDirection())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents dispensers and droppers from placing fluids, blocks, or entities into another Dominion.
     */
    @EventHandler(ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent e) {
        Block block = e.getBlock();
        if (block.getBlockData() instanceof Directional directional
                && isEnteringOtherDominion(block, block.getRelative(directional.getFacing()))) {
            e.setCancelled(true);
        }
    }

    /**
     * Provides whether a piston would move any block out of or into a Dominion that does not own the piston.
     */
    private boolean isPistonCrossingDominion(Block piston, List<Block> moved, BlockFace direction) {
        if (isEnteringOtherDominion(piston, piston.getRelative(direction))) {
            return true;
        }
        for (Block block : moved) {
            if (isEnteringOtherDominion(piston, block) || isEnteringOtherDominion(piston, block.getRelative(direction))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Provides whether the target block is in a Dominion (or one of its outposts) that does not also
     * own the source block.
     */
    private boolean isEnteringOtherDominion(Block source, Block target) {
        if ((source.getX() >> 4) == (target.getX() >> 4) && (source.getZ() >> 4) == (target.getZ() >> 4)) {
            return false;
        }
        Dominion targetDominion = DominionUtils.getDominionOfChunkAnywhere(target.getChunk());
        if (targetDominion == null) {
            return false;
        }
        Dominion sourceDominion = DominionUtils.getDominionOfChunkAnywhere(source.getChunk());
        return sourceDominion == null || !sourceDominion.isSameDominion(targetDominion);
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
        // Only hostile mobs will make it this far - check per-outpost flag first, then dominion flag
        Chunk spawnChunk = e.getLocation().getChunk();
        Outpost spawnOutpost = OutpostUtils.getOutpostOfChunk(spawnChunk);
        if (spawnOutpost != null) {
            if (!spawnOutpost.isMobSpawningEnabled()) {
                e.setCancelled(true);
            }
        } else {
            Dominion chunkDominion = DominionUtils.getDominionOfChunk(spawnChunk);
            if (chunkDominion != null && !chunkDominion.isMobSpawningEnabled()) {
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
            dominion = DominionUtils.getDominionOfChunkAnywhere(block.getChunk());
        } else if (entity != null) {
            dominion = DominionUtils.getDominionOfChunkAnywhere(entity.getLocation().getChunk());
        }

        if (dominion != null) {
            // Check region assignments - public regions use the normal Dominion permissions below
            Chunk targetChunk = block != null ? block.getChunk() : entity.getLocation().getChunk();
            String chunkKey = targetChunk.getWorld().getName() + ":" + targetChunk.getX() + ":" + targetChunk.getZ();
            String regionName = dominion.getRegionChunkNames().get(chunkKey);
            if (regionName != null && !dominion.isRegionPublic(regionName)) {
                Set<UUID> regionOwners = dominion.getRegionMembers().get(regionName);
                UUID playerUuid = player.getUniqueId();
                if (playerUuid.equals(dominion.getLeader())
                        || (regionOwners != null && regionOwners.contains(playerUuid))
                        || DominionUtils.hasPermission(player, dominion, DominionPermission.MANAGE_REGIONS)) {
                    return false; // Region owner, leader, or bypass permission
                }
                if (dominion.getMembers().contains(playerUuid)) {
                    // Dominion member who does not own this region and lacks bypass, deny them
                    long now = System.currentTimeMillis();
                    Long last = lastDenyMessageTime.get(playerUuid);
                    if (last == null || now - last >= DENY_MESSAGE_COOLDOWN_MS) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_not_yours")));
                        lastDenyMessageTime.put(playerUuid, now);
                    }
                    return true;
                }
            }

            if (!DominionUtils.hasPermission(player, dominion, permission)) {
                long now = System.currentTimeMillis();
                Long last = lastDenyMessageTime.get(player.getUniqueId());
                if (last == null || now - last >= DENY_MESSAGE_COOLDOWN_MS) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.no_permission_in", "name", dominion.getName())));
                    lastDenyMessageTime.put(player.getUniqueId(), now);
                }
                return true;
            }
        }
        return false;
    }
}
