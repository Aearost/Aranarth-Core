package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Boost;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent;
import com.gmail.nossr50.mcMMO;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import com.projectkorra.projectkorra.event.AbilityEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Handles all logic involving extra effects with server boosts that are not required to be addressed within another event.
 */
public class BoostEffectsListener implements Listener {

	public BoostEffectsListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the additional mcMMO EXP gain while server boosts are applied.
	 */
	@EventHandler
	public void onExpGain(McMMOPlayerXpGainEvent e) {
		String name = e.getPlayer().getLocation().getWorld().getName();
		if (name.startsWith("world") || name.startsWith("smp") || name.startsWith("resource")) {
			if (AranarthUtils.getServerBoosts().containsKey(Boost.MINER)) {
				if (e.getSkill() == PrimarySkillType.MINING) {
					e.setRawXpGained((float) (e.getRawXpGained() * 1.5));
				}
			}
			if (AranarthUtils.getServerBoosts().containsKey(Boost.HARVEST)) {
				if (e.getSkill() == PrimarySkillType.HERBALISM || e.getSkill() == PrimarySkillType.EXCAVATION
						|| e.getSkill() == PrimarySkillType.WOODCUTTING) {
					e.setRawXpGained((float) (e.getRawXpGained() * 1.5));
				}
			}
			if (AranarthUtils.getServerBoosts().containsKey(Boost.HUNTER)) {
				if (e.getSkill() == PrimarySkillType.SWORDS || e.getSkill() == PrimarySkillType.AXES
						|| e.getSkill() == PrimarySkillType.TRIDENTS || e.getSkill() == PrimarySkillType.MACES
						|| e.getSkill() == PrimarySkillType.ARCHERY || e.getSkill() == PrimarySkillType.CROSSBOWS
						|| e.getSkill() == PrimarySkillType.FISHING || e.getSkill() == PrimarySkillType.SPEARS) {
					e.setRawXpGained((float) (e.getRawXpGained() * 1.5));
				}
			}
			if (AranarthUtils.getServerBoosts().containsKey(Boost.CHI)) {
				if (e.getSkill() == PrimarySkillType.UNARMED || e.getSkill() == PrimarySkillType.ACROBATICS) {
					e.setRawXpGained((float) (e.getRawXpGained() * 1.5));
				}
			}
		}
	}

	/**
	 * Handles additional functionality relating to breaking blocks while server boosts are applied.
	 */
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		if (e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
			return;
		}

		String name = e.getBlock().getLocation().getWorld().getName();
		if (name.startsWith("world") || name.startsWith("smp") || name.startsWith("resource")) {
			boolean isEligible = mcMMO.getChunkManager().isEligible(e.getBlock());
			if (isEligible) {
				if (AranarthUtils.getServerBoosts().containsKey(Boost.HARVEST)) {
					Material type = e.getBlock().getType();
					Location loc = e.getBlock().getLocation();
					// Increase drops by 1.5x — use the block's actual loot table drops so blocks
					// like grass (which drop dirt, not the block itself) are handled correctly.
					if (AranarthUtils.isHarvestableWithShovel(type) || AranarthUtils.isHarvestableWithAxe(type)) {
						if (new Random().nextInt(2) == 0) {
							for (ItemStack drop : e.getBlock().getDrops(e.getPlayer().getInventory().getItemInMainHand())) {
								loc.getWorld().dropItemNaturally(loc, drop);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Handles extra mob drops.
	 */
	@EventHandler
	public void onEntityDeath(EntityDeathEvent e) {
		if (e.getDamageSource().getCausingEntity() != null) {
			if (e.getDamageSource().getCausingEntity() instanceof Player) {
				if (!(e.getEntity() instanceof Player)) {
					if (AranarthUtils.getServerBoosts().containsKey(Boost.HUNTER)) {
						EntityEquipment equipment = e.getEntity().getEquipment();
						List<ItemStack> equipmentList = new ArrayList<>();
						equipmentList.addAll(Arrays.asList(equipment.getArmorContents()));
						equipmentList.add(equipment.getItemInMainHand());
						equipmentList.add(equipment.getItemInOffHand());

						for (ItemStack drop : e.getDrops()) {
							// Avoid duplication of held items or worn armor
							if (equipmentList.contains(drop)) {
								continue;
							}

							// Avoids duplication of saddles and armor on mounts
							if (drop.getType() == Material.SADDLE || drop.getType().name().contains("_ARMOR") || drop.getType() == Material.ARMOR_STAND) {
								continue;
							}

							int rand = new Random().nextInt(2);
							// 50% chance to double the drop
							if (rand == 0) {
								drop.setAmount(drop.getAmount() * 2);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Handles increasing bending ability damage.
	 */
	@EventHandler
	public void onBendAttack(AbilityDamageEntityEvent e) {
		if (e.getAbility().getPlayer() != null) {
			String name = e.getAbility().getPlayer().getLocation().getWorld().getName();
			if (!AranarthUtils.isSurvivalWorld(name)) {
				return;
			}

			if (AranarthUtils.getServerBoosts().containsKey(Boost.CHI)) {
				e.setDamage(e.getDamage() * 1.5);
			}
		}
	}

	/**
	 * Handles decreasing bending ability cooldowns.
	 */
	@EventHandler
	public void onAbilityEnd(AbilityEndEvent e) {
		if (e.getAbility().getPlayer() != null) {
			String name = e.getAbility().getPlayer().getLocation().getWorld().getName();
			if (!AranarthUtils.isSurvivalWorld(name)) {
				return;
			}

			if (AranarthUtils.getServerBoosts().containsKey(Boost.CHI)) {
				Ability ability = e.getAbility();
				BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(e.getAbility().getPlayer());
				bendingPlayer.removeCooldown(ability.getName());
				bendingPlayer.addCooldown(ability, ability.getCooldown() / 2);
			}
		}
	}

}
