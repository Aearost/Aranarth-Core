package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

public class BannerExtendPatternLimit implements Listener {

	public BannerExtendPatternLimit(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Increases the amount of patterns able to be placed on a banner.
	 * @param e The event.
	 */
	@EventHandler
	public void onBannerEdit(final InventoryClickEvent e) {
		if (e.getClickedInventory() == null || e.getCurrentItem() == null) {
			return;
		}

		if (e.getClickedInventory().getType() == InventoryType.LOOM) {
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
					Bukkit.getLogger().info("C");
					// If removing a banner and not simply removing
					if (e.getClickedInventory().getContents()[clickedSlot] != null) {

						ItemStack banner = e.getClickedInventory().getContents()[clickedSlot];
						if (banner.getItemMeta() instanceof BannerMeta bannerMeta) {
							if (bannerMeta.getPatterns().size() >= 6) {
								Bukkit.getLogger().info("D");
								banner.setItemMeta(AranarthUtils.getPlayerBanner(e.getWhoClicked().getUniqueId()));
							}
						}
					}
				}
			}
			// Dye
			else if (clickedSlot == 1) {

			}
			// Banner Pattern
			else if (clickedSlot == 2) {

			}
			// Result Banner
			else if (clickedSlot == 3) {

			} else {
				Bukkit.getLogger().info("Something went wrong with clicking a slot while applying a banner pattern...");
			}
//			ItemStack item = e.getCurrentItem();
//			Bukkit.getLogger().info("A");
//			if (item.getType().name().endsWith("_BANNER")) {
//				Bukkit.getLogger().info("B");
//				if (item.getItemMeta() instanceof BannerMeta) {
//					Bukkit.getLogger().info("C");
//					BannerMeta meta = (BannerMeta) item.getItemMeta();
//
//					if (meta.getPatterns().size() >= 6) { // Bypass vanilla limit
//						Bukkit.getLogger().info("D");
//						// e.setCancelled(true);
//						ItemStack newBanner = item.clone();
//						BannerMeta newMeta = (BannerMeta) newBanner.getItemMeta();
//						List<Pattern> patterns = meta.getPatterns();
//					}
//					//Pattern pattern = new Pattern(DyeColor, PatternType);
////					newMeta.setPatterns(meta.getPatterns());
////					newBanner.setItemMeta(newMeta);
////					e.setCurrentItem(newBanner);
//				} else {
//					Bukkit.getLogger().info("E");
//				}
//			}
		} else {

        }
	}
}
