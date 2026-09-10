package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.enums.QuestTaskType;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.QuestUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Tracks quest progress when a player harvests glow berries from cave vines.
 */
public class CaveVineHarvest {

	public void execute(PlayerInteractEvent e) {
		if (e.getHand() != EquipmentSlot.HAND) {
			return;
		}

		Block block = e.getClickedBlock();
		if (block == null || (block.getType() != Material.CAVE_VINES && block.getType() != Material.CAVE_VINES_PLANT)) {
			return;
		}

		if (!(block.getBlockData() instanceof CaveVines vine)) {
			return;
		}

		if (!vine.isBerries()) {
			return;
		}

		if (AranarthUtils.isSpawnLocation(block.getLocation())) {
			return;
		}

		Player player = e.getPlayer();
		Dominion blockDominion = DominionUtils.getDominionOfChunk(block.getChunk());
		Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());

		if (blockDominion != null) {
			if (playerDominion == null || !playerDominion.isSameDominion(blockDominion)) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (!aranarthPlayer.isInAdminMode()) {
					e.setCancelled(true);
					return;
				}
			}
		}

		e.setCancelled(true);

		block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.GLOW_BERRIES, 1));

		vine.setBerries(false);
		block.setBlockData(vine);

		block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CAVE_VINES_PICK_BERRIES, 1.0F, 1.0F);
		QuestUtils.updateProgress(player, QuestTaskType.HARVEST_CROPS, 1);
	}
}
