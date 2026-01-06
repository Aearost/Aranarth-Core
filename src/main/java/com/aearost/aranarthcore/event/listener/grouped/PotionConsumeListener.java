package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.Objects;

public class PotionConsumeListener implements Listener {

	public PotionConsumeListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the auto-refill functionality when consuming of regular potions.
	 * @param e The event.
	 */
	@EventHandler
	public void onPotionUse(final PlayerItemConsumeEvent e) {
		if (e.getPlayer().getLocation().getWorld().getName().startsWith("world") || e.getPlayer().getLocation().getWorld().getName().startsWith("smp")) {
            replacePotion(e.getPlayer(), e.getItem(), e.getHand() == EquipmentSlot.HAND);
		}
	}

	/**
	 * Handles the auto-refill functionality when throwing splash and lingering potions.
	 * @param e The event.
	 */
	@EventHandler
	public void onPotionUse(final ProjectileLaunchEvent e) {
		if (e.getEntity() instanceof ThrownPotion potion) {
			if (potion.getShooter() instanceof Player player) {
				int heldSlot = player.getInventory().getHeldItemSlot();

				// Thrown from off-hand while holding nothing
				ItemStack heldItem = player.getInventory().getStorageContents()[heldSlot];
				if (heldItem == null) {
					replacePotion(player, potion.getItem(), false);
				} else {
					Material heldType = heldItem.getType();
					// Thrown from off-hand while holding an item that is not a splash or lingering potion
					if (heldType != Material.SPLASH_POTION && heldType != Material.LINGERING_POTION) {
						replacePotion(player, potion.getItem(), false);
					}
					// Thrown from main-hand
					else {
						replacePotion(player, potion.getItem(), true);
					}
				}
			}
		}
	}

	/**
	 * Helper method to handle the automatic potion replacement when consumed.
	 * @param player The player who used the potion.
	 * @param consumedPotion The potion that was consumed.
	 * @param isUsedFromMainHand Whether the thrown potion was from the main hand.
	 */
	private void replacePotion(Player player, ItemStack consumedPotion, boolean isUsedFromMainHand) {
		if (player.getGameMode() != GameMode.SURVIVAL) {
			return;
		}
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

		HashMap<ItemStack, Integer> potions = aranarthPlayer.getPotions();
		if (Objects.nonNull(potions)) {
			for (ItemStack potion : potions.keySet()) {
				PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
				if (consumedPotion.getType() == potion.getType()) {
					
					PotionMeta consumedPotionMeta = (PotionMeta) consumedPotion.getItemMeta();
					// Ensures that the potion is the same
					if (consumedPotionMeta.getBasePotionType() == potionMeta.getBasePotionType()) {
						if (consumedPotionMeta.getBasePotionType() == PotionType.MUNDANE) {
							if (!isSamePotion(consumedPotionMeta, potionMeta)) {
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
							ItemStack potionCopy = potion.clone();

							potions.put(potion, potions.get(potion) - 1);
							if (potions.get(potion) == 0) {
								potions.remove(potion);
							}
							aranarthPlayer.setPotions(potions);
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

							// Replaces the potion with the copy
							potionCopy.setAmount(1);
							Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), new Runnable() {
								@Override
								public void run() {
									player.getInventory().setItem(slot, potionCopy);
								}
							}, 1);
						} else {
							potions.put(potion, potions.get(potion) - 1);
							if (potions.get(potion) == 0) {
								potions.remove(potion);
							}
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

	/**
	 * Confirms that the two potions are different.
	 * @param metaA The first potion meta.
	 * @param metaB The second potion meta.
	 * @return Confirmation that the two potions are different.
	 */
	private boolean isSamePotion(PotionMeta metaA, PotionMeta metaB) {
		// If one is a Mundane potion
		if (metaA.hasCustomEffects() != metaB.hasCustomEffects()) {
			return false;
		}

		for (PotionEffect potionEffectA : metaA.getCustomEffects()) {
			if (metaB.getAllEffects().contains(potionEffectA)) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

}
