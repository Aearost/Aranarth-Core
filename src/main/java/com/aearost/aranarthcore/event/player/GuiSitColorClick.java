package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiSitColor;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.SitUtils;
import org.bukkit.DyeColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class GuiSitColorClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null || e.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        int slot = e.getSlot();

        if (slot == GuiSitColor.EXIT_SLOT) {
            player.closeInventory();
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
            return;
        }

        int colorIndex = -1;
        for (int i = 0; i < GuiSitColor.COLOR_SLOTS.length; i++) {
            if (GuiSitColor.COLOR_SLOTS[i] == slot) {
                colorIndex = i;
                break;
            }
        }
        if (colorIndex == -1) {
            return;
        }

        if (!player.hasPermission("aranarth.sit.color")) {
            player.closeInventory();
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        DyeColor oldColor = SitUtils.getCushionColor(aranarthPlayer.getSitCushionColor());
        DyeColor newColor = GuiSitColor.COLORS[colorIndex];
        if (oldColor == newColor) {
            return;
        }

        aranarthPlayer.setSitCushionColor(newColor.name());
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        SitUtils.updateCushionColor(player, newColor);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("sit.color_set", "color", ChatUtils.getFormattedItemName(newColor.name()))));
        player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);

        // Update the previous and new selections in place
        Inventory top = player.getOpenInventory().getTopInventory();
        top.setItem(GuiSitColor.getSlotOfColor(oldColor), GuiSitColor.buildColorItem(player, oldColor, false));
        top.setItem(slot, GuiSitColor.buildColorItem(player, newColor, true));
    }

}
