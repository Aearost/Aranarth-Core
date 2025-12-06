package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Boost;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BoostExpBuffsListener implements Listener {

	public BoostExpBuffsListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the additional mcMMO EXP gain while server boosts are applied.
	 */
	@EventHandler
	public void onExpGain(McMMOPlayerXpGainEvent e) {
		String name = e.getPlayer().getLocation().getWorld().getName();
		if (name.startsWith("world") || name.startsWith("smp") || name.startsWith("resource")) {
			if (AranarthUtils.getServerBoosts().containsKey(Boost.MINER)) {
				if (e.getSkill() == PrimarySkillType.MINING) {
					e.setRawXpGained((float) (e.getRawXpGained() * 1.5));
				}
			}
			if (AranarthUtils.getServerBoosts().containsKey(Boost.HARVEST)) {
				if (e.getSkill() == PrimarySkillType.HERBALISM || e.getSkill() == PrimarySkillType.EXCAVATION
						|| e.getSkill() == PrimarySkillType.WOODCUTTING) {
					e.setRawXpGained((float) (e.getRawXpGained() * 1.5));
				}
			}
			if (AranarthUtils.getServerBoosts().containsKey(Boost.HUNTER)) {
				if (e.getSkill() == PrimarySkillType.SWORDS || e.getSkill() == PrimarySkillType.AXES
						|| e.getSkill() == PrimarySkillType.TRIDENTS || e.getSkill() == PrimarySkillType.MACES
						|| e.getSkill() == PrimarySkillType.ARCHERY || e.getSkill() == PrimarySkillType.CROSSBOWS) {
					e.setRawXpGained((float) (e.getRawXpGained() * 1.5));
				}
			}
			if (AranarthUtils.getServerBoosts().containsKey(Boost.CHI)) {
				if (e.getSkill() == PrimarySkillType.UNARMED || e.getSkill() == PrimarySkillType.ACROBATICS) {
					e.setRawXpGained((float) (e.getRawXpGained() * 1.5));
				}
			}
		}
	}
}
