package com.aearost.aranarthcore.event;

import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;

public class ArrowConsume implements Listener {

	public ArrowConsume(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the auto-refill functionality when consuming of arrows
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onArrowUse(final EntityShootBowEvent e) {
		if (e.getEntity() instanceof Player) {
			if (e.getProjectile() instanceof Arrow || e.getProjectile() instanceof SpectralArrow) {
				Player player = (Player) e.getEntity();
				if (e.getHand() == EquipmentSlot.HAND) {
					replaceArrow(player, e.getConsumable(), true);
				} else {
					replaceArrow(player, e.getConsumable(), false);
				}
			}
		}
	}

	private void replaceArrow(Player player, ItemStack launchedArrow, boolean isUsedFromMainHand) {
		if (player.getGameMode() != GameMode.SURVIVAL) {
			return;
		}
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

		List<ItemStack> arrows = aranarthPlayer.getArrows();
		if (Objects.nonNull(arrows)) {
			for (ItemStack arrow : arrows) {
				if (Objects.nonNull(arrow)) {
					// If it is a normal or spectral arrow
					if (launchedArrow.getType() == arrow.getType()) {
						
						if (launchedArrow.getType() == Material.ARROW) {
							player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
						} else if (launchedArrow.getType() == Material.SPECTRAL_ARROW) {
							player.getInventory().addItem(new ItemStack(Material.SPECTRAL_ARROW, 1));
						} else {
							PotionMeta launchedArrowMeta = (PotionMeta) launchedArrow.getItemMeta();
							PotionMeta arrowMeta = (PotionMeta) arrow.getItemMeta();
							if (launchedArrowMeta.getBasePotionType() != arrowMeta.getBasePotionType()) {
								return;
							} else {
								ItemStack arrowToAdd = new ItemStack(Material.TIPPED_ARROW, 1);
								arrowToAdd.setItemMeta(launchedArrowMeta);
								player.getInventory().addItem(arrowToAdd);
							}
						}
						
						int newAmountInQuiver = arrow.getAmount() - 1;
						if (newAmountInQuiver > 0) {
							arrow.setAmount(newAmountInQuiver);
						} else {
							arrows.remove(arrow);
						}
						
						aranarthPlayer.setArrows(arrows);
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						return;
					}
				}
			}
		}
	}

}
