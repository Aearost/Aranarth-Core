package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.SitUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Handles sitting on stairs and slabs, and cleaning up cushions once players stand up.
 */
public class SitListener implements Listener {

	public SitListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Sits the player on a cushion when right-clicking a stair or slab with an empty hand.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onClickStairOrSlab(final PlayerInteractEvent e) {
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		Player player = e.getPlayer();
		// Prevents the off-hand item from being used once the player has been sat down by the main hand
		if (e.getHand() != EquipmentSlot.HAND) {
			if (SitUtils.isSitting(player)) {
				e.setCancelled(true);
			}
			return;
		}

		Block block = e.getClickedBlock();
		if (block == null || player.isSneaking() || !player.getInventory().getItemInMainHand().getType().isAir()) {
			return;
		}
		if (!player.hasPermission("aranarth.sit") || !SitUtils.isSittableBlock(block)) {
			return;
		}

		// Only allow sitting in the wilderness or where the player can interact within a Dominion
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (!aranarthPlayer.isInAdminMode()) {
			Dominion dominion = DominionUtils.getDominionOfChunk(block.getChunk());
			if (dominion != null && !DominionUtils.hasPermission(player, dominion, DominionPermission.MISC_INTERACT)) {
				return;
			}
		}

		if (SitUtils.sitOnBlock(player, block)) {
			e.setCancelled(true);
		}
	}

	/**
	 * Removes the cushion once the player stands up.
	 * @param e The event.
	 */
	@EventHandler
	public void onDismount(final EntityDismountEvent e) {
		if (!(e.getEntity() instanceof Player player)) {
			return;
		}
		Entity cushion = e.getDismounted();
		if (!SitUtils.isSitCushion(cushion)) {
			return;
		}
		// Removed on the next tick so the dismount is fully processed first
		Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> SitUtils.onDismount(player, cushion));
	}

	/**
	 * Stands the player up when they leave the server.
	 * @param e The event.
	 */
	@EventHandler
	public void onQuit(final PlayerQuitEvent e) {
		SitUtils.standUp(e.getPlayer());
	}

	/**
	 * Prevents sit cushions from being damaged or broken.
	 * @param e The event.
	 */
	@EventHandler
	public void onCushionDamage(final EntityDamageEvent e) {
		if (SitUtils.isSitCushion(e.getEntity())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Prevents other players from interacting with sit cushions.
	 * @param e The event.
	 */
	@EventHandler
	public void onCushionInteract(final PlayerInteractEntityEvent e) {
		if (SitUtils.isSitCushion(e.getRightClicked())) {
			e.setCancelled(true);
		}
	}

}
