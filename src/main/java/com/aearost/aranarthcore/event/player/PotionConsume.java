package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
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

import java.util.List;
import java.util.Objects;

public class PotionConsume implements Listener {

	public PotionConsume(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the auto-refill functionality when consuming of regular potions.
	 * @param e The event.
	 */
	@EventHandler
	public void onPotionUse(final PlayerItemConsumeEvent e) {
		if (e.getPlayer().getLocation().getWorld().getName().startsWith("world")) {
            replacePotion(e.getPlayer(), e.getItem(), e.getHand() == EquipmentSlot.HAND);
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
                    replacePotion(e.getPlayer(), e.getItem(), e.getHand() == EquipmentSlot.HAND);
				}
			}
		}
	}
	
	private void replacePotion(Player player, ItemStack consumedPotion, boolean isUsedFromMainHand) {
		if (player.getGameMode() != GameMode.SURVIVAL) {
			return;
		}
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		
		List<ItemStack> potions = aranarthPlayer.getPotions();
		if (Objects.nonNull(potions)) {
			for (ItemStack potion : potions) {
				PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
				if (consumedPotion.getType() == potion.getType()) {
					
					PotionMeta consumedPotionMeta = (PotionMeta) consumedPotion.getItemMeta();
					// Ensures that the potion is the same
					if (consumedPotionMeta.getBasePotionType() == potionMeta.getBasePotionType()) {
						// If it's an mcMMO potion, ensure that it is the exact same potion
						if (consumedPotionMeta.hasItemName()) {
							if (!consumedPotionMeta.getItemName().equals(potionMeta.getItemName())) {
								continue;
							}
						}
						
						// This might not include potions thrown from off-hand
						int slot;
						if (isUsedFromMainHand) {
							slot = player.getInventory().getHeldItemSlot();
						} else {
							// This is the slot number for the off-hand
							slot = 40;
						}
						
						if (potion.getType() == Material.SPLASH_POTION || potion.getType() == Material.LINGERING_POTION) {
							potion.setAmount(2);
							player.getInventory().setItem(slot, potion);
							potions.remove(potion);
							aranarthPlayer.setPotions(potions);
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						} else {
							potions.remove(potion);
							aranarthPlayer.setPotions(potions);
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
							final int finalSlot = slot;
							
							// Required to add potion to inventory after consumption
							Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(AranarthCore.getInstance(), new Runnable() {
				                public void run() {
				                    player.getInventory().setItem(finalSlot, consumedPotion);
				                }
				            }, 1L);
							
							player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 1));
						}
						
						return;
					}
				}
			}
		}
	}

}
