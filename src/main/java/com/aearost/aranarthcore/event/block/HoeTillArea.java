package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.utils.DominionLevelUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_TYPE;

/**
 * Tills the surrounding 3x3 area for the incantation of plentiful.
 */
public class HoeTillArea {

    public void execute(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block center = e.getClickedBlock();
        if (center == null) {
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!heldItem.hasItemMeta()) {
            return;
        }
        String incantationType = heldItem.getItemMeta().getPersistentDataContainer().get(INCANTATION_TYPE, PersistentDataType.STRING);
        if (!"incantation_plentiful".equals(incantationType)) {
            return;
        }

        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        // Center block is handled by the vanilla event
                        continue;
                    }

                    Block block = center.getWorld().getBlockAt(cx + dx, cy + dy, cz + dz);
                    Material type = block.getType();

                    if (type != Material.DIRT && type != Material.GRASS_BLOCK) {
                        continue;
                    }

                    // Vanilla tilling requires the block directly above to be non-solid
                    if (block.getRelative(0, 1, 0).getType().isSolid()) {
                        continue;
                    }

                    // Dominion permission check
                    Dominion chunkDominion = DominionUtils.getDominionOfChunk(block.getChunk());
                    if (chunkDominion != null) {
                        boolean ownsDominion = playerDominion != null && playerDominion.isSameDominion(chunkDominion);
                        boolean hasBuildPermission = DominionUtils.hasPermission(player, chunkDominion, DominionPermission.BUILD);
                        if (!ownsDominion && !hasBuildPermission) {
                            continue;
                        }
                    }

                    block.setType(Material.FARMLAND);
                    block.getWorld().playSound(block.getLocation(), Sound.ITEM_HOE_TILL, 1.0F, 1.0F);

                    // Track farmland for dominion leveling
                    Dominion dominion = DominionUtils.getDominionOfChunkAnywhere(block.getChunk());
                    if (dominion != null) {
                        dominion.setCachedFarmlandCount(dominion.getCachedFarmlandCount() + 1);
                        DominionLevelUtils.reevaluateDominion(dominion);
                    }
                }
            }
        }
    }
}
