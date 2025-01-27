package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GuiQuiver {

	private final Player player;
	private final Inventory initializedGui;

	public GuiQuiver(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		if (initializedGui != null) {
			player.openInventory(initializedGui);
		}
	}
	
	private Inventory initializeGui(Player player) {

		Inventory gui = null;
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		List<ItemStack> arrows = new ArrayList<>();
		for (ItemStack is : aranarthPlayer.getArrows()) {
			if (is != null) {
				ItemStack clone = is.clone();
				arrows.add(clone);
			}
		}

		List<ItemStack> initializedArrows = new ArrayList<>();
		int guiSize = 0;

		if (!arrows.isEmpty()) {
            for (ItemStack arrow : arrows) {
                if (Objects.nonNull(arrow)) {

					// Arrow selector
					if (player.isSneaking()) {
						arrow.setAmount(1);
						if (!initializedArrows.contains(arrow)) {
							initializedArrows.add(arrow);
							guiSize++;
						}
					}

					// Modify Quiver inventory
					else {
						initializedArrows.add(arrow);
					}
                }
            }
			// Size is based on which method is used
			// If the amount is a multiple of 9, use a full row
			if (guiSize % 9 != 0) {
				guiSize = ((int) (double) (guiSize / 9) + 1) * 9;
			}
			if (!player.isSneaking()) {
				guiSize = 45;
				aranarthPlayer.setIsAddingToQuiver(true);
			}
			if (guiSize == 0) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have any arrows in your Quiver!"));
				return null;
			}
			gui = Bukkit.getServer().createInventory(player, guiSize, "Quiver");

            for (ItemStack initializedArrow : initializedArrows) {
                gui.addItem(initializedArrow);
            }
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou do not have any arrows in your Quiver!"));
		}
		
		return gui;
	}

}
