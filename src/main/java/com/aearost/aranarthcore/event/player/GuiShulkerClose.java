package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiShulkerClose implements Listener {

	public GuiShulkerClose(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles updating the shulker inventory when the GUI is closed.
	 * @param e The event.
	 */
	@EventHandler
	public void onShulkerInventoryClose(final InventoryCloseEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Shulker") && e.getView().getType() == InventoryType.CHEST) {
			Inventory inventory = e.getInventory();
            HumanEntity player = e.getPlayer();
            ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
            ItemMeta meta = heldItem.hasItemMeta() ? heldItem.getItemMeta() : Bukkit.getItemFactory().getItemMeta(heldItem.getType());

            if (meta instanceof BlockStateMeta im) {
                if (im.getBlockState() instanceof ShulkerBox shulker) {
                    shulker.getInventory().setContents(inventory.getContents());
                    im.setBlockState(shulker);
                    heldItem.setItemMeta(im);
                    return;
                }
            }
            // Should never be reached as per GuiShulkerPreventDrop
            player.sendMessage(ChatUtils.chatMessage("&cYou are not holding a shulker box!"));
		}
		
	}
}
