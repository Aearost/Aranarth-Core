package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Prevents dyeing a shop sign or using an essence as a dye on any sign.
 */
public class SignDye {

    public void execute(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null) return;
        if (!item.getType().name().toLowerCase().endsWith("dye")) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        if (!block.getType().name().toLowerCase().endsWith("sign")) return;

        // Block essences from being used as dyes on any sign
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(CustomKeys.ESSENCE_TYPE, PersistentDataType.STRING)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.get("essence.cannot_use_as_dye")));
            // Force-resend the sign's block state 1 tick later to revert any client-side color change
            BlockState snapshot = block.getState();
            AranarthCore.getInstance().getServer().getScheduler().runTaskLater(AranarthCore.getInstance(), () -> snapshot.update(true, false), 1L);
            return;
        }

        Shop playerShop = ShopUtils.getShopFromLocation(block.getLocation());
        if (playerShop != null) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.get("shop.cannot_dye_sign")));
            BlockState snapshot = block.getState();
            AranarthCore.getInstance().getServer().getScheduler().runTaskLater(AranarthCore.getInstance(), () -> snapshot.update(true, false), 1L);
        }
    }
}
