package com.aearost.aranarthcore.event.block;

import com.gmail.nossr50.mcMMO;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Caps gold nugget drops from player-placed gilded blackstone to prevent market exploits.
 */
public class GildedBlackstoneBreak {
	public void execute(BlockBreakEvent e) {
		if (mcMMO.getChunkManager().isEligible(e.getBlock())) {
			return;
		}
		e.setDropItems(false);
		for (ItemStack drop : e.getBlock().getDrops(e.getPlayer().getInventory().getItemInMainHand())) {
			if (drop.getType() == Material.GOLD_NUGGET && drop.getAmount() > 4) {
				drop.setAmount(4);
			}
			e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
		}
	}
}
