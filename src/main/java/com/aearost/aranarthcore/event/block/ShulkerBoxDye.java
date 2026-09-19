package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Allows players to dye a placed shulker box by sneaking and right-clicking it while holding a dye.
 */
public class ShulkerBoxDye {

    public void execute(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = e.getClickedBlock();
        if (block == null || !block.getType().name().endsWith("SHULKER_BOX")) {
            return;
        }

        Player player = e.getPlayer();
        if (!AranarthUtils.isPhysicallySneaking(player.getUniqueId())) {
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR || !heldItem.getType().name().endsWith("_DYE")) {
            return;
        }

        // Derive the target shulker box material from the dye name
        String targetName = heldItem.getType().name().replace("_DYE", "_SHULKER_BOX");
        Material targetMaterial = Material.getMaterial(targetName);
        if (targetMaterial == null) {
            return;
        }

        if (block.getType() == targetMaterial) {
            e.setCancelled(true);
            player.sendMessage(ChatUtils.chatMessage(Lang.get("shulker.already_that_color")));
            return;
        }

        // Check locked container - only trusted players may dye it
        LockedContainer container = AranarthUtils.getLockedContainerAtBlock(block);
        if (container != null) {
            UUID uuid = player.getUniqueId();
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
            boolean trusted = container.getTrusted().contains(uuid);
            if (!trusted && !aranarthPlayer.isInAdminMode()) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage(Lang.get("lock.no_permission_open")));
                return;
            }
        }

        e.setCancelled(true);

        // Preserve shulker box contents across the block type change
        ShulkerBox oldState = (ShulkerBox) block.getState();
        ItemStack[] contents = oldState.getInventory().getContents();

        block.setType(targetMaterial);

        ShulkerBox newState = (ShulkerBox) block.getState();
        newState.getInventory().setContents(contents);
        newState.update();

        // Consume one dye from the player's hand
        if (heldItem.getAmount() > 1) {
            heldItem.setAmount(heldItem.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.playSound(player, Sound.BLOCK_SHULKER_BOX_CLOSE, 1F, 1.5F);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("shulker.dyed")));
    }
}
