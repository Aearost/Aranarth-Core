package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.commands.general.CommandPrefix;
import com.aearost.aranarthcore.enums.PrefixStyle;
import com.aearost.aranarthcore.gui.GuiPrefix;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

public class GuiPrefixClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null || e.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        int slot = e.getSlot();

        if (slot == GuiPrefix.EXIT_SLOT) {
            player.closeInventory();
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
            return;
        }

        for (int type = 0; type < GuiPrefix.TYPES.length; type++) {
            if (slot != GuiPrefix.TYPE_SLOTS[type]) {
                continue;
            }
            // Cycles through Full, Short, and Symbol
            PrefixStyle current = GuiPrefix.getStyle(AranarthUtils.getPlayer(player.getUniqueId()), type);
            PrefixStyle next = PrefixStyle.values()[(current.ordinal() + 1) % PrefixStyle.values().length];
            CommandPrefix.applyStyle(player, GuiPrefix.TYPES[type], next);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
            player.getOpenInventory().getTopInventory().setItem(slot, GuiPrefix.buildTypeItem(player, type));
            return;
        }
    }

}
