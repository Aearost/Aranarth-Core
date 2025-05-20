package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

/**
 * Increases the amount of patterns able to be placed on a banner.
 */
public class BannerExtendPatternLimit {
	public void execute(InventoryClickEvent e) {
		if (e.getClickedInventory() == null || e.getCurrentItem() == null) {
			return;
		}

		int clickedSlot = e.getSlot();

		// Input banner
		if (clickedSlot == 0) {
			// If adding the banner
			if (e.getClickedInventory().getContents()[clickedSlot] == null) {
				ItemStack banner = e.getCursor();
				// Counts how many patterns are currently applied
				if (banner.getItemMeta() instanceof BannerMeta bannerMeta) {
					if (bannerMeta.getPatterns().size() >= 6) {
						// Since this is a clone, it might be updated as well when patterns are removed
						BannerMeta copyOfMeta = (BannerMeta) bannerMeta.clone();
						AranarthUtils.setPlayerBanner(e.getWhoClicked().getUniqueId(), copyOfMeta);

						int originalPatternAmount = bannerMeta.getPatterns().size() - 1;
						int i = originalPatternAmount;

						while (i >= originalPatternAmount) {
							bannerMeta.removePattern(i);
							i--;
						}
						banner.setItemMeta(bannerMeta);
						// This is putting a quantity of 2
						// Cannot remove it or it yields 0
						// No other line seems to be interfering
						// e.getClickedInventory().setItem(clickedSlot, banner);
					}
				}
			} else {
				// If removing a banner and not simply removing
				if (e.getClickedInventory().getContents()[clickedSlot] != null) {

					ItemStack banner = e.getClickedInventory().getContents()[clickedSlot];
					if (banner.getItemMeta() instanceof BannerMeta bannerMeta) {
						if (bannerMeta.getPatterns().size() >= 6) {
							banner.setItemMeta(AranarthUtils.getPlayerBanner(e.getWhoClicked().getUniqueId()));
						}
					}
				}
			}
		}

		// Uncomment below when working on this again
		// Dye
//		else if (clickedSlot == 1) {
//
//		}
//		// Banner Pattern
//		else if (clickedSlot == 2) {
//
//		}
//		// Result Banner
//		else if (clickedSlot == 3) {
//
//		} else {
//			Bukkit.getLogger().info("Something went wrong with clicking a slot while applying a banner pattern...");
//		}
	}
}
