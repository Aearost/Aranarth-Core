package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiShulker;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.Objects;

/**
 * Handles opening the shulker box when right-clicking while holding it.
 */
public class ShulkerClick {
	public void execute(PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Player player = e.getPlayer();
			if (e.getPlayer().isSneaking()) {
				if (Objects.nonNull(e.getItem())) {
					ItemStack is = e.getItem();
					if (is.getItemMeta() instanceof BlockStateMeta im) {
						if (im.getBlockState() instanceof ShulkerBox shulker) {
							e.setCancelled(true);
							Inventory shulkerInventory = shulker.getInventory();
							GuiShulker gui = new GuiShulker(player, shulkerInventory);
							gui.openGui();
						}
					}
				}
			}
		}
	}
}
