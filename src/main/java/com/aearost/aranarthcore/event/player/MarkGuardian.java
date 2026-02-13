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
public class MarkGuardian {
	public void execute(PlayerInteractEntityEvent e) {
		Player player = e.getPlayer();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		HashMap<EntityType, List<Guardian>> guardians = aranarthPlayer.getGuardians();
		if (e.getHand() == EquipmentSlot.HAND) {
			if (player.getInventory().getItemInMainHand().hasItemMeta()) {
				if (player.getInventory().getItemInMainHand().getItemMeta() instanceof MusicInstrumentMeta meta) {
					// Iron Golem Guardians
					if (e.getRightClicked() instanceof IronGolem) {
						if (meta.getInstrument() == MusicInstrument.CALL_GOAT_HORN) {

						}
					}
					// Wolf Guardians
					else if (e.getRightClicked() instanceof Wolf) {
						if (meta.getInstrument() == MusicInstrument.CALL_GOAT_HORN) {

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
