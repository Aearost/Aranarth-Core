package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.meta.Damageable;

/**
 * Sends the user warning messages when their gear's durability reaches thresholds.
 */
public class DurabilityDecreaseWarning {

	public void execute(PlayerItemDamageEvent e) {
		if (e.getItem().getItemMeta() instanceof Damageable damageableItemMeta) {
			int maxDurability = e.getItem().getType().getMaxDurability();
			int damagedDurability = maxDurability - damageableItemMeta.getDamage();
			int thresholdA = (int) Math.round(maxDurability * 0.1);
			int thresholdB = (int) Math.round(maxDurability * 0.05);
			int thresholdC = (int) Math.round(maxDurability * 0.01);

			// Handles gear with low durability not showing helpful messages
			if (thresholdA < 10) {
				thresholdA = 10;
				thresholdB = 5;
				thresholdC = 3;
			}

			Player player = e.getPlayer();
			if (damagedDurability == thresholdA) {
				player.sendMessage(ChatUtils.chatMessage("&7Your &e" + ChatUtils.getFormattedItemName(e.getItem().getType().name())
															+" &7has only 10% durability remaining!"));
			} else if (damagedDurability == thresholdB) {
				player.sendMessage(ChatUtils.chatMessage("&cYour &e" + ChatUtils.getFormattedItemName(e.getItem().getType().name())
				+" &chas only 5% durability remaining!"));
			} else if (damagedDurability == thresholdC) {
				player.sendMessage(ChatUtils.chatMessage("&4&lYour &6&l" + ChatUtils.getFormattedItemName(e.getItem().getType().name())
				+" &4&lhas only 1% durability remaining!"));
			}
		}
	}
}
