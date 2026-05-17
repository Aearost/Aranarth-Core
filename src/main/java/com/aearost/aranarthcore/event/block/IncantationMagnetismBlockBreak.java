package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

import static com.aearost.aranarthcore.objects.CustomKeys.MAGNETISM_TAG;
import static com.aearost.aranarthcore.objects.CustomKeys.MAGNETISM_TOOL_ID;

/**
 * Handles logic when a block is broken with a tool that has the Magnetism incantation applied.
 * Records the break location so items spawning there are tagged, and drives the sneak-pull mechanic.
 */
public class IncantationMagnetismBlockBreak {

    private static final Map<String, String> activeBreakLocations = new HashMap<>();

    public void execute(BlockBreakEvent e) {
        Player player = e.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!heldItem.hasItemMeta()) {
            return;
        }

        String name = heldItem.getType().name();
        boolean isCorrectTool;
        if (name.endsWith("_PICKAXE")) {
            isCorrectTool = AranarthUtils.isHarvestableWithPickaxe(e.getBlock().getType());
        } else if (name.endsWith("_AXE")) {
            isCorrectTool = AranarthUtils.isHarvestableWithAxe(e.getBlock().getType());
        } else if (name.endsWith("_SHOVEL")) {
            isCorrectTool = AranarthUtils.isHarvestableWithShovel(e.getBlock().getType());
        } else if (name.endsWith("_HOE")) {
            isCorrectTool = AranarthUtils.isBlockCrop(e.getBlock().getType());
        } else {
            isCorrectTool = false;
        }

        if (!isCorrectTool) {
            return;
        }

        String toolId = heldItem.getItemMeta().getPersistentDataContainer().get(MAGNETISM_TOOL_ID, PersistentDataType.STRING);
        if (toolId == null) {
            return;
        }

        Location loc = e.getBlock().getLocation();
        String locationKey = toLocationKey(loc);
        activeBreakLocations.put(locationKey, toolId);

        // All block drops (including cluster/extra drops) should have spawned by then.
        new BukkitRunnable() {
            @Override
            public void run() {
                activeBreakLocations.remove(locationKey);
            }
        }.runTaskLater(AranarthCore.getInstance(), 1L);
    }

    /**
     * Returns the magnetism tool UUID for any item spawning at the given location this tick, or null if none.
     */
    public static String getToolIdForSpawn(Location loc) {
        return activeBreakLocations.get(toLocationKey(loc));
    }

    /**
     * Pulls item entities tagged with the held magnetism tool's UUID towards the player holding the tool.
     */
    public static void tickMagnetismPull() {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!player.isSneaking()) {
                continue;
            }

            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (!heldItem.hasItemMeta()) {
                continue;
            }

            var pdc = heldItem.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(MAGNETISM_TOOL_ID)) {
                continue;
            }

            String toolId = pdc.get(MAGNETISM_TOOL_ID, PersistentDataType.STRING);
            if (toolId == null) {
                continue;
            }

            Location playerLoc = player.getLocation().add(0, 1, 0);
            for (Entity nearby : player.getNearbyEntities(5, 5, 5)) {
                if (!(nearby instanceof Item item)) {
                    continue;
                }

                if (!item.getPersistentDataContainer().has(MAGNETISM_TAG)) {
                    continue;
                }

                String itemTag = item.getPersistentDataContainer().get(MAGNETISM_TAG, PersistentDataType.STRING);
                if (!toolId.equals(itemTag)) {
                    continue;
                }

                Vector toPlayer = playerLoc.toVector().subtract(item.getLocation().toVector());
                double distance = toPlayer.length();
                if (distance > 0.5) {
                    item.setVelocity(toPlayer.normalize().multiply(0.4));
                }
            }
        }
    }

    private static String toLocationKey(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "," + loc.getWorld().getName();
    }
}
