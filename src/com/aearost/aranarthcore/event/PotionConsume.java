package com.aearost.aranarthcore.event;

import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
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
		System.out.println("A");
	}

	/**
	 * Handles the auto-refill functionality when throwing splash potions.
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
					Player player = e.getPlayer();
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					ItemStack thrownPotion = e.getItem();
					PotionMeta thrownPotionMeta = (PotionMeta) thrownPotion.getItemMeta();
					List<ItemStack> potions = aranarthPlayer.getPotions();
					for (ItemStack potion : potions) {
						PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
						if (thrownPotion.getType() == potion.getType()) {
							if (thrownPotionMeta.getBasePotionType() == potionMeta.getBasePotionType()) {
								
								
								
								
								
								return; // Must prevent other quantities from being consumed
							}
						}
					}
				}
			}
		}
	}

}
