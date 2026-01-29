package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Deals additional damage from all melee sources during the month of Faunivor.
 */
public class FaunivorExtraAttackDamage {
	public void execute(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Player killer) {
			if (e.getEntity() instanceof Player player) {
				Material killerHeldItem = killer.getInventory().getItemInMainHand().getType();
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(killer.getUniqueId());
				if (killerHeldItem == Material.AIR) {
					e.setDamage(e.getDamage() + 1);
					return;
				} else {
					int damageIncrease = calculateDamageIncrease(killerHeldItem);
					e.setDamage(e.getDamage() + damageIncrease);
				}
			}
		}
	}

	private int calculateDamageIncrease(Material type) {
		if (type == Material.MACE) {
			return 6;
		} else if (type == Material.NETHERITE_SWORD || type == Material.DIAMOND_SWORD
				|| type == Material.NETHERITE_AXE || type == Material.DIAMOND_AXE
				|| type == Material.TRIDENT) {
			return 5;
		} else if (type == Material.NETHERITE_PICKAXE || type == Material.DIAMOND_PICKAXE
					|| type == Material.NETHERITE_SHOVEL || type == Material.DIAMOND_SHOVEL) {
			return 3;
		} else if (type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD
					|| type == Material.IRON_AXE || type == Material.GOLDEN_AXE) {
			return 2;
		}
		return 1;
	}
}
