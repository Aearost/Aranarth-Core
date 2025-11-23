package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Prevents crops from being trampled by both players and other mobs
 */
public class DominionProtection implements Listener {

	public DominionProtection(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from placing blocks in another Dominion.
	 */
	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
		if (!aranarthPlayer.getIsInAdminMode()) {
			boolean isActionPrevented = applyLogic(e.getPlayer(), e.getBlock(), null);
			if (isActionPrevented) {
				e.setCancelled(true);
			}
		}

	}

	/**
	 * Prevents players from breaking blocks in another Dominion.
	 */
	@EventHandler
	public void onBreak(BlockBreakEvent e) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
		if (!aranarthPlayer.getIsInAdminMode()) {
			boolean isActionPrevented = applyLogic(e.getPlayer(), e.getBlock(), null);
			if (isActionPrevented) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents players from interacting with non-alive entities in another Dominion.
	 */
	@EventHandler
	public void onInteractEntity(PlayerInteractEntityEvent e) {
		if (e.getRightClicked() != null) {
			EntityType type = e.getRightClicked().getType();
			// Armor stands are considered alive
			if (!type.isAlive() || type == EntityType.ARMOR_STAND) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
				if (!aranarthPlayer.getIsInAdminMode()) {
					boolean isActionPrevented = applyLogic(e.getPlayer(), null, e.getRightClicked());
					if (isActionPrevented) {
						e.setCancelled(true);
					}
				}

			}
		}
	}

	/**
	 * Prevents players from placing non-alive entities in another Dominion.
	 */
	@EventHandler
	public void onInteractEntity(EntityPlaceEvent e) {
		if (e.getEntity() != null) {
			EntityType type = e.getEntity().getType();
			// Armor stands are considered alive
			if (!type.isAlive() || type == EntityType.ARMOR_STAND) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
				if (!aranarthPlayer.getIsInAdminMode()) {
					boolean isActionPrevented = applyLogic(e.getPlayer(), null, e.getEntity());
					if (isActionPrevented) {
						e.setCancelled(true);
					}
				}
			}
		}
	}

	/**
	 * Prevents players from attacking non-alive entities in another Dominion.
	 */
	@EventHandler
	public void onAttackEntity(EntityDamageEvent e) {
		if (e.getEntity() != null) {
			EntityType type = e.getEntity().getType();
			// Armor stands are considered alive
			if (!type.isAlive() || type == EntityType.ARMOR_STAND) {
				if (e.getDamageSource().getCausingEntity() instanceof Player player) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					if (!aranarthPlayer.getIsInAdminMode()) {
						boolean isActionPrevented = applyLogic(player, null, e.getEntity());
						if (isActionPrevented) {
							e.setCancelled(true);
						}
					}

				}
			}
		}
	}

	/**
	 * All validation logic for interacting with another Dominion.
	 */
	private boolean applyLogic(Player player, Block block, Entity entity) {
		Dominion dominion = null;
		// If the player is attempting to place or break a block
		if (block != null) {
			dominion = DominionUtils.getDominionOfChunk(block.getChunk());
		} else if (entity != null) {
			dominion = DominionUtils.getDominionOfChunk(entity.getLocation().getChunk());
		}

		// If the block is in a dominion
		if (dominion != null) {
			Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
			// If the player is not in the dominion of the block
			if (playerDominion == null || !dominion.getOwner().equals(playerDominion.getOwner())) {
				player.sendMessage(ChatUtils.chatMessage("&cYou are not in the Dominion of &e" + dominion.getName()));
				return true;
			}
		}
		return false;
	}
}
