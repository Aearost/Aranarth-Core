package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class LeafDecayAppleDropIncrease implements Listener {

	public LeafDecayAppleDropIncrease(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds increased apple drop rates during the month of Solarvor.
	 * Also adds god apple fragments into the game.
	 * @param e The event.
	 */
	@EventHandler
	public void onLeavesDecay(final LeavesDecayEvent e) {
		if (AranarthUtils.getMonth() == 7) {
			Block block = e.getBlock();
			if (block.getType() == Material.OAK_LEAVES || block.getType() == Material.DARK_OAK_LEAVES) {
				// 5% chance of dropping an apple instead of 0.5%
				if (new Random().nextInt(20) == 0) {
					Bukkit.getLogger().info("Apple");
					block.getLocation().getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
				}
				// 1% chance of dropping a god apple fragment
				else if (new Random().nextInt(100) == 0) {
					Bukkit.getLogger().info("GOD Apple");
					block.getLocation().getWorld().dropItemNaturally(block.getLocation(), GodAppleFragment.getGodAppleFragment());
				}
			}
		}
	}
}
