package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Objects;

public class DragonHeadClick implements Listener {

	public DragonHeadClick(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the interaction of clicking on a dragon head.
	 * @param e The event.
	 */
	@EventHandler
	public void onDragonHeadClick(final PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (e.getClickedBlock().getType() == Material.DRAGON_HEAD
					|| e.getClickedBlock().getType() == Material.DRAGON_WALL_HEAD) {
				Block head = e.getClickedBlock();
				Location location = e.getClickedBlock().getLocation();
				Player player = e.getPlayer();
				// If clicking and trying to add more fuel
				if (e.getItem().getType() == Material.GLASS_BOTTLE) {
					if (AranarthUtils.getDragonHeadFuelAmount(e.getClickedBlock().getLocation()) > 0) {
						HashMap<Integer, ItemStack> remains = player.getInventory()
								.addItem(new ItemStack(Material.DRAGON_BREATH, 1));
						int newAmount = e.getItem().getAmount() - 1;
						e.getItem().setAmount(newAmount);
						AranarthUtils.decrementDragonHeadFuelAmount(location);
						if (!remains.isEmpty()) {
							player.getLocation().getWorld().dropItemNaturally(player.getLocation(),
									remains.get(1));
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThis dragon head has no fuel!"));
					}
				}
				// If clicking with a chorus diamond to add fuel
				else if (e.getItem().getType() == Material.DIAMOND) {
					if (e.getItem().hasItemMeta()) {
						ItemMeta meta = e.getItem().getItemMeta();
						if (Objects.nonNull(meta)) {
							if (meta.hasLore()) {
								boolean isPoweredByRedstone = head.isBlockPowered() || head.isBlockIndirectlyPowered();
								int fuelAmountAdded = AranarthUtils.updateDragonHead(location, isPoweredByRedstone);
								int newAmount = e.getItem().getAmount() - 1;
								e.getItem().setAmount(newAmount);
								player.sendMessage(ChatUtils.chatMessage("&7You have added " + fuelAmountAdded + " fuel to the head!"));
							}
						}
					}
				}
			}

		}
	}
}
