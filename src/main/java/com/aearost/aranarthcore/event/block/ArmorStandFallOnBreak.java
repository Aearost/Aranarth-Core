package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Makes locked armor stands fall under vanilla gravity when the block beneath them is destroyed.
 * The stand re-locks itself once it lands on a solid surface.
 */
public class ArmorStandFallOnBreak {

    private static final NamespacedKey LOCKED_KEY = new NamespacedKey(AranarthCore.getInstance(), "armor_stand_locked");

    public void execute(BlockBreakEvent e) {
        Block block = e.getBlock();
        Location center = block.getLocation().add(0.5, 1.0, 0.5);
        for (Entity entity : block.getWorld().getNearbyEntities(center, 0.5, 0.1, 0.5)) {
            if (!(entity instanceof ArmorStand armorStand)) {
                continue;
            }
            PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
            if (!pdc.has(LOCKED_KEY, PersistentDataType.BYTE)) {
                continue;
            }

            armorStand.setCanMove(true);
            armorStand.setGravity(true);
            scheduleLandingCheck(armorStand);
        }
    }

    /**
     * Polls every tick until the armor stand is resting on a solid block, then re-locks it.
     * Skips the first few ticks so the support block has time to be removed before checking.
     * Times out after ~10 seconds to avoid leaking tasks if the stand falls into a void.
     */
    private void scheduleLandingCheck(ArmorStand armorStand) {
        new BukkitRunnable() {
            private int elapsed = 0;

            @Override
            public void run() {
                if (!armorStand.isValid()) {
                    cancel();
                    return;
                }

                if (++elapsed > 200) {
                    cancel();
                    return;
                }

                // Wait a few ticks for the broken block to be removed before checking
                if (elapsed < 5) {
                    return;
                }

                Block beneath = armorStand.getLocation().subtract(0, 0.05, 0).getBlock();
                if (beneath.getType().isSolid()) {
                    armorStand.setGravity(false);
                    armorStand.setCanMove(false);
                    cancel();
                }
            }
        }.runTaskTimer(AranarthCore.getInstance(), 1L, 1L);
    }
}
