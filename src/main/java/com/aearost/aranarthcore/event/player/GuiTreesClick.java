package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiTrees;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.trees.CustomTrees.Variant;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

/**
 * Handles clicks in the /trees GUI.
 */
public class GuiTreesClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        Inventory top = e.getView().getTopInventory();
        int page = aranarthPlayer.getCurrentGuiPageNum();
        int slot = e.getSlot();

        switch (slot) {
            case GuiTrees.SLOT_EXIT -> {
                player.closeInventory();
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
            }
            case GuiTrees.SLOT_ALL -> {
                aranarthPlayer.setBigTreeDisabled(!aranarthPlayer.isBigTreeDisabled());
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
                top.setItem(slot, GuiTrees.buildAllItem(player, aranarthPlayer));
            }
            case GuiTrees.SLOT_ENABLE_ALL, GuiTrees.SLOT_DISABLE_ALL -> {
                boolean disable = slot == GuiTrees.SLOT_DISABLE_ALL;
                for (Variant variant : GuiTrees.choosable()) {
                    aranarthPlayer.setTreeDisabled(variant.name(), disable);
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.5F, 1.75F);
                new GuiTrees(player, page).populateInto(top);
            }
            case GuiTrees.SLOT_PREVIOUS, GuiTrees.SLOT_NEXT -> {
                int target = page + (slot == GuiTrees.SLOT_NEXT ? 1 : -1);
                if (target < 0 || target >= GuiTrees.pageCount()) {
                    return;
                }
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
                new GuiTrees(player, target).populateInto(top);
            }
            default -> {
                Variant variant = GuiTrees.treeAt(page, slot);
                if (variant == null) {
                    return;
                }
                aranarthPlayer.setTreeDisabled(variant.name(), !aranarthPlayer.isTreeDisabled(variant.name()));
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
                top.setItem(slot, GuiTrees.buildTreeItem(player, aranarthPlayer, variant));
            }
        }
    }
}
