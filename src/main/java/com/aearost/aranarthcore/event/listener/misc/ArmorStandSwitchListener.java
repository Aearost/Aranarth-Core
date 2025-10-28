package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Switches the player's armor if shift right-clicking an armor stand.
 */
public class ArmorStandSwitchListener implements Listener {

	public ArmorStandSwitchListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void execute(PlayerInteractAtEntityEvent e) {
		if (e.getRightClicked() instanceof ArmorStand armorStand) {
			Player player = e.getPlayer();
			Dominion dominion = DominionUtils.getDominionOfChunk(e.getRightClicked().getLocation().getChunk());
			// Do not proceed if it is in a claimed dominion that the player is not in
			if (dominion != null) {
				if (!dominion.getMembers().contains(player.getUniqueId())) {
					player.sendMessage(ChatUtils.chatMessage("&cYou are not in the Dominion of &e" + dominion.getName()));
					e.setCancelled(true);
					return;
				}
			}

			if (e.getPlayer().isSneaking()) {
				if (Objects.nonNull(armorStand.getEquipment())) {

					// Gets the player's current armor
					ItemStack playerHelmet = player.getInventory().getArmorContents()[3];
					ItemStack playerChestplate = player.getInventory().getArmorContents()[2];
					ItemStack playerLeggings = player.getInventory().getArmorContents()[1];
					ItemStack playerBoots = player.getInventory().getArmorContents()[0];

					// Updates the player's armor to match what is on the armor stand
					player.getInventory().setArmorContents(new ItemStack[] {
							armorStand.getEquipment().getBoots(),
							armorStand.getEquipment().getLeggings(),
							armorStand.getEquipment().getChestplate(),
							armorStand.getEquipment().getHelmet(),
					});
					// Updates the armor stand's armor to match what was on the player
					armorStand.getEquipment().setArmorContents(new ItemStack[] {
							playerBoots, playerLeggings, playerChestplate, playerHelmet
					});

					player.playSound(player, Sound.BLOCK_ANVIL_USE, 0.5F, 1.5F);
					e.setCancelled(true);
				}
			}
		}
	}
}
