package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PermissionUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Recounts a player shop's stock when its container is closed.
 */
public class ShopContainerClose {

    public void execute(InventoryCloseEvent e, Player player) {
        InventoryHolder holder = e.getInventory().getHolder(false);
        Set<Block> blocks = new LinkedHashSet<>();
        if (holder instanceof DoubleChest doubleChest) {
            if (doubleChest.getLeftSide(false) instanceof BlockState left) {
                blocks.add(left.getBlock());
            }
            if (doubleChest.getRightSide(false) instanceof BlockState right) {
                blocks.add(right.getBlock());
            }
        } else if (holder instanceof BlockState state) {
            blocks.add(state.getBlock());
        } else {
            return;
        }

        Set<Shop> shops = new LinkedHashSet<>();
        for (Block block : blocks) {
            Shop shop = ShopUtils.getShopForContainer(block);
            if (shop != null && shop.getUuid() != null) {
                shops.add(shop);
            }
        }

        boolean isWarned = false;
        for (Shop shop : shops) {
            ShopUtils.refreshShopStock(shop);

            if (!isWarned && player.getUniqueId().equals(shop.getUuid())
                    && !PermissionUtils.hasShulkerAssist(shop.getUuid())
                    && ShopUtils.hasShopItemInChestShulkers(shop)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "shop.shulker_no_perk")));
                isWarned = true;
            }
        }
    }
}
