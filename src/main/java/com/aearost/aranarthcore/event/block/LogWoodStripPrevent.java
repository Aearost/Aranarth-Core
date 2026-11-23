package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
                if (isStrippable(e.getClickedBlock())) {
                    if (!e.getPlayer().isSneaking()) {
                        e.setCancelled(true);
                        e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.getFor(e.getPlayer(), "log.must_sneak_strip")));
                    }
                }
            }
        }
    }

    /**
     * Determines if the input block can be stripped by an axe (unstripped log, wood, stem, hyphae or bamboo block).
     * @param block The block.
     * @return Confirmation whether the block can be stripped or not.
     */
    private boolean isStrippable(Block block) {
        if (block == null) {
            return false;
        }
        String name = block.getType().name();
        if (name.startsWith("STRIPPED_")) {
            return false;
        }
        Material type = block.getType();
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_HYPHAE")
                || type == Material.CRIMSON_STEM || type == Material.WARPED_STEM || type == Material.BAMBOO_BLOCK;
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
