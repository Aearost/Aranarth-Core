package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.event.block.*;
import com.aearost.aranarthcore.event.player.HomepadBreak;
import com.aearost.aranarthcore.event.player.ShopDestroy;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_TYPE;

/**
 * Centralizes all logic to be called by blocks being broken.
 */
public class BlockBreakEventListener implements Listener {

    public BlockBreakEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        String worldName = e.getBlock().getWorld().getName();
        if (worldName.startsWith("world") || worldName.startsWith("smp") || worldName.startsWith("resource")) {
            UUID uuid = e.getPlayer().getUniqueId();
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);

            // Prevents recursive calls, limits special functionality i.e extra ore drops to only the destroyed block itself
            if (aranarthPlayer.getPlentifulBlocksToDestroy() > 0) {
                aranarthPlayer.setPlentifulBlocksToDestroy(aranarthPlayer.getPlentifulBlocksToDestroy() - 1);
            } else {
                ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
                if (heldItem.hasItemMeta() && heldItem.getItemMeta().getPersistentDataContainer().has(INCANTATION_TYPE)) {
                    String type = heldItem.getItemMeta().getPersistentDataContainer().get(INCANTATION_TYPE, PersistentDataType.STRING);
                    if (type.equals("incantation_plentiful")) {
                        aranarthPlayer.setPlentifulBlocksToDestroy(9);
                        new IncantationPlentifulBlockBreak().execute(e);
                    }
                }
            }
            AranarthUtils.setPlayer(uuid, aranarthPlayer);
        }

        // Blocks that are not necessarily destroyed
        if (AranarthUtils.isBlockCrop(e.getBlock().getType())) {
            new CropHarvest().execute(e);
        } else {
            Material type = e.getBlock().getType();

            if (type == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
                new HomepadBreak().execute(e);
            } else if (type == Material.BUDDING_AMETHYST) {
                new BuddingAmethystBreak().execute(e);
            } else if (type == Material.TORCHFLOWER || type == Material.TORCHFLOWER_CROP || hasLightCropAbove(e.getBlock(), "TORCHFLOWER")) {
                new TorchflowerBreak().execute(e);
            } else if (type == Material.PITCHER_PLANT || type == Material.PITCHER_CROP || hasLightCropAbove(e.getBlock(), "PITCHER_PLANT")) {
                new PitcherPlantBreak().execute(e);
            } else {
                if (e.getBlock().getType().name().endsWith("_ORE")) {
                    new OreClusterDrops().execute(e);
                    new OreExtraDrops().execute(e);
                }
            }

            new ShopDestroy().execute(e);
            new ContainerBreak().execute(e);

            if (AranarthUtils.getMonth() == Month.FOLLIVOR) {
                new LogExtraDrops().execute(e);
            }
        }
    }

    private boolean hasLightCropAbove(Block block, String name) {
        Location location = block.getLocation();
        Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());
        if (name.equals("TORCHFLOWER") && (locationAbove.getBlock().getType() == Material.TORCHFLOWER || locationAbove.getBlock().getType() == Material.TORCHFLOWER_CROP)) {
            return true;
        } else if (name.equals("PITCHER_PLANT") && (locationAbove.getBlock().getType() == Material.PITCHER_PLANT || locationAbove.getBlock().getType() == Material.PITCHER_CROP)) {
            return true;
        }
        return false;
    }
}
