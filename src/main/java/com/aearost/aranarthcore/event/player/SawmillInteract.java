package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.recipes.RecipeSawmill;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.MenuType;

/**
 * Opens the sawmill GUI when a player right-clicks a stonecutter placed on top of a wood block.
 */
public class SawmillInteract {
	public void execute(PlayerInteractEvent e) {
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		if (e.getHand() != EquipmentSlot.HAND) {
			return;
		}
		if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.STONECUTTER) {
			return;
		}

		Block below = e.getClickedBlock().getRelative(BlockFace.DOWN);
		if (!RecipeSawmill.isWoodBlock(below.getType())) {
			return;
		}

		e.setCancelled(true);
		Player player = e.getPlayer();
		MenuType.STONECUTTER.builder()
				.title(Component.text("Sawmill"))
				.checkReachable(false)
				.build(player)
				.open();
	}
}
