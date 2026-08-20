
package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiHeadExchange;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.objects.HeadEntry;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.HeadsDatabaseManager;
import com.aearost.aranarthcore.utils.MobHeadUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;

public class GuiHeadExchangeClick {

    public void execute(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;

        boolean clickedTop = e.getClickedInventory() == e.getView().getTopInventory();
        Inventory topInv = e.getView().getTopInventory();

        if (!clickedTop) {
            // Prevent shift-click into GUI and double-click collecting from the input slot
            if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                e.setCancelled(true);
            }
            return;
        }

        int slot = e.getSlot();

        // Input slot
        if (slot == GuiHeadExchange.SLOT_INPUT) {
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                    () -> GuiHeadExchange.updateExchangeSlots(topInv, player));
            return;
        }

        // All other top-inventory slots are locked
        e.setCancelled(true);

        switch (slot) {
            case GuiHeadExchange.SLOT_EXIT -> {
                returnInputItems(player, topInv);
                player.closeInventory();
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
            }
            case GuiHeadExchange.SLOT_OUTPUT -> processExchange(player, topInv);
            case GuiHeadExchange.SLOT_PREV_VARIANT -> cycleVariant(player, topInv, -1);
            case GuiHeadExchange.SLOT_NEXT_VARIANT -> cycleVariant(player, topInv, 1);
        }
    }

    private void cycleVariant(Player player, Inventory topInv, int direction) {
        ItemStack inputItem = topInv.getItem(GuiHeadExchange.SLOT_INPUT);
        if (inputItem == null || inputItem.getType() == Material.AIR) return;

        Material mat = inputItem.getType();
        List<HeadEntry> variants = HeadsDatabaseManager.getExchangeableHeads().stream()
                .filter(h -> h.material() == mat)
                .toList();
        if (variants.size() <= 1) return;

        int current = GuiHeadExchange.playerVariantIndex.getOrDefault(player.getUniqueId(), 0);
        int next = (current + direction + variants.size()) % variants.size();
        GuiHeadExchange.playerVariantIndex.put(player.getUniqueId(), next);

        GuiHeadExchange.updateExchangeSlots(topInv, player);
        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
    }

    private void processExchange(Player player, Inventory topInv) {
        ItemStack outputItem = topInv.getItem(GuiHeadExchange.SLOT_OUTPUT);
        if (outputItem == null || !outputItem.hasItemMeta()) return;
        if (!outputItem.getItemMeta().getPersistentDataContainer()
                .has(CustomKeys.HEAD_TEXTURE, PersistentDataType.STRING)) return;

        String texture = outputItem.getItemMeta().getPersistentDataContainer()
                .get(CustomKeys.HEAD_TEXTURE, PersistentDataType.STRING);
        String materialName = outputItem.getItemMeta().getPersistentDataContainer()
                .get(CustomKeys.HEAD_REQUIRED_MATERIAL, PersistentDataType.STRING);

        Material requiredMaterial = Material.getMaterial(materialName);
        if (requiredMaterial == null) return;

        ItemStack inputItem = topInv.getItem(GuiHeadExchange.SLOT_INPUT);
        if (inputItem == null || inputItem.getType() == Material.AIR) return;
        if (inputItem.getType() != requiredMaterial) return;

        int qty = inputItem.getAmount();
        String headName = ChatUtils.stripColorFormatting(outputItem.getItemMeta().getDisplayName());

        // Consume input items
        topInv.setItem(GuiHeadExchange.SLOT_INPUT, null);

        // Give heads
        int remaining = qty;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, 64);
            ItemStack head = MobHeadUtils.createCustomHead(texture, "&f" + headName);
            head.setAmount(stackSize);
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(head);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            remaining -= stackSize;
        }

        // Reset the GUI slots now that input is empty
        GuiHeadExchange.updateExchangeSlots(topInv, player);
    }

    static void returnInputItems(Player player, Inventory topInv) {
        ItemStack input = topInv.getItem(GuiHeadExchange.SLOT_INPUT);
        if (input != null && input.getType() != Material.AIR) {
            topInv.setItem(GuiHeadExchange.SLOT_INPUT, null);
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(input);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }
}
