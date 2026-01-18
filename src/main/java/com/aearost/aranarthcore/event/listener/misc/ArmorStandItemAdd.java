package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;


public class ArmorStandItemAdd implements Listener {

	public ArmorStandItemAdd(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Allows players to enable the hands of an Armor Stand.
	 * @param e The event.
	 */
	@EventHandler
	public void onArmorStandClick(final PlayerInteractAtEntityEvent e) {
		if (e.getRightClicked().getType() == EntityType.ARMOR_STAND) {
			if (e.getRightClicked() instanceof LivingEntity livingEntity) {
				Player player = e.getPlayer();
				if (!player.isSneaking()) {
					if (livingEntity instanceof ArmorStand armorStand) {
						// Disabling the armor stand's arms
						if (armorStand.hasArms()) {
							boolean mainHandReady = armorStand.getEquipment().getItemInMainHand() == null
									|| armorStand.getEquipment().getItemInMainHand().getType() == Material.AIR;
							boolean offHandReady = armorStand.getEquipment().getItemInOffHand() == null
									|| armorStand.getEquipment().getItemInOffHand().getType() == Material.AIR;

							// Removing the arms only if both hands are empty
							if (mainHandReady && offHandReady) {
								if (player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType() == Material.AIR) {
									player.playSound(player, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.2F, 1.8F);
									armorStand.setArms(false);
								}

							}
						}
						// Enabling the armor stand's hands
						else {
							player.playSound(player, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.2F, 1.8F);
							armorStand.setArms(true);
						}
					}
				}
			}
		}
	}
}
