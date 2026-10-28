package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

/**
 * Causes a player to immediately die when logging off while combat logged.
 */
public class CombatLog {
	public void execute(EntityDamageByEntityEvent e) {
		if (e.isCancelled()) {
			return;
		}
		if (e.getDamager() instanceof Player attacker) {
			if (e.getEntity() instanceof Player player) {
				Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
				Dominion attackerDominion = DominionUtils.getPlayerDominion(attacker.getUniqueId());
				if (playerDominion != null) {
					if (attackerDominion != null && attackerDominion.isSameDominion(playerDominion)) {
						if (!playerDominion.isMemberPvpEnabled()) {
							return;
						}
					} else if (!DominionUtils.hasPermission(attacker, playerDominion, DominionPermission.PVP)) {
						return;
					}
				}
				if (!player.getWorld().getName().startsWith("resource") && !player.getWorld().getName().startsWith("creative")
					&& !player.getWorld().getName().equals("spawn")) {
					new BukkitRunnable() {
						@Override
						public void run() {
							if (player.getHealth() > 0) {
								AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
								AranarthPlayer attackerAranarthPlayer = AranarthUtils.getPlayer(attacker.getUniqueId());

								boolean isNotTaggedByAttacker = aranarthPlayer.getCombatLogTime().isEmpty()
										|| !aranarthPlayer.getCombatLogTime().containsKey(attacker.getUniqueId());
								if (isNotTaggedByAttacker) {
									player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "combat.tagged_by", "player", attackerAranarthPlayer.getNickname())));
									int playerWarMultiplier = DominionUtils.getDeathPenaltyMultiplier(player.getUniqueId(), attacker.getUniqueId());
									if (playerWarMultiplier > 1) {
										player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "combat.war_penalty", "multiplier", playerWarMultiplier)));
									}
								}
								boolean isAlreadyTaggingPlayer = attackerAranarthPlayer.getCombatLogTime().isEmpty()
										|| !attackerAranarthPlayer.getCombatLogTime().containsKey(player.getUniqueId());
								if (isAlreadyTaggingPlayer) {
									attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "combat.tagged_other", "player", aranarthPlayer.getNickname())));
									int attackerWarMultiplier = DominionUtils.getDeathPenaltyMultiplier(attacker.getUniqueId(), player.getUniqueId());
									if (attackerWarMultiplier > 1) {
										attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "combat.war_penalty", "multiplier", attackerWarMultiplier)));
									}
								}

								// Capture the exact timestamp this hit was registered
								long hitTime = System.currentTimeMillis();

								HashMap<UUID, Long> combatLog = new HashMap<>();
								combatLog.put(attacker.getUniqueId(), hitTime);
								aranarthPlayer.setCombatLogTime(combatLog);
								if (isNotTaggedByAttacker && player.getGameMode() == GameMode.SURVIVAL && aranarthPlayer.isInAdminMode()) {
									aranarthPlayer.setInAdminMode(false);
									player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "admin.mode_exit_combat")));
								}
								if (isNotTaggedByAttacker && player.getGameMode() == GameMode.SURVIVAL && aranarthPlayer.isInArchitectMode()) {
									aranarthPlayer.setInArchitectMode(false);
									player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "architect.mode_exit_combat")));
								}
								AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
								HashMap<UUID, Long> attackerCombatLog = new HashMap<>();
								attackerCombatLog.put(player.getUniqueId(), hitTime);
								attackerAranarthPlayer.setCombatLogTime(attackerCombatLog);
								if (isAlreadyTaggingPlayer && attacker.getGameMode() == GameMode.SURVIVAL && attackerAranarthPlayer.isInAdminMode()) {
									attackerAranarthPlayer.setInAdminMode(false);
									attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "admin.mode_exit_combat")));
								}
								if (isAlreadyTaggingPlayer && attacker.getGameMode() == GameMode.SURVIVAL && attackerAranarthPlayer.isInArchitectMode()) {
									attackerAranarthPlayer.setInArchitectMode(false);
									attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "architect.mode_exit_combat")));
								}
								AranarthUtils.setPlayer(attacker.getUniqueId(), attackerAranarthPlayer);

								// Combat tagged players are reset to the normal size
								AranarthUtils.applyAranarthiumScale(player);
								AranarthUtils.applyAranarthiumScale(attacker);

								new BukkitRunnable() {
									@Override
									public void run() {
										AranarthPlayer updatedAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
										HashMap<UUID, Long> updatedCombatLog = updatedAranarthPlayer.getCombatLogTime();

										if (updatedCombatLog.containsKey(attacker.getUniqueId())) {
											// Only clear if no newer hit has updated the timestamp
											if (updatedCombatLog.get(attacker.getUniqueId()) == hitTime) {
												player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "combat.ended")));
												updatedCombatLog.remove(attacker.getUniqueId());
												updatedAranarthPlayer.setCombatLogTime(updatedCombatLog);
												AranarthUtils.setPlayer(player.getUniqueId(), updatedAranarthPlayer);
												// Restores the Aranarthium size once the combat tag ends
												if (player.isOnline()) {
													AranarthUtils.applyAranarthiumScale(player);
												}
											}
										}

										AranarthPlayer updatedAttackerAranarthPlayer = AranarthUtils.getPlayer(attacker.getUniqueId());
										HashMap<UUID, Long> updatedAttackerCombatLog = updatedAttackerAranarthPlayer.getCombatLogTime();

										if (updatedAttackerCombatLog.containsKey(player.getUniqueId())) {
											// Only clear if no newer hit has updated the timestamp
											if (updatedAttackerCombatLog.get(player.getUniqueId()) == hitTime) {
												attacker.sendMessage(ChatUtils.chatMessage(Lang.getFor(attacker, "combat.no_longer_tagged")));
												updatedAttackerCombatLog.remove(player.getUniqueId());
												updatedAttackerAranarthPlayer.setCombatLogTime(updatedAttackerCombatLog);
												AranarthUtils.setPlayer(attacker.getUniqueId(), updatedAttackerAranarthPlayer);
												if (attacker.isOnline()) {
													AranarthUtils.applyAranarthiumScale(attacker);
												}
											}
										}
									}
								}.runTaskLater(AranarthCore.getInstance(), 200);
							}
						}
					}.runTaskLater(AranarthCore.getInstance(), 2);
				}
			}
		}
	}
}
