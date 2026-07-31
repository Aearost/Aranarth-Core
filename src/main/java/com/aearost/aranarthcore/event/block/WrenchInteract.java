package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.gui.GuiWrench;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

import static com.aearost.aranarthcore.objects.CustomKeys.*;

/**
 * Handles right-clicking a block with a Wrench.
 */
public class WrenchInteract {

    public void execute(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(WRENCH, PersistentDataType.STRING)) {
            return;
        }

        // From here the item is confirmed as a wrench
        e.setCancelled(true);

        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = e.getPlayer();
        BlockData data = block.getBlockData();

        // Check that this block actually has properties
        if (GuiWrench.buildPropertyItems(data).isEmpty()) {
            player.sendMessage(ChatUtils.chatMessage("&cThis block has no properties"));
            return;
        }

        String lastBlock = meta.getPersistentDataContainer().get(WRENCH_LAST_BLOCK, PersistentDataType.STRING);
        String currentBlockKey = blockKey(block);
        boolean sameBlock = currentBlockKey.equals(lastBlock);

        if (!sameBlock) {
            // Apply Unbreaking
            int unbreakingLevel = meta.getEnchantLevel(Enchantment.UNBREAKING);
            boolean consumeDurability = unbreakingLevel == 0
                    || new Random().nextInt(unbreakingLevel + 1) == 0;

            if (consumeDurability && meta instanceof Damageable damageable) {
                int newDamage = damageable.getDamage() + 1;
                if (newDamage >= damageable.getMaxDamage()) {
                    player.getInventory().setItemInMainHand(null);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                    player.sendMessage(ChatUtils.chatMessage("&cYour Wrench has broken!"));
                    return;
                }
                damageable.setDamage(newDamage);
            }

            meta.getPersistentDataContainer().set(WRENCH_LAST_BLOCK, PersistentDataType.STRING, currentBlockKey);
            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);
        }

        new GuiWrench(player, block).openGui();
        player.playSound(player, Sound.BLOCK_METAL_PLACE, 1F, 0.5F);
    }

    private static String blockKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
