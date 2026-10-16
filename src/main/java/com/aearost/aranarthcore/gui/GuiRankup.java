package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * A chest GUI that appears once a user clicks on the rank they are to rank up
 * to next. This GUI simply allows the user to confirm their upgrade to the
 * following rank.
 * 
 * @author Aearost
 *
 */
public class GuiRankup {

	private final Player player;
	private final Inventory initializedGui;

	public GuiRankup(Player player, String nextRankInfo, String rankupCost) {
		this.player = player;
		this.initializedGui = initializeGui(player, nextRankInfo, rankupCost);
	}

	public void openGui() {
		player.openInventory(initializedGui);
	}

	private Inventory initializeGui(Player player, String rankName, String rankupCost) {
		Inventory gui = GuiHolder.create(player, 27,
				Lang.getFor(player, "gui.rankup.title"));
		// The click handler prices the rankup from the config, the button name is only for display
		GuiHolder.of(gui).setData(new RankupOffer(AranarthUtils.getPlayer(player.getUniqueId()).getRank(), rankName));

		// Initialize Items
		ItemStack yellowPane = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
		ItemStack blackPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
		ItemStack rankup = new ItemStack(Material.LIME_CONCRETE);

		// Removing name of panes
		ItemMeta yellowPaneMeta = yellowPane.getItemMeta();
		yellowPaneMeta.setDisplayName(" ");
		yellowPane.setItemMeta(yellowPaneMeta);
		ItemMeta blackPaneMeta = blackPane.getItemMeta();
		blackPaneMeta.setDisplayName(" ");
		blackPane.setItemMeta(blackPaneMeta);

		ItemMeta cancelMeta = cancel.getItemMeta();
		cancelMeta.setDisplayName(Lang.getFor(player, "gui.rankup.cancel"));
		cancel.setItemMeta(cancelMeta);
		ItemMeta rankupMeta = rankup.getItemMeta();
		rankupMeta.setDisplayName(Lang.getFor(player, "gui.rankup.confirm", "rank", rankName, "cost", rankupCost));
		rankup.setItemMeta(rankupMeta);

		// Initialize GUI
		for (int position = 0; position < 27; position++) {
			// Top and bottom lines
			if (position < 9 || position >= 18) {
				gui.setItem(position, blackPane);
			}
		}

		gui.setItem(9, blackPane);
		gui.setItem(10, yellowPane);
		gui.setItem(11, yellowPane);
		gui.setItem(12, cancel);
		gui.setItem(13, yellowPane);
		gui.setItem(14, rankup);
		gui.setItem(15, yellowPane);
		gui.setItem(16, yellowPane);
		gui.setItem(17, blackPane);

		return gui;
	}

	/**
	 * The rank the player had when the GUI was opened and the display name of the next rank.
	 */
	public record RankupOffer(int fromRank, String nextRankName) {
	}

}
