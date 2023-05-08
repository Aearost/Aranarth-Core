package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;

public class LogStrip implements Listener {

	public LogStrip(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents stripping a log if the player is not sneaking
	 * 
	 * @param e
	 */
	@EventHandler
	public void onLogStrip(final PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (isHoldingAxe(e.getPlayer())) {
				if (getMaterialIfWood(e.getClickedBlock()) != null) {
					if (!e.getPlayer().isSneaking()) {
						e.setCancelled(true);
						e.getPlayer().sendMessage(ChatUtils.chatMessageError("You must be sneaking to strip logs!"));
					}
				}
			}
		}
	}

	private Material getMaterialIfWood(Block block) {
		if (block.getType() == Material.OAK_LOG || block.getType() == Material.BIRCH_LOG
				|| block.getType() == Material.SPRUCE_LOG || block.getType() == Material.JUNGLE_LOG
				|| block.getType() == Material.DARK_OAK_LOG || block.getType() == Material.ACACIA_LOG
				|| block.getType() == Material.CRIMSON_STEM || block.getType() == Material.WARPED_STEM) {
			return block.getType();
		} else {
			return null;
		}
	}

	private boolean isHoldingAxe(Player player) {
		Material item = player.getInventory().getItemInMainHand().getType();
		if (item == Material.WOODEN_AXE || item == Material.STONE_AXE || item == Material.IRON_AXE
				|| item == Material.GOLDEN_AXE || item == Material.DIAMOND_AXE || item == Material.NETHERITE_AXE) {
			return true;
		} else {
			return false;
		}
	}
}
