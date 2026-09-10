package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiBlacklist;
import com.aearost.aranarthcore.gui.GuiBlacklistEditor;
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
import java.util.List;
import java.util.Objects;

public class GuiBlacklistEditorClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        int presetIndex = GuiBlacklistEditor.getPresetIndex(player.getUniqueId());

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());

        // Ensure presets list is large enough
        while (ap.getBlacklistPresets().size() <= presetIndex) {
            ap.getBlacklistPresets().add(new BlacklistPreset("", new ArrayList<>()));
        }

        BlacklistPreset preset = ap.getBlacklistPresets().get(presetIndex);

        if (e.getClickedInventory() == null) {
            return;
        }

        int slot = e.getSlot();

        // Rename button
        if (e.getClickedInventory().getType() == InventoryType.CHEST && slot == 4) {
            ItemStack clicked = e.getClickedInventory().getItem(4);
            if (clicked != null && clicked.getType() == Material.NAME_TAG) {
                player.closeInventory();
                GuiBlacklistEditor.setAwaitingRename(player.getUniqueId(), presetIndex);
                player.sendMessage(ChatUtils.chatMessage("&7Enter a name for this preset in chat - up to 30 characters"));
                player.sendMessage(ChatUtils.chatMessage("&7Type &ecancel &7to abort"));
                return;
            }
        }

        // Back button
        if (e.getClickedInventory().getType() == InventoryType.CHEST && slot == 49) {
            ItemStack clicked = e.getClickedInventory().getItem(49);
            if (clicked != null && clicked.getType() == Material.ARROW) {
                PersistenceUtils.saveBlacklistPresetsAsync(player.getUniqueId());
                new GuiBlacklist(player).openGui();
                return;
            }
        }

        // Clicking in player inventory
        if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
            ItemStack clickedItem = e.getClickedInventory().getItem(slot);
            if (Objects.isNull(clickedItem) || clickedItem.getType() == Material.AIR) {
                return;
            }

            List<ItemStack> items = preset.getItems();
            if (items.size() >= 36) {
                player.sendMessage(ChatUtils.chatMessage("&cThis preset is full (36 items max)!"));
                return;
            }
            for (ItemStack existing : items) {
                if (existing.getType() == clickedItem.getType()) {
                    player.sendMessage(ChatUtils.chatMessage("&cThis item is already in the preset!"));
                    return;
                }
            }
            items.add(new ItemStack(clickedItem.getType(), 1));
            preset.setItems(items);
            PersistenceUtils.saveBlacklistPresetsAsync(player.getUniqueId());
            String presetLabel = preset.getName().isEmpty()
                    ? "Preset " + (presetIndex + 1)
                    : "the " + ChatUtils.translateToColor(preset.getName()) + "&7 Preset";
            player.sendMessage(ChatUtils.chatMessage("&7Added &e" + ChatUtils.getFormattedItemName(clickedItem.getType().name()) + " &7to " + presetLabel));
            player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.5F, 1.75F);
            refreshEditorSlots(player, items);
            return;
        }

        // Clicking in editor GUI
        if (e.getClickedInventory().getType() == InventoryType.CHEST) {
            if (slot < 9 || slot > 44) {
                return; // Top/bottom border rows
            }
            ItemStack clicked = e.getClickedInventory().getItem(slot);
            if (Objects.isNull(clicked) || clicked.getType() == Material.AIR) {
                return;
            }

            int itemIndex = slot - 9;
            List<ItemStack> items = preset.getItems();
            if (itemIndex >= items.size()) {
                return;
            }

            ItemStack removed = items.get(itemIndex);
            items.remove(itemIndex);
            preset.setItems(items);
            PersistenceUtils.saveBlacklistPresetsAsync(player.getUniqueId());
            player.sendMessage(ChatUtils.chatMessage("&7Removed &e" + ChatUtils.getFormattedItemName(removed.getType().name()) + " &7from preset"));
            player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.5F, 0.8F);
            refreshEditorSlots(player, items);
        }
    }

    private void refreshEditorSlots(Player player, List<ItemStack> items) {
        org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
        for (int i = 0; i < 36; i++) {
            top.setItem(9 + i, i < items.size() ? items.get(i) : null);
        }
    }
}
