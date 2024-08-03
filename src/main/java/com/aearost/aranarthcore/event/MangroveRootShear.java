package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import com.aearost.aranarthcore.AranarthCore;

import java.util.Objects;

public class MangroveRootShear implements Listener {

	public MangroveRootShear(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Allows a player to remove the roots from a Muddy Mangrove Roots block.
	 * @param e The event.
	 */
	@EventHandler
	public void onRootsClick(final PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
			if (heldItem.getType() == Material.SHEARS) {
				if (Objects.requireNonNull(e.getClickedBlock()).getType() == Material.MUDDY_MANGROVE_ROOTS) {
					e.setCancelled(true);
					e.getClickedBlock().setType(Material.MUD);
					e.getClickedBlock().getWorld().dropItemNaturally(
							e.getClickedBlock().getLocation(), new ItemStack(Material.HANGING_ROOTS, 1));
					Damageable damageable = (Damageable) heldItem.getItemMeta();
					Objects.requireNonNull(damageable).setDamage(damageable.getDamage() + 1);
					heldItem.setItemMeta(damageable);
					e.getPlayer().playSound(e.getPlayer(), Sound.ENTITY_BOGGED_SHEAR, 0.25F, 1);
				}
			}
		}
	}
}
