package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiShulker;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

/**
 * Handles opening the shulker box when right-clicking while holding it.
 */
public class ShulkerClick {
	public void execute(PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Player player = e.getPlayer();
			if (e.getPlayer().isSneaking()) {
				if (e.getHand() == EquipmentSlot.HAND) {
					ItemStack heldItem = player.getInventory().getItemInMainHand();
					if (heldItem != null && heldItem.getType() != Material.AIR) {
						if (heldItem.getItemMeta() instanceof BlockStateMeta im) {
							if (im.getBlockState() instanceof ShulkerBox shulker) {
								if (!player.hasPermission("aranarth.shulker")) {
									return;
								}

								e.setCancelled(true);
								player.playSound(player, Sound.BLOCK_SHULKER_BOX_OPEN, 1F, 1F);
								Inventory shulkerInventory = shulker.getInventory();
								GuiShulker gui = new GuiShulker(player, shulkerInventory);
								gui.openGui();
								return;
							}
						}
					}
				}
			}
		}
	}
}
