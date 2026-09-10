package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiBlacklist;
import com.aearost.aranarthcore.gui.GuiBlacklistSelect;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.BlacklistPreset;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class GuiBlacklistSelectClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        String title = ChatUtils.stripColorFormatting(e.getView().getTitle());
        boolean isClear = title.equals(GuiBlacklistSelect.TITLE_CLEAR);

        if (e.getClickedInventory() == null) {
            return;
        }
        if (e.getClickedInventory().getType() != InventoryType.CHEST) {
            return;
        }

        int slot = e.getSlot();

        // Back button
        if (slot == 31) {
            ItemStack clicked = e.getClickedInventory().getItem(31);
            if (clicked != null && clicked.getType() == Material.ARROW) {
                new GuiBlacklist(player).openGui();
                return;
            }
        }

        // Preset slots
        int presetIndex = switch (slot) {
            case 11 -> 0;
            case 13 -> 1;
            case 15 -> 2;
            case 20 -> 3;
            case 22 -> 4;
            case 24 -> 5;
            default -> -1;
        };
        if (presetIndex < 0) {
            return;
        }

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());

        // Ensure presets list is large enough
        while (ap.getBlacklistPresets().size() <= presetIndex) {
            ap.getBlacklistPresets().add(new BlacklistPreset("", new ArrayList<>()));
        }

        if (isClear) {
            ap.getBlacklistPresets().get(presetIndex).setItems(new ArrayList<>());
            ap.getBlacklistPresets().get(presetIndex).setName("");
            // If this was the active preset, deactivate it
            if (ap.getActivePresetIndex() == presetIndex) {
                ap.setActivePresetIndex(-1);
            }
            PersistenceUtils.saveBlacklistPresetsAsync(player.getUniqueId());
            player.sendMessage(ChatUtils.chatMessage("&7Preset &e" + (presetIndex + 1) + " &7has been cleared"));
            player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.5F, 0.8F);
        } else {
            ap.setActivePresetIndex(presetIndex);
            PersistenceUtils.saveBlacklistPresetsAsync(player.getUniqueId());
            player.sendMessage(ChatUtils.chatMessage("&7Now using preset &e" + (presetIndex + 1)));
            player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.5F, 1.75F);
        }

        new GuiBlacklist(player).openGui();
    }
}
