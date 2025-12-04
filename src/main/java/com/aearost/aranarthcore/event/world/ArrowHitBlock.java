package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

import static com.aearost.aranarthcore.objects.CustomItemKeys.ARROW;

/**
 * Overrides behaviour of custom arrows hitting blocks.
 */
public class ArrowHitBlock {
	public void execute(final ProjectileHitEvent e) {
		Block block = e.getHitBlock();
		if (e.getEntity() instanceof Arrow arrow) {
			if (arrow.getPersistentDataContainer().has(ARROW)) {
				arrow.setItem(AranarthUtils.getArrowFromType(arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING)));
				if (arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING).equals("obsidian")) {
					// 60% chance of breaking
					if (new Random().nextInt(10) >= 4) {
						e.getEntity().remove();
					}
				}
			}
		}
	}
}
