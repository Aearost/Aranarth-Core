package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Objects;

public class GuiPotionPreventNonPotionAdd implements Listener {

	public GuiPotionPreventNonPotionAdd(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from adding non-potion items to the potion inventory.
	 * @param e The event.
	 */
	@EventHandler
	public void onGuiClick(final InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Potions") && e.getView().getType() == InventoryType.CHEST) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}
			
			// If adding a new item to the potions inventory
			if (e.getClickedInventory().getSize() == 41) {
				ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
				// Ensures a non-empty slot is clicked
				if (Objects.isNull(clickedItem)) {
					// If placing potion back into player slots
					if (Objects.nonNull(e.getCursor())) {
						return;
					}
					e.setCancelled(true);
				}
				
				if (clickedItem.getType() != Material.POTION
						&& clickedItem.getType() != Material.SPLASH_POTION
						&& clickedItem.getType() != Material.LINGERING_POTION) {
					e.setCancelled(true);
				} else {
					PotionMeta meta = (PotionMeta) clickedItem.getItemMeta();
					// Prevent potions without effects from being added
					if (meta.getBasePotionType() == PotionType.AWKWARD ||
							meta.getBasePotionType() == PotionType.MUNDANE || 
							meta.getBasePotionType() == PotionType.THICK || 
							meta.getBasePotionType() == PotionType.WATER) {
						// Allows mcMMO potions to be placed in the inventory
						if (meta.getCustomEffects().isEmpty()) {
							e.setCancelled(true);
						}
					}
				}
			}
		}
	}
	
}
