package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
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
		if (e.getDamager() instanceof Player attacker) {
			if (e.getEntity() instanceof Player player) {
				Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
				Dominion attackerDominion = DominionUtils.getPlayerDominion(attacker.getUniqueId());
				if (playerDominion != null) {
					if (attackerDominion != null && attackerDominion.getId().equals(playerDominion.getId())) {
						if (!playerDominion.isMemberPvpEnabled()) {
							return;
						}
					} else if (!DominionUtils.hasPermission(attacker, playerDominion, DominionPermission.PVP)) {
						return;
					}
				}
				if (!player.getWorld().getName().startsWith("resource") && !player.getWorld().getName().equals("creative")
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
									player.sendMessage(ChatUtils.chatMessage("&4You have been combat tagged by &e" + attackerAranarthPlayer.getNickname()));
								}
								boolean isAlreadyTaggingPlayer = attackerAranarthPlayer.getCombatLogTime().isEmpty()
										|| !attackerAranarthPlayer.getCombatLogTime().containsKey(player.getUniqueId());
								if (isAlreadyTaggingPlayer) {
									attacker.sendMessage(ChatUtils.chatMessage("&4You have combat tagged &e" + aranarthPlayer.getNickname()));
								}

								// Capture the exact timestamp this hit was registered
								long hitTime = System.currentTimeMillis();

								HashMap<UUID, Long> combatLog = new HashMap<>();
								combatLog.put(attacker.getUniqueId(), hitTime);
								aranarthPlayer.setCombatLogTime(combatLog);
								AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
								HashMap<UUID, Long> attackerCombatLog = new HashMap<>();
								attackerCombatLog.put(player.getUniqueId(), hitTime);
								attackerAranarthPlayer.setCombatLogTime(attackerCombatLog);
								AranarthUtils.setPlayer(attacker.getUniqueId(), attackerAranarthPlayer);

								new BukkitRunnable() {
									@Override
									public void run() {
										AranarthPlayer updatedAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
										HashMap<UUID, Long> updatedCombatLog = updatedAranarthPlayer.getCombatLogTime();

										if (updatedCombatLog.containsKey(attacker.getUniqueId())) {
											// Only clear if no newer hit has updated the timestamp
											if (updatedCombatLog.get(attacker.getUniqueId()) == hitTime) {
												player.sendMessage(ChatUtils.chatMessage("&7You are no longer engaged in combat"));
												updatedCombatLog.remove(attacker.getUniqueId());
												updatedAranarthPlayer.setCombatLogTime(updatedCombatLog);
												AranarthUtils.setPlayer(player.getUniqueId(), updatedAranarthPlayer);
											}
										}

										AranarthPlayer updatedAttackerAranarthPlayer = AranarthUtils.getPlayer(attacker.getUniqueId());
										HashMap<UUID, Long> updatedAttackerCombatLog = updatedAttackerAranarthPlayer.getCombatLogTime();

										if (updatedAttackerCombatLog.containsKey(player.getUniqueId())) {
											// Only clear if no newer hit has updated the timestamp
											if (updatedAttackerCombatLog.get(player.getUniqueId()) == hitTime) {
												attacker.sendMessage(ChatUtils.chatMessage("&7You are no longer engaged in combat"));
												updatedAttackerCombatLog.remove(player.getUniqueId());
												updatedAttackerAranarthPlayer.setCombatLogTime(updatedAttackerCombatLog);
												AranarthUtils.setPlayer(attacker.getUniqueId(), updatedAttackerAranarthPlayer);
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
