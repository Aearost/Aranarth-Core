package com.aearost.aranarthcore.event;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;

public class GuiQuiverPreventNonArrowAdd implements Listener {

	public GuiQuiverPreventNonArrowAdd(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from adding non-arrow items to the arrows inventory
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onGuiClick(final InventoryClickEvent e) {
		if (ChatUtils.stripColor(e.getView().getTitle()).equals("Quiver") && e.getView().getType() == InventoryType.CHEST) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}
			
			// If adding a new item to the arrows inventory
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
				
				if (!isItemArrow(clickedItem)) {
					e.setCancelled(true);
				}
			}
		}
	}
	
	private boolean isItemArrow(ItemStack item) {
		if (item.getType() == Material.ARROW || item.getType() == Material.TIPPED_ARROW
				|| item.getType() == Material.SPECTRAL_ARROW) {
			if (item.getType() == Material.TIPPED_ARROW) {
				PotionMeta meta = (PotionMeta) item.getItemMeta();
				if (meta.getBasePotionType() == PotionType.WATER
						|| meta.getBasePotionType() == PotionType.AWKWARD
						|| meta.getBasePotionType() == PotionType.MUNDANE
						|| meta.getBasePotionType() == PotionType.THICK) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
}
