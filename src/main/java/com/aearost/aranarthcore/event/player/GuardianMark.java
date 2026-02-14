package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Guardian;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.MusicInstrument;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.MusicInstrumentMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Marks the entity as one of the player's Guardians to be summoned by a Goat Horn.
 */
public class GuardianMark {
	public void execute(PlayerInteractEntityEvent e) {
		Player player = e.getPlayer();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		HashMap<EntityType, List<Guardian>> guardians = aranarthPlayer.getGuardians();
		if (e.getHand() == EquipmentSlot.HAND) {
			if (player.getInventory().getItemInMainHand().hasItemMeta()) {
				if (player.getInventory().getItemInMainHand().getItemMeta() instanceof MusicInstrumentMeta meta) {
					// Iron Golem Guardians
					if (e.getRightClicked() instanceof IronGolem ironGolem) {
						if (meta.getInstrument() == MusicInstrument.ADMIRE_GOAT_HORN) {
							e.setCancelled(true);
							List<Guardian> ironGolemGuardians = guardians.get(EntityType.IRON_GOLEM);
							if (ironGolemGuardians == null) {
								ironGolemGuardians = new ArrayList<>();
							}

							Guardian guardianToUnmark = null;
							for (Guardian guardian : ironGolemGuardians) {
								if (guardian.getUuid().equals(ironGolem.getUniqueId())) {
									guardianToUnmark = guardian;
								}
							}
							if (guardianToUnmark != null) {
								ironGolemGuardians.remove(guardianToUnmark);
								player.sendMessage(ChatUtils.chatMessage("&7You have unmarked this &eIron Golem"));
								return;
							}

							if (ironGolemGuardians.size() < 2) {
								ironGolemGuardians.add(new Guardian(ironGolem.getUniqueId(), EntityType.IRON_GOLEM, ironGolem.getLocation()));
								guardians.put(EntityType.IRON_GOLEM, ironGolemGuardians);
								aranarthPlayer.setGuardians(guardians);
								player.sendMessage(ChatUtils.chatMessage("&7You have marked this as one of your &eIron Golem Guardians"));
								player.playSound(player, Sound.BLOCK_BELL_RESONATE, 1F, 2F);
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou have already designated 2 &eIron Golem Guardians!"));
							}
						}
					}
					// Wolf Guardians
					else if (e.getRightClicked() instanceof Wolf wolf) {
						if (meta.getInstrument() == MusicInstrument.CALL_GOAT_HORN) {
							e.setCancelled(true);
							List<Guardian> wolfGuardians = guardians.get(EntityType.WOLF);
							if (wolfGuardians == null) {
								wolfGuardians = new ArrayList<>();
							}

							Guardian guardianToUnmark = null;
							for (Guardian guardian : wolfGuardians) {
								if (guardian.getUuid().equals(wolf.getUniqueId())) {
									guardianToUnmark = guardian;
								}
							}
							if (guardianToUnmark != null) {
								wolfGuardians.remove(guardianToUnmark);
								player.sendMessage(ChatUtils.chatMessage("&7You have unmarked this &eWolf"));
								return;
							}

							if (wolfGuardians.size() < 8) {
								wolfGuardians.add(new Guardian(wolf.getUniqueId(), EntityType.WOLF, wolf.getLocation()));
								guardians.put(EntityType.WOLF, wolfGuardians);
								aranarthPlayer.setGuardians(guardians);
								player.sendMessage(ChatUtils.chatMessage("&7You have marked this as one of your &eWolf Guardians"));
								player.playSound(player, Sound.BLOCK_BELL_RESONATE, 1F, 2F);
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou have already designated 8 &eWolf Guardians!"));
							}
						}
					}
					// Horse Guardian
					else if (e.getRightClicked() instanceof AbstractHorse horse) {
						if (meta.getInstrument() == MusicInstrument.YEARN_GOAT_HORN) {
							e.setCancelled(true);
							List<Guardian> horseGuardian = guardians.get(EntityType.HORSE);
							if (horseGuardian == null) {
								horseGuardian = new ArrayList<>();
							}

							Guardian guardianToUnmark = null;
							for (Guardian guardian : horseGuardian) {
								if (guardian.getUuid().equals(horse.getUniqueId())) {
									guardianToUnmark = guardian;
								}
							}
							if (guardianToUnmark != null) {
								horseGuardian.remove(guardianToUnmark);
								player.sendMessage(ChatUtils.chatMessage("&7You have unmarked this &eHorse"));
								return;
							}

							if (horseGuardian.isEmpty()) {
								horseGuardian.add(new Guardian(horse.getUniqueId(), EntityType.HORSE, horse.getLocation()));
								guardians.put(EntityType.HORSE, horseGuardian);
								aranarthPlayer.setGuardians(guardians);
								player.sendMessage(ChatUtils.chatMessage("&7You have marked this as your &eHorse Guardian"));
								player.playSound(player, Sound.BLOCK_BELL_RESONATE, 1F, 2F);
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou have already designated a &eHorse Guardian!"));
							}
						}
					}
				}
			}
		}
	}

}
