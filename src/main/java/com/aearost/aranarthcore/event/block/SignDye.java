package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.PlayerShop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents dyeing a shop sign.
 */
public class SignDye {
    
    public void execute(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item != null) {
            if (item.getType().name().toLowerCase().endsWith("dye") && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (e.getClickedBlock() != null) {
                    if (e.getClickedBlock().getType().name().toLowerCase().endsWith("sign")) {
                        PlayerShop playerShop = AranarthUtils.getShop(e.getClickedBlock().getLocation());
                        if (playerShop != null) {
                            e.setCancelled(true);
                            e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot dye a player shop sign!"));
                        }
                    }
                }
            }
        }
    }
}
