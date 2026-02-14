package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Sentinel;
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
 * Marks the entity as one of the player's Sentinels to be summoned by a Goat Horn.
 */
public class SentinelMark {
	public void execute(PlayerInteractEntityEvent e) {
		Player player = e.getPlayer();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		HashMap<EntityType, List<Sentinel>> sentinels = aranarthPlayer.getSentinels();
		if (e.getHand() == EquipmentSlot.HAND) {
			if (player.getInventory().getItemInMainHand().hasItemMeta()) {
				if (player.getInventory().getItemInMainHand().getItemMeta() instanceof MusicInstrumentMeta meta) {
					// Iron Golem sentinels
					if (e.getRightClicked() instanceof IronGolem ironGolem) {
						if (meta.getInstrument() == MusicInstrument.ADMIRE_GOAT_HORN) {
							e.setCancelled(true);
							List<Sentinel> ironGolemSentinels = sentinels.get(EntityType.IRON_GOLEM);
							if (ironGolemSentinels == null) {
								ironGolemSentinels = new ArrayList<>();
							}

							Sentinel sentinelToUnmark = null;
							for (Sentinel sentinel : ironGolemSentinels) {
								if (sentinel.getUuid().equals(ironGolem.getUniqueId())) {
									sentinelToUnmark = sentinel;
								}
							}
							if (sentinelToUnmark != null) {
								ironGolemSentinels.remove(sentinelToUnmark);
								player.sendMessage(ChatUtils.chatMessage("&7You have unmarked this &eIron Golem"));
								return;
							}

							if (ironGolemSentinels.size() < 2) {
								ironGolemSentinels.add(new Sentinel(ironGolem.getUniqueId(), EntityType.IRON_GOLEM, ironGolem.getLocation()));
								sentinels.put(EntityType.IRON_GOLEM, ironGolemSentinels);
								aranarthPlayer.setSentinels(sentinels);
								player.sendMessage(ChatUtils.chatMessage("&7You have marked this as one of your &eIron Golem Sentinels"));
								player.playSound(player, Sound.BLOCK_BELL_RESONATE, 1F, 2F);
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou have already designated 2 &eIron Golem Sentinels"));
							}
						}
					}
					// Wolf sentinels
					else if (e.getRightClicked() instanceof Wolf wolf) {
						if (meta.getInstrument() == MusicInstrument.CALL_GOAT_HORN) {
							e.setCancelled(true);

							if (!wolf.isTamed() || !wolf.getOwnerUniqueId().equals(player.getUniqueId())) {
								player.sendMessage(ChatUtils.chatMessage("&cYou do not own this &eWolf!"));
								return;
							}

							List<Sentinel> wolfSentinels = sentinels.get(EntityType.WOLF);
							if (wolfSentinels == null) {
								wolfSentinels = new ArrayList<>();
							}

							Sentinel sentinelToUnmark = null;
							for (Sentinel sentinel : wolfSentinels) {
								if (sentinel.getUuid().equals(wolf.getUniqueId())) {
									sentinelToUnmark = sentinel;
								}
							}
							if (sentinelToUnmark != null) {
								wolfSentinels.remove(sentinelToUnmark);
								player.sendMessage(ChatUtils.chatMessage("&7You have unmarked this &eWolf"));
								return;
							}

							if (wolfSentinels.size() < 8) {
								wolfSentinels.add(new Sentinel(wolf.getUniqueId(), EntityType.WOLF, wolf.getLocation()));
								sentinels.put(EntityType.WOLF, wolfSentinels);
								aranarthPlayer.setSentinels(sentinels);
								player.sendMessage(ChatUtils.chatMessage("&7You have marked this as one of your &eWolf Sentinels"));
								player.playSound(player, Sound.BLOCK_BELL_RESONATE, 1F, 2F);
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou have already designated 8 &eWolf Sentinels"));
							}
						}
					}
					// Horse sentinel
					else if (e.getRightClicked() instanceof AbstractHorse horse) {
						if (meta.getInstrument() == MusicInstrument.YEARN_GOAT_HORN) {
							e.setCancelled(true);

							if (!horse.isTamed() || !horse.getOwnerUniqueId().equals(player.getUniqueId())) {
								player.sendMessage(ChatUtils.chatMessage("&cYou do not own this &eHorse!"));
								return;
							}

							List<Sentinel> horseSentinel = sentinels.get(EntityType.HORSE);
							if (horseSentinel == null) {
								horseSentinel = new ArrayList<>();
							}

							Sentinel sentinelToUnmark = null;
							for (Sentinel sentinel : horseSentinel) {
								if (sentinel.getUuid().equals(horse.getUniqueId())) {
									sentinelToUnmark = sentinel;
								}
							}
							if (sentinelToUnmark != null) {
								horseSentinel.remove(sentinelToUnmark);
								player.sendMessage(ChatUtils.chatMessage("&7You have unmarked this &eHorse"));
								return;
							}

							if (horseSentinel.isEmpty()) {
								horseSentinel.add(new Sentinel(horse.getUniqueId(), EntityType.HORSE, horse.getLocation()));
								sentinels.put(EntityType.HORSE, horseSentinel);
								aranarthPlayer.setSentinels(sentinels);
								player.sendMessage(ChatUtils.chatMessage("&7You have marked this as your &eHorse Sentinel"));
								player.playSound(player, Sound.BLOCK_BELL_RESONATE, 1F, 2F);
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou have already designated a &eHorse Sentinel"));
							}
						}
					}
				}
			}
		}
	}

}
