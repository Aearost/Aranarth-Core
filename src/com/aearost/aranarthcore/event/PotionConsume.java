package com.aearost.aranarthcore.event;

import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;

public class PotionConsume implements Listener {

	public PotionConsume(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the auto-refill functionality when consuming of regular potions.
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onPotionUse(final PlayerItemConsumeEvent e) {
		if (e.getHand() == EquipmentSlot.HAND) {
			replacePotion(e.getPlayer(), e.getItem(), true);
		} else {
			replacePotion(e.getPlayer(), e.getItem(), false);
		}
		
	}

	/**
	 * Handles the auto-refill functionality when throwing splash and lingering potions.
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onPotionUse(final PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (Objects.nonNull(e.getItem())) {
				if (e.getItem().getType() == Material.SPLASH_POTION
						|| e.getItem().getType() == Material.LINGERING_POTION) {
					if (e.getHand() == EquipmentSlot.HAND) {
						replacePotion(e.getPlayer(), e.getItem(), true);
					} else {
						replacePotion(e.getPlayer(), e.getItem(), false);
					}
				}
			}
		}
	}
	
	private void replacePotion(Player player, ItemStack consumedPotion, boolean isUsedFromMainHand) {
		
		if (player.getGameMode() != GameMode.SURVIVAL) {
			return;
		}
		
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		PotionMeta consumedPotionMeta = (PotionMeta) consumedPotion.getItemMeta();
		
		List<ItemStack> potions = aranarthPlayer.getPotions();
		for (ItemStack potion : potions) {
			PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
			if (consumedPotion.getType() == potion.getType()) {
				if (consumedPotionMeta.getBasePotionType() == potionMeta.getBasePotionType()) {
					// This might not include potions thrown from off-hand
					int slot = 0;
					if (isUsedFromMainHand) {
						slot = player.getInventory().getHeldItemSlot();
					} else {
						// This is the slot number for the off-hand
						slot = 40;
					}
					
					potion.setAmount(2);
					player.getInventory().setItem(slot, potion);
					potions.remove(potion);
					aranarthPlayer.setPotions(potions);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					return;
				}
			}
		}
	}

}
