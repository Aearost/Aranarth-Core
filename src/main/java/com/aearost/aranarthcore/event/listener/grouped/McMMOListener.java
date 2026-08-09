package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;
import com.gmail.nossr50.events.skills.repair.McMMOPlayerRepairCheckEvent;
import com.gmail.nossr50.events.skills.salvage.McMMOPlayerSalvageCheckEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles mcMMO-specific restrictions and interactions.
 */
public class McMMOListener implements Listener {

	public McMMOListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents mcMMO Repair from being used on Aranarthium armor.
	 */
	@EventHandler
	public void onRepairCheck(McMMOPlayerRepairCheckEvent e) {
		ItemStack repairedObject = e.getRepairedObject();
		if (repairedObject != null && repairedObject.getItemMeta() != null
				&& repairedObject.getItemMeta().getPersistentDataContainer().has(CustomKeys.ARMOR_TYPE, PersistentDataType.STRING)) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot repair Aranarthium armor!"));
		}
	}

	/**
	 * Prevents mcMMO Salvage from being used on Aranarthium armor.
	 */
	@EventHandler
	public void onSalvageCheck(McMMOPlayerSalvageCheckEvent e) {
		ItemStack salvageItem = e.getSalvageItem();
		if (salvageItem != null && salvageItem.getItemMeta() != null
				&& salvageItem.getItemMeta().getPersistentDataContainer().has(CustomKeys.ARMOR_TYPE, PersistentDataType.STRING)) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot salvage Aranarthium armor!"));
		}
	}

	/**
	 * Sends a custom cross-server broadcast when a player reaches a power level or
	 * individual skill level milestone (every 100 levels).
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onMcmmoLevelUp(McMMOPlayerLevelUpEvent e) {
		Player player = e.getPlayer();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		String displayName = aranarthPlayer != null ? aranarthPlayer.getNickname() : player.getName();

		int skillLevel = e.getSkillLevel();
		if (skillLevel % 100 == 0) {
			String skillName = e.getSkill().getName();
			Bukkit.broadcastMessage(ChatUtils.chatMessage(
					"&e" + displayName + " &7has reached level &e" + skillLevel + " &7in &e" + skillName));
		}

		int powerLevel = ExperienceAPI.getPowerLevel(player);
		if (powerLevel % 100 == 0) {
			Bukkit.broadcastMessage(ChatUtils.chatMessage(
					"&e" + displayName + " &7has reached an mcMMO power level &6" + powerLevel));
		}
	}

}
