package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class ConcretePowderGravityPrevent implements Listener {

	public ConcretePowderGravityPrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents concrete powder from being affected by gravity.
	 * @param e The event.
	 */
	@EventHandler
	public void onConcretePowderFall(final EntityChangeBlockEvent e) {
		Entity entity = e.getEntity();
		
		if (entity instanceof FallingBlock) {
			Block block = e.getBlock();
			if (e.getTo() == Material.AIR && isConcretePowder(block.getType())) {
				e.setCancelled(true);
			}
		}
	}
	
	private boolean isConcretePowder(Material material) {
		return (material == Material.BLACK_CONCRETE_POWDER || material == Material.BLUE_CONCRETE_POWDER ||
				material == Material.BROWN_CONCRETE_POWDER || material == Material.CYAN_CONCRETE_POWDER ||
				material == Material.GRAY_CONCRETE_POWDER || material == Material.GREEN_CONCRETE_POWDER ||
				material == Material.LIGHT_BLUE_CONCRETE_POWDER || material == Material.LIGHT_GRAY_CONCRETE_POWDER ||
				material == Material.LIME_CONCRETE_POWDER || material == Material.MAGENTA_CONCRETE_POWDER ||
				material == Material.ORANGE_CONCRETE_POWDER || material == Material.PINK_CONCRETE_POWDER ||
				material == Material.PURPLE_CONCRETE_POWDER || material == Material.RED_CONCRETE_POWDER ||
				material == Material.WHITE_CONCRETE_POWDER || material == Material.YELLOW_CONCRETE_POWDER);
	}
}
