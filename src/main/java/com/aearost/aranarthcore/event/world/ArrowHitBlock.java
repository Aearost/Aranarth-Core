package com.aearost.aranarthcore.event.world;

import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARROW;

/**
 * Overrides behaviour of custom arrows hitting blocks.
 */
public class ArrowHitBlock {
	public void execute(final ProjectileHitEvent e) {
		Block block = e.getHitBlock();
		if (e.getEntity() instanceof Arrow arrow) {
			if (arrow.getPersistentDataContainer().has(ARROW)) {
				if (arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING).equals("obsidian")) {
					e.getEntity().remove();
				}
			}
		}
	}
}
