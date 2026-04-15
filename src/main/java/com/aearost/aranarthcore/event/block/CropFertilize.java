package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.CropUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.BlockFertilizeEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies the current month's crop growth speed modifier to bone meal usage on crops.
 * Intercepts BlockFertilizeEvent so that bone meal respects seasonal speed multipliers
 * rather than being treated as a single natural growth tick by CropGrowBoost.
 */
public class CropFertilize {

    public void execute(BlockFertilizeEvent e) {
        Block block = e.getBlock();
        if (!(block.getBlockData() instanceof Ageable currentCrop)) return;

        Material seedKey = CropUtils.getSeedMaterial(block.getType());
        double speed = CropUtils.getCropGrowthSpeed(AranarthUtils.getMonth(), seedKey);

        // Speed should not go below 1x for nether wart in the nether
        if (seedKey == Material.NETHER_WART && block.getWorld().getName().endsWith("_nether")) {
            if (speed < 1.0) speed = 1.0;
        }

        // At 1x speed let vanilla bone meal run normally
        if (speed == 1.0) return;

        // Determine the age vanilla bone meal would produce by reading the event's block list
        int currentAge = currentCrop.getAge();
        int vanillaNewAge = currentAge;
        for (BlockState state : e.getBlocks()) {
            if (state.getLocation().equals(block.getLocation())
                    && state.getBlockData() instanceof Ageable vanillaCrop) {
                vanillaNewAge = vanillaCrop.getAge();
                break;
            }
        }

        int vanillaAdvance = vanillaNewAge - currentAge;
        if (vanillaAdvance <= 0) return;

        int scaledAdvance;
        if (speed > 1.0) {
            double totalGrowth = vanillaAdvance * speed;
            int base = (int) totalGrowth;
            double fracChance = totalGrowth - base;
            scaledAdvance = base + (ThreadLocalRandom.current().nextDouble() < fracChance ? 1 : 0);
        } else {
            // Reduce the bone meal advance proportionally, but guarantee at least 1 stage
            // so bone meal is never wasted entirely in an off-season
            scaledAdvance = Math.max(1, (int) (vanillaAdvance * speed));
        }

        int newAge = Math.min(currentAge + scaledAdvance, currentCrop.getMaximumAge());

        // Cancel vanilla growth and apply the speed-adjusted result directly so that
        // BlockGrowEvent is not fired and CropGrowBoost does not re-process this tick
        e.setCancelled(true);
        currentCrop.setAge(newAge);
        block.setBlockData(currentCrop);
    }
}
