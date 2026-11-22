package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.SpecialArrowUtils;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.objects.CustomKeys.ARROW;

/**
 * Handles movement effects of special arrows, such as Rooting and Grapple Arrows.
 */
public class RootingArrowMovePrevent implements Listener {

	public RootingArrowMovePrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with cancelling explosion block damage.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent e) {
		if (e.getPlayer().getPersistentDataContainer() != null) {
			String type = e.getPlayer().getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
			if (type != null && type.equals("rooting")) {
				e.setCancelled(true);
			}
		}
	}
	
	
	/**
	 * Deals with cancelling explosion item damage.
	 * @param e The event.
	 */
	@EventHandler
	public void onEntityMove(final EntityMoveEvent e) {
		if (e.getEntity().getPersistentDataContainer() != null) {
			String type = e.getEntity().getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
			if (type != null && type.equals("rooting")) {
				e.setCancelled(true);
			}
		}
	}

	/**
	 * Starts pulling the player's Grapple or Tether rope when they sneak while holding a bow.
	 * @param e The event.
	 */
	@EventHandler
	public void onSneak(final PlayerToggleSneakEvent e) {
		if (e.isSneaking()) {
			SpecialArrowUtils.tryPullRope(e.getPlayer());
		}
	}

	/**
	 * Snaps a pulling Grapple or Tether rope when its user takes damage of any kind.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onRopeUserDamaged(final EntityDamageEvent e) {
		if (e.getEntity() instanceof Player player && e.getFinalDamage() > 0) {
			SpecialArrowUtils.snapRope(player);
		}
	}

	/**
	 * Clears any Grapple or Tether rope when the player leaves.
	 * @param e The event.
	 */
	@EventHandler
	public void onQuit(final PlayerQuitEvent e) {
		SpecialArrowUtils.clearRope(e.getPlayer().getUniqueId());
	}
}
