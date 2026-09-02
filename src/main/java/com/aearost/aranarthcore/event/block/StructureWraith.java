package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.loot.Lootable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns a Wraith mob the first time a player opens an unopened structure chest.
 */
public class StructureWraith {

    public enum WraithType {
        ANCIENT_CITY,
        MINESHAFT,
        STRONGHOLD,
        DESERT_PYRAMID,
        FORTRESS,
        JUNGLE_PYRAMID,
        OCEAN_RUINS,
        SHIPWRECK
    }

    public void execute(PlayerInteractEvent e) {
        // Fires once per interaction (avoid double-fire from off-hand)
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }

        Material type = block.getType();
        if (type != Material.CHEST && type != Material.TRAPPED_CHEST && type != Material.BARREL) {
            return;
        }

        BlockState state = block.getState();
        if (!(state instanceof Lootable lootable) || !(state instanceof TileState tileState)) {
            return;
        }

        LootTable lootTable = lootable.getLootTable();
        if (lootTable == null) {
            return;
        }

        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        if (pdc.has(CustomKeys.WRAITH_TRIGGERED, PersistentDataType.BYTE)) {
            return;
        }

        WraithType wraithType = resolveWraithType(lootTable);
        if (wraithType == null) {
            return;
        }

        // Mark this chest so the check never fires again, regardless of the 50% roll
        pdc.set(CustomKeys.WRAITH_TRIGGERED, PersistentDataType.BYTE, (byte) 1);
        tileState.update();

        // 50% chance to actually spawn a Wraith
        if (ThreadLocalRandom.current().nextBoolean()) {
            return;
        }

        e.setCancelled(true);

        Player player = e.getPlayer();
        player.playSound(block.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.75f);
        player.sendMessage(ChatUtils.chatMessage("&cA Wraith has started to attack you!"));

        spawnWraith(wraithType, block);
    }

    private WraithType resolveWraithType(LootTable lootTable) {
        if (matches(lootTable, LootTables.ANCIENT_CITY, LootTables.ANCIENT_CITY_ICE_BOX)) {
            return WraithType.ANCIENT_CITY;
        }
        if (matches(lootTable, LootTables.ABANDONED_MINESHAFT)) {
            return WraithType.MINESHAFT;
        }
        if (matches(lootTable, LootTables.STRONGHOLD_CORRIDOR, LootTables.STRONGHOLD_CROSSING, LootTables.STRONGHOLD_LIBRARY)) {
            return WraithType.STRONGHOLD;
        }
        if (matches(lootTable, LootTables.DESERT_PYRAMID)) {
            return WraithType.DESERT_PYRAMID;
        }
        if (matches(lootTable, LootTables.NETHER_BRIDGE)) {
            return WraithType.FORTRESS;
        }
        if (matches(lootTable, LootTables.JUNGLE_TEMPLE)) {
            return WraithType.JUNGLE_PYRAMID;
        }
        if (matches(lootTable, LootTables.UNDERWATER_RUIN_BIG, LootTables.UNDERWATER_RUIN_SMALL)) {
            return WraithType.OCEAN_RUINS;
        }
        if (matches(lootTable, LootTables.SHIPWRECK_SUPPLY, LootTables.SHIPWRECK_TREASURE, LootTables.SHIPWRECK_MAP)) {
            return WraithType.SHIPWRECK;
        }
        return null;
    }

    private boolean matches(LootTable lootTable, LootTables... candidates) {
        for (LootTables candidate : candidates) {
            if (candidate.getLootTable().getKey().equals(lootTable.getKey())) {
                return true;
            }
        }
        return false;
    }

    private void spawnWraith(WraithType wraithType, Block chest) {
        World world = chest.getWorld();
        Location loc = chest.getLocation().add(0.5, 1, 0.5);
        switch (wraithType) {
            case ANCIENT_CITY   -> spawnSilverfish(world, loc, wraithType);
            case MINESHAFT      -> spawnCaveSpider(world, loc, wraithType);
            case STRONGHOLD     -> spawnStrongholdZombie(world, loc, wraithType);
            case DESERT_PYRAMID -> spawnParched(world, loc, wraithType);
            case FORTRESS       -> spawnFortress(world, loc, wraithType);
            case JUNGLE_PYRAMID -> spawnBogged(world, loc, wraithType);
            case OCEAN_RUINS    -> spawnDrownedTrident(world, loc, wraithType);
            case SHIPWRECK      -> spawnDrowned(world, loc, wraithType);
        }
    }

    private void tagWraith(LivingEntity entity, WraithType wraithType) {
        entity.getPersistentDataContainer().set(CustomKeys.WRAITH_TYPE, PersistentDataType.STRING, wraithType.name());
    }

    private void spawnSilverfish(World world, Location loc, WraithType wraithType) {
        world.spawn(loc, Silverfish.class, mob -> {
            mob.setCustomName(ChatUtils.translateToColor("&cAncient City Wraith"));
            mob.setCustomNameVisible(true);
            setHealth(mob, 16.0);
            tagWraith(mob, wraithType);
        });
    }

    private void spawnCaveSpider(World world, Location loc, WraithType wraithType) {
        world.spawn(loc, CaveSpider.class, mob -> {
            mob.setCustomName(ChatUtils.translateToColor("&cMineshaft Wraith"));
            mob.setCustomNameVisible(true);
            setHealth(mob, 18.0);
            tagWraith(mob, wraithType);
        });
    }

    private void spawnStrongholdZombie(World world, Location loc, WraithType wraithType) {
        world.spawn(loc, Zombie.class, mob -> {
            mob.setCustomName(ChatUtils.translateToColor("&cStronghold Wraith"));
            mob.setCustomNameVisible(true);
            setHealth(mob, 30.0);
            tagWraith(mob, wraithType);

            EntityEquipment equip = mob.getEquipment();
            if (equip != null) {
                equip.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                equip.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                equip.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                equip.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
                equip.setHelmetDropChance(0.2f);
                equip.setChestplateDropChance(0.2f);
                equip.setLeggingsDropChance(0.2f);
                equip.setBootsDropChance(0.2f);
            }
        });
    }

    private void spawnParched(World world, Location loc, WraithType wraithType) {
        world.spawn(loc, Husk.class, mob -> {
            mob.setCustomName(ChatUtils.translateToColor("&cParched Wraith"));
            mob.setCustomNameVisible(true);
            setHealth(mob, 30.0);
            tagWraith(mob, wraithType);
        });
    }

    private void spawnFortress(World world, Location loc, WraithType wraithType) {
        world.spawn(loc, WitherSkeleton.class, mob -> {
            mob.setCustomName(ChatUtils.translateToColor("&cFortress Wraith"));
            mob.setCustomNameVisible(true);
            setHealth(mob, 30.0);
            tagWraith(mob, wraithType);
        });
    }

    private void spawnBogged(World world, Location loc, WraithType wraithType) {
        world.spawn(loc, Bogged.class, mob -> {
            mob.setCustomName(ChatUtils.translateToColor("&cJungle Pyramid Wraith"));
            mob.setCustomNameVisible(true);
            setHealth(mob, 26.0);
            tagWraith(mob, wraithType);
        });
    }

    private void spawnDrownedTrident(World world, Location loc, WraithType wraithType) {
        world.spawn(loc, Drowned.class, mob -> {
            mob.setCustomName(ChatUtils.translateToColor("&cOcean Ruin Wraith"));
            mob.setCustomNameVisible(true);
            setHealth(mob, 30.0);
            tagWraith(mob, wraithType);

            EntityEquipment equip = mob.getEquipment();
            if (equip != null) {
                equip.setItemInMainHand(new ItemStack(Material.TRIDENT));
                equip.setItemInMainHandDropChance(0.0f);
            }
        });
    }

    private void spawnDrowned(World world, Location loc, WraithType wraithType) {
        world.spawn(loc, Drowned.class, mob -> {
            mob.setCustomName(ChatUtils.translateToColor("&cShipwreck Wraith"));
            mob.setCustomNameVisible(true);
            setHealth(mob, 26.0);
            tagWraith(mob, wraithType);
        });
    }

    private void setHealth(LivingEntity entity, double health) {
        if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
        }
        entity.setHealth(health);
    }
}
