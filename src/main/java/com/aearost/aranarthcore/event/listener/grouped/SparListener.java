package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.SparUtils;
import com.projectkorra.projectkorra.ability.PassiveAbility;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Handles spars started via /countdown, preventing either player from killing the other.
 */
public class SparListener implements Listener {

	public SparListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Makes stunned players invulnerable, and prevents them from dealing any damage.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onStunnedDamage(final EntityDamageEvent e) {
		if (e.getEntity() instanceof Player victim && SparUtils.isStunned(victim) && !SparUtils.isUnpreventable(e.getCause())) {
			e.setCancelled(true);
			return;
		}
		if (e.getDamageSource().getCausingEntity() instanceof Player attacker && SparUtils.isStunned(attacker)) {
			e.setCancelled(true);
		}
	}

	/**
	 * Cancels the killing blow between two sparring players and stuns the loser instead.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSparDamage(final EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player victim) || !SparUtils.isInSparWorld(victim)) {
			return;
		}
		UUID partnerUuid = SparUtils.getPartner(victim.getUniqueId());
		if (partnerUuid == null) {
			return;
		}

		UUID attackerUuid = SparUtils.getAttackerUuid(e.getDamageSource().getCausingEntity());
		boolean isFromPartner = partnerUuid.equals(attackerUuid);
		// Damage from anyone or anything else that has a source is not part of the spar
		if (attackerUuid != null && !isFromPartner) {
			return;
		}

		boolean isLethal = e.getFinalDamage() >= victim.getHealth();
		if (isLethal && !SparUtils.isUnpreventable(e.getCause())) {
			// Sourceless damage (fire ticks, wither, falling after being knocked, etc.) counts if the partner hit them recently
			if (isFromPartner || SparUtils.wasRecentlyHitByPartner(victim.getUniqueId())) {
				e.setCancelled(true);
				SparUtils.loseSpar(victim, Bukkit.getPlayer(partnerUuid));
				return;
			}
		}

		if (isFromPartner) {
			SparUtils.recordPartnerHit(victim.getUniqueId());
		}
	}

	/**
	 * Prevents stunned players from moving, other than falling down to the ground.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onStunnedMove(final PlayerMoveEvent e) {
		Player player = e.getPlayer();
		if (!SparUtils.isStunned(player) || player.isInsideVehicle()) {
			return;
		}
		Location from = e.getFrom();
		Location to = e.getTo();
		if (to == null) {
			return;
		}
		if (from.getX() == to.getX() && from.getZ() == to.getZ() && to.getY() <= from.getY()) {
			return;
		}
		Location frozen = from.clone();
		frozen.setY(Math.min(from.getY(), to.getY()));
		frozen.setYaw(to.getYaw());
		frozen.setPitch(to.getPitch());
		e.setTo(frozen);
	}

	/**
	 * Prevents stunned players from using items, such as ender pearls, potions and food.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onStunnedInteract(final PlayerInteractEvent e) {
		if (SparUtils.isStunned(e.getPlayer())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Prevents stunned players from eating or drinking.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onStunnedConsume(final PlayerItemConsumeEvent e) {
		if (SparUtils.isStunned(e.getPlayer())) {
			e.setCancelled(true);
		}
	}

	/**
	 * Prevents stunned players from using bending abilities. Passive abilities are always exempt.
	 * @param e The event.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onStunnedAbilityStart(final AbilityStartEvent e) {
		if (e.getAbility() instanceof PassiveAbility) {
			return;
		}
		Player player = e.getAbility().getPlayer();
		if (player != null && SparUtils.isStunned(player)) {
			e.setCancelled(true);
		}
	}

	/**
	 * Ends the spar and stun of a player that dies by other means.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onDeath(final PlayerDeathEvent e) {
		SparUtils.endSpar(e.getEntity().getUniqueId());
		SparUtils.clearStun(e.getEntity());
	}

	/**
	 * Ends the spar and stun of a player that leaves the server.
	 * @param e The event.
	 */
	@EventHandler
	public void onQuit(final PlayerQuitEvent e) {
		SparUtils.endSpar(e.getPlayer().getUniqueId());
		SparUtils.clearStun(e.getPlayer());
	}

}
