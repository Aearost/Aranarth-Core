package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.AfkCancelByInteract;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;

/**
 * Treats every form of direct player input as activity for AFK purposes, beyond moving, chatting, commands and inventories.
 */
public class AfkActivityListener implements Listener {

	public AfkActivityListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * WASD, jump, sneak and sprint key changes, including while riding a vehicle or when movement is blocked.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onInput(PlayerInputEvent e) {
		new AfkCancelByInteract().execute(e.getPlayer());
	}

	/**
	 * Left-clicking, attacking, mining, dropping items and successful entity interactions all swing the arm.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onArmSwing(PlayerAnimationEvent e) {
		new AfkCancelByInteract().execute(e.getPlayer());
	}

	/**
	 * Right click uses that do not swing the arm, such as eating, drinking, bows and shields.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onInteract(PlayerInteractEvent e) {
		if (e.getAction() == Action.PHYSICAL) {
			return;
		}
		new AfkCancelByInteract().execute(e.getPlayer());
	}

	/**
	 * Scrolling or pressing a number key to change the held hotbar slot.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onHotbarChange(PlayerItemHeldEvent e) {
		new AfkCancelByInteract().execute(e.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onSwapHands(PlayerSwapHandItemsEvent e) {
		new AfkCancelByInteract().execute(e.getPlayer());
	}

	/**
	 * The book screen blocks all other input and is only sent once closed, so long writing sessions end AFK here.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBookEdit(PlayerEditBookEvent e) {
		new AfkCancelByInteract().execute(e.getPlayer());
	}
}
