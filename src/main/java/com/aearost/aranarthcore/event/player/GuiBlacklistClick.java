package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiBlacklist;
import com.aearost.aranarthcore.gui.GuiBlacklistEditor;
import com.aearost.aranarthcore.gui.GuiBlacklistSelect;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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

        // On/off toggle
        if (slot == 4) {
            int method = AranarthUtils.toggleBlacklist(player);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
            player.getOpenInventory().getTopInventory().setItem(4, GuiBlacklist.buildOnOffButton(player, method != -1));
            return;
        }

        // Mode toggle (ignore/trash)
        if (slot == 28) {
            int next = (ap.getBlacklistingMethod() == -1 ? ap.getLastActiveBlacklistingMethod() : ap.getBlacklistingMethod()) == 0 ? 1 : 0;
            if (ap.getBlacklistingMethod() == -1) {
                // Blacklist is off, so only remember the mode for when it is toggled back on
                ap.setLastActiveBlacklistingMethod(next);
                AranarthUtils.setPlayer(player.getUniqueId(), ap);
                String modeName = Lang.getFor(player, next == 0 ? "blacklist.mode_ignore" : "blacklist.mode_trash");
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "blacklist.mode_saved", "mode", modeName)));
            } else {
                AranarthUtils.setBlacklistMethod(player, next);
            }
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
            player.getOpenInventory().getTopInventory().setItem(28, GuiBlacklist.buildModeButton(player, next));
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
