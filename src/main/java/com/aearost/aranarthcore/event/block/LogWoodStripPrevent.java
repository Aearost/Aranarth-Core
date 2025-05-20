package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Prevents stripping a log or wood block if the player is not sneaking.
 */
public class LogWoodStripPrevent {
    public void execute(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.HAND) {
            if (isHoldingAxe(e.getPlayer())) {
                if (getMaterialIfLogOrWood(e.getClickedBlock()) != null) {
                    if (!e.getPlayer().isSneaking()) {
                        e.setCancelled(true);
                        e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou must be sneaking to strip logs!"));
                    }
                }
            }
        }
    }

    /**
     * Determines if the input block is a log/wood block or not.
     * @param block The block.
     * @return Confirmation whether the block is a log/wood or not.
     */
    private Material getMaterialIfLogOrWood(Block block) {
        if (block.getType() == Material.OAK_LOG || block.getType() == Material.BIRCH_LOG
                || block.getType() == Material.SPRUCE_LOG || block.getType() == Material.JUNGLE_LOG
                || block.getType() == Material.DARK_OAK_LOG || block.getType() == Material.ACACIA_LOG
                || block.getType() == Material.CRIMSON_STEM || block.getType() == Material.WARPED_STEM
                || block.getType() == Material.MANGROVE_LOG || block.getType() == Material.CHERRY_LOG
                || block.getType() == Material.OAK_WOOD || block.getType() == Material.BIRCH_WOOD
                || block.getType() == Material.SPRUCE_WOOD || block.getType() == Material.JUNGLE_WOOD
                || block.getType() == Material.DARK_OAK_WOOD || block.getType() == Material.ACACIA_WOOD
                || block.getType() == Material.CRIMSON_HYPHAE || block.getType() == Material.WARPED_HYPHAE
                || block.getType() == Material.MANGROVE_WOOD || block.getType() == Material.CHERRY_WOOD) {
            return block.getType();
        } else {
            return null;
        }
    }

    /**
     * Determines if the player is holding an axe or not.
     * @param player The player.
     * @return Confirmation whether the player is holding an axe or not.
     */
    private boolean isHoldingAxe(Player player) {
        Material item = player.getInventory().getItemInMainHand().getType();
        return item == Material.WOODEN_AXE || item == Material.STONE_AXE || item == Material.IRON_AXE
                || item == Material.GOLDEN_AXE || item == Material.DIAMOND_AXE || item == Material.NETHERITE_AXE;
    }
}
