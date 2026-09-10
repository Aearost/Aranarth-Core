package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiBlacklist;
import com.aearost.aranarthcore.gui.GuiBlacklistEditor;
import com.aearost.aranarthcore.gui.GuiBlacklistSelect;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Deals with all clicks of the main blacklist GUI.
 */
public class GuiBlacklistClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (e.getClickedInventory() == null) {
            return;
        }
        if (e.getClickedInventory().getType() != InventoryType.CHEST) {
            return;
        }

        int slot = e.getSlot();
        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());

        // Preset buttons
        int presetIndex = switch (slot) {
            case 11 -> 0;
            case 13 -> 1;
            case 15 -> 2;
            case 20 -> 3;
            case 22 -> 4;
            case 24 -> 5;
            default -> -1;
        };
        if (presetIndex >= 0) {
            new GuiBlacklistEditor(player, presetIndex).openGui();
            return;
        }

        // Mode toggle
        if (slot == 28) {
            int current = ap.getBlacklistingMethod();
            int next = current == 0 ? 1 : current == 1 ? -1 : 0;
            ap.setBlacklistingMethod(next);
            AranarthUtils.setPlayer(player.getUniqueId(), ap);
            String modeName = next == 0 ? "&eIgnore" : next == 1 ? "&eTrash" : "&cOff";
            player.sendMessage(ChatUtils.chatMessage("&7Blacklist mode set to " + modeName));
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
            player.getOpenInventory().getTopInventory().setItem(28, GuiBlacklist.buildToggleButton(next));
            return;
        }

        // Use Preset
        if (slot == 31) {
            new GuiBlacklistSelect(player, false).openGui();
            return;
        }

        // Clear Preset
        if (slot == 34) {
            new GuiBlacklistSelect(player, true).openGui();
        }
    }
}
