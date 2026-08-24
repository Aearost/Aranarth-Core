package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.List;

import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_LEVEL;
import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_TYPE;
import static com.aearost.aranarthcore.objects.CustomKeys.PRESERVATION_USES;

/**
 * Handles block breaks for pickaxes with the Incantation of Preservation.
 */
public class IncantationPreservationBlockBreak {
    public void execute(BlockBreakEvent e) {
        Material type = e.getBlock().getType();
        Location location = e.getBlock().getLocation();

        boolean specialBlock = type == Material.SPAWNER
                || type == Material.TRIAL_SPAWNER
                || type == Material.REINFORCED_DEEPSLATE
                || type == Material.DRAGON_EGG;

        if (!specialBlock) return;

        if (type == Material.DRAGON_EGG) {
            // Cancel to prevent the vanilla teleport behavior, then manually remove and drop
            e.setCancelled(true);
            e.getBlock().setType(Material.AIR);
            location.getWorld().dropItemNaturally(location, new ItemStack(Material.DRAGON_EGG, 1));
        } else {
            e.setDropItems(false);
            location.getWorld().dropItemNaturally(location, new ItemStack(type, 1));
        }

        // Decrement uses and strip the incantation when exhausted
        Player player = e.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        ItemMeta meta = heldItem.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(PRESERVATION_USES)) return;

        int uses = meta.getPersistentDataContainer().get(PRESERVATION_USES, PersistentDataType.INTEGER);
        uses--;

        if (uses <= 0) {
            meta.getPersistentDataContainer().remove(INCANTATION_TYPE);
            meta.getPersistentDataContainer().remove(INCANTATION_LEVEL);
            meta.getPersistentDataContainer().remove(PRESERVATION_USES);
            meta.removeEnchant(Enchantment.SILK_TOUCH);
            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.removeIf(line -> ChatUtils.stripColorFormatting(line).equals("Preservation"));
                meta.setLore(lore.isEmpty() ? null : lore);
            }
            player.sendMessage(ChatUtils.chatMessage("&5Your Incantation of Preservation has expired."));
        } else {
            meta.getPersistentDataContainer().set(PRESERVATION_USES, PersistentDataType.INTEGER, uses);
        }

        heldItem.setItemMeta(meta);
    }
}
