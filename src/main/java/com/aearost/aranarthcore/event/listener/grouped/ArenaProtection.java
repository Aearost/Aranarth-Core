package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

/**
 * Handles preventing behaviour in the arena world.
 */
public class ArenaProtection implements Listener {

	public ArenaProtection(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents vines from being grown in the arena world.
	 */
	@EventHandler
	public void onVineGrow(BlockSpreadEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("arena")) {
			Material material = e.getSource().getType();
			if (material == Material.VINE || material == Material.CAVE_VINES_PLANT) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents blocks from being destroyed in the arena world spawn
	 */
	@EventHandler
	public void onBlockBreak(final BlockBreakEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("arena")) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
			if (aranarthPlayer.getCouncilRank() != 3) {
				int x = e.getBlock().getX();
				int y = e.getBlock().getY();
				int z = e.getBlock().getZ();

				if ((x >= -4 && x <= 4) && (y >= 100 && y <= 111) && (z >= -4 && z <=4)) {
					e.setCancelled(true);
					e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot break this!"));
				}
			}
		}
	}

	/**
	 * Prevents armour from being damaged in the arena world.
	 */
	@EventHandler
	public void onArmorTakeDamage(final PlayerItemDamageEvent e) {
		if (e.getPlayer().getWorld().getName().equalsIgnoreCase("arena")) {
			e.setCancelled(true);
		}

	}

	/**
	 * Prevents items from being dropped in the arena world aside from iron ingots and arrows.
	 */
	@EventHandler
	public void onItemDrop(ItemSpawnEvent e) {
		if (e.getLocation().getWorld().getName().equalsIgnoreCase("arena")) {
			if (e.getEntity().getItemStack().getType() != Material.IRON_INGOT
					&& e.getEntity().getItemStack().getType() != Material.ARROW) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents ice and snow from melting in the arena world.
	 */
	@EventHandler
	public void onMelt(BlockFadeEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("arena")) {
			Material material = e.getBlock().getType();
			if (material == Material.ICE || material == Material.PACKED_ICE || material == Material.BLUE_ICE ||
					material == Material.SNOW || material == Material.SNOW_BLOCK) {
				e.setCancelled(true);
			}
		}

	}

	/**
	 * Prevents hunger from being lost in the arena world.
	 */
	@EventHandler
	public void onArenaHungerDeplete(FoodLevelChangeEvent e) {
		if (e.getEntity().getWorld().getName().equalsIgnoreCase("arena")) {
			e.setCancelled(true);
		}
	}

}
