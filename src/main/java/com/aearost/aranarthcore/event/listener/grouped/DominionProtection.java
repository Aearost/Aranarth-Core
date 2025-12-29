package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

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
	public void onPlaceEntity(PlayerInteractEntityEvent e) {
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
	public void onPlaceEntity(EntityPlaceEvent e) {
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
		String name = e.getEntity().getLocation().getWorld().getName();
		if (name.startsWith("world") || name.startsWith("smp") || name.startsWith("resource")) {
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
				// Prevents PvP
				else if (e.getEntity() instanceof Player target) {
					if (e.getDamageSource().getCausingEntity() != null) {
						if (e.getDamageSource().getCausingEntity() instanceof Player attacker) {
							Dominion attackerDominion = DominionUtils.getPlayerDominion(attacker.getUniqueId());
							Dominion targetDominion = DominionUtils.getPlayerDominion(target.getUniqueId());
							if (attackerDominion != null && targetDominion != null) {
								// Do not display extra messages when at spawn
								if (AranarthUtils.isSpawnLocation(target.getLocation())) {
									return;
								}

								// Prevent PvP within the same Dominion
								if (attackerDominion.getLeader().equals(targetDominion.getLeader())) {
									e.setCancelled(true);
									AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
									attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthPlayer.getNickname() + " &7as you are both in &e" + attackerDominion.getName()));
								}
								// Prevent PvP between allies
								else if (DominionUtils.areAllied(attackerDominion, targetDominion)) {
									e.setCancelled(true);
									AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
									attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthPlayer.getNickname() + " &7as you are &5Allied"));
								}
								// Prevent PvP between truces
								else if (DominionUtils.areTruced(attackerDominion, targetDominion)) {
									e.setCancelled(true);
									AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
									attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthPlayer.getNickname() + " &7as you are &dTruced"));
								} else {
									Dominion chunkDominion = DominionUtils.getDominionOfChunk(target.getLocation().getChunk());
									// Prevent damage if they're in their own Dominion's land and you are not allied, truced, or enemied
									if (chunkDominion != null && chunkDominion.getLeader().equals(targetDominion.getLeader())) {
										if (!DominionUtils.areEnemied(attackerDominion, targetDominion)) {
											e.setCancelled(true);
											AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
											attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm &e" + aranarthPlayer.getNickname() + " &7in their lands as you are &fNeutral"));
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Prevents players from opening containers and other functional blocks that are not in another dominion.
	 */
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		Block block = e.getClickedBlock();
		Player player = e.getPlayer();
		if (block == null) {
			return;
		}

		if (isDominionProtectedBlock(block)) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (!aranarthPlayer.getIsInAdminMode()) {
				// Only show the error if it is not a shop
				if (ShopUtils.getShopFromLocation(block.getLocation()) == null) {
					Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
					Dominion chunkDominion = DominionUtils.getDominionOfChunk(block.getChunk());
					// Allies can interact with all the blocks
					if (playerDominion == null || chunkDominion == null
							|| !DominionUtils.areAllied(playerDominion, chunkDominion)) {
						boolean isActionPrevented = applyLogic(player, block, null);
						if (isActionPrevented) {
							e.setCancelled(true);
						}
					}
				}
			}
		}
	}

	/**
	 * Confirms if the input block is one of the Dominion-protected blocks from being interacted with.
	 * @param block The block being interacted with.
	 * @return Confirmation if the input block is one of the Dominion-protected blocks from being interacted with.
	 */
	private boolean isDominionProtectedBlock(Block block) {
		return AranarthUtils.isContainerBlock(block) || block.getType().name().endsWith("_SIGN") || block.getType() == Material.NOTE_BLOCK
			|| block.getType() == Material.SMOKER || block.getType() == Material.BLAST_FURNACE || block.getType() == Material.FURNACE
			|| block.getType() == Material.JUKEBOX || block.getType() == Material.LEVER || block.getType().name().endsWith("_TRAPDOOR")
			|| block.getType().name().endsWith("_DOOR") || block.getType().name().endsWith("_BUTTON")
			|| block.getType().name().endsWith("_GATE") || block.getType() == Material.CRAFTER || block.getType() == Material.HOPPER;
	}

	/**
	 * Prevents players from placing item frames and paintings in another dominion.
	 */
	@EventHandler
	public void onEntityPlace(HangingPlaceEvent e) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
		if (!aranarthPlayer.getIsInAdminMode()) {
			boolean isActionPrevented = applyLogic(e.getPlayer(), e.getBlock(), null);
			if (isActionPrevented) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents players from using villagers that are in a different Dominion.
	 */
	@EventHandler
	public void onVillagerClick(PlayerInteractEntityEvent e) {
		if (!(e.getRightClicked() instanceof Villager)) {
			return;
		}

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
		if (!aranarthPlayer.getIsInAdminMode()) {
			boolean isActionPrevented = applyLogic(e.getPlayer(), null, e.getRightClicked());
			if (isActionPrevented) {
				e.setCancelled(true);
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

		// If the block/entity is in a dominion
		if (dominion != null) {
			Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
			// If the player is not in the dominion of the block
			if (playerDominion == null || !dominion.getLeader().equals(playerDominion.getLeader())) {
				player.sendMessage(ChatUtils.chatMessage("&cYou are not in the Dominion of &e" + dominion.getName()));
				return true;
			}
		}
		return false;
	}
}
