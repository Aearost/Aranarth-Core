package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Material;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_LEVEL;
import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_TYPE;

/**
 * Increases damage for various weapons depending on the Aranarthium armor set that is worn.
 */
public class WeaponsExtraDamage {
	public void execute(EntityDamageEvent e) {
		Entity entity = e.getEntity();
		if (isPlayerCausedDamage(e.getCause())) {
			if (e.getDamageSource().getCausingEntity() != null) {
				if (e.getDamageSource().getCausingEntity() instanceof Player attacker) {
					Random random = new Random();
					ItemStack weapon = attacker.getInventory().getItemInMainHand();
					Material weaponType = attacker.getInventory().getItemInMainHand().getType();
					if (AranarthUtils.isWearingArmorType(attacker, "aquatic")) {
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
					} else if (AranarthUtils.isWearingArmorType(attacker, "ardent")) {
						// Sword damage
						if (e.getDamageSource().getDamageType() == DamageType.PLAYER_ATTACK) {
							if (weaponType.name().endsWith("_SWORD")) {
								e.setDamage(e.getDamage() + random.nextInt(6) + 4);
							}
						}
					} else if (AranarthUtils.isWearingArmorType(attacker, "dwarven")) {
						// Axe and mace damage increase
						if (e.getDamageSource().getDamageType() == DamageType.PLAYER_ATTACK) {
							if (weaponType.name().endsWith("_AXE") || weaponType == Material.MACE) {
								e.setDamage(e.getDamage() + random.nextInt(6) + 4);
							}
						}
					} else if (AranarthUtils.isWearingArmorType(attacker, "elven")) {
						// Arrow damage increase
						if (e.getDamageSource().getDamageType() == DamageType.ARROW) {
							e.setDamage(e.getDamage() + random.nextInt(6) + 4);
						} else if (e.getDamageSource().getDamageType() == DamageType.SPEAR) {
							e.setDamage(e.getDamage() + random.nextInt(6) + 4);
						}
					} else if (AranarthUtils.isWearingArmorType(attacker, "scorched")) {
						if (e.getDamageSource().getDamageType() == DamageType.PLAYER_ATTACK) {
							// Bypass fire ticks when in the same or allied/truced Dominion
							if (entity instanceof Player target) {
								Dominion targetDominion = DominionUtils.getPlayerDominion(target.getUniqueId());
								if (targetDominion != null) {
									Dominion attackerDominion = DominionUtils.getPlayerDominion(attacker.getUniqueId());
									if (attackerDominion != null) {
										if (targetDominion.getLeader().equals(attackerDominion.getLeader())
												|| DominionUtils.areAllied(targetDominion, attackerDominion)
												|| DominionUtils.areTruced(targetDominion, attackerDominion)) {
											return;
										}
									}
								}
							}

							// Applies fire ticks for any source of melee damage
							entity.setFireTicks(60);
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

					// If there's an incantation
					if (weapon.hasItemMeta()) {
						if (weapon.getItemMeta().getPersistentDataContainer().has(INCANTATION_TYPE, PersistentDataType.STRING)) {
							String incantationType = weapon.getItemMeta().getPersistentDataContainer().get(INCANTATION_TYPE, PersistentDataType.STRING);
							int level = weapon.getItemMeta().getPersistentDataContainer().get(INCANTATION_LEVEL, PersistentDataType.INTEGER);
							if (incantationType.equals("lifesteal")) {
								if (e.getDamageSource().getDamageType() == DamageType.PLAYER_ATTACK) {
									double healAmount = 0;
									if (level == 1) {
										healAmount = e.getDamage() * 0.15;
									} else if (level == 2) {
										healAmount = e.getDamage() * 0.3;
									} else if (level == 3) {
										healAmount = e.getDamage() * 0.5;
									}
									Player player = (Player) e.getDamageSource().getCausingEntity();
									player.setHealth(player.getHealth() + healAmount);
								}
							}
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
