package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Increases damage for various weapons depending on the Aranarthium armor set that is worn.
 */
public class ExtraWeaponsDamage {
	public void execute(EntityDamageEvent e) {
		Entity entity = e.getEntity();
		if (isPlayerCausedDamage(e.getCause())) {
			if (e.getDamageSource().getCausingEntity() != null) {
				if (e.getDamageSource().getCausingEntity() instanceof Player attacker) {
					Random random = new Random();
					ItemStack weapon = attacker.getInventory().getItemInMainHand();
					Material weaponType = attacker.getInventory().getItemInMainHand().getType();
					if (AranarthUtils.isArmorType(attacker, "aquatic")) {
						// Ranged trident throw
						if (e.getDamageSource().getDamageType() == DamageType.TRIDENT) {
							// 2 to 6 hearts of additional damage
							e.setDamage(e.getDamage() + random.nextInt(6) + 4);
						}
						// Melee trident attack
						else if (e.getDamageSource().getDamageType() == DamageType.PLAYER_ATTACK) {
							if (weaponType == Material.TRIDENT) {
								e.setDamage(e.getDamage() + random.nextInt(6) + 4);
							}
						}
					} else if (AranarthUtils.isArmorType(attacker, "ardent")) {
						// Sword damage
						if (e.getDamageSource().getDamageType() == DamageType.PLAYER_ATTACK) {
							if (weaponType.name().endsWith("_SWORD")) {
								e.setDamage(e.getDamage() + random.nextInt(6) + 4);
							}
						}
					} else if (AranarthUtils.isArmorType(attacker, "dwarven")) {
						// Axe and mace damage increase
						if (e.getDamageSource().getDamageType() == DamageType.PLAYER_ATTACK) {
							if (weaponType.name().endsWith("_AXE") || weaponType == Material.MACE) {
								e.setDamage(e.getDamage() + random.nextInt(6) + 4);
							}
						}
					} else if (AranarthUtils.isArmorType(attacker, "elven")) {
						// Arrow damage increase
						if (e.getDamageSource().getDamageType() == DamageType.ARROW) {
							e.setDamage(e.getDamage() + random.nextInt(6) + 4);
						}
					} else if (AranarthUtils.isArmorType(attacker, "scorched")) {
						if (weapon.containsEnchantment(Enchantment.FIRE_ASPECT)) {
							if (weapon.getEnchantmentLevel(Enchantment.FIRE_ASPECT) == 1) {
								entity.setFireTicks(140);
							} else {
								entity.setFireTicks(240);
							}
						} else if (weapon.containsEnchantment(Enchantment.FLAME)) {
							entity.setFireTicks(160);
						}
					}
				}
			}
		}
	}
	
	private boolean isPlayerCausedDamage(DamageCause cause) {
		return (cause == DamageCause.ENTITY_ATTACK) || (cause == DamageCause.ENTITY_SWEEP_ATTACK)
				|| (cause == DamageCause.PROJECTILE);
	}
}
