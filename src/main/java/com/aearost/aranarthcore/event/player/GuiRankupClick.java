package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.EventUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.text.NumberFormat;

/**
 * Handles logic for clicking in the Rank-Up GUI
 */
public class GuiRankupClick {
	public void execute(InventoryClickEvent e) {
		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		e.setCancelled(true);

		int slot = e.getSlot();
		// Rankup
		if (slot == 14) {
			Player player = (Player) e.getWhoClicked();
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			double balance = aranarthPlayer.getBalance();
			String clickedItem = e.getClickedInventory().getItem(slot).getItemMeta().getDisplayName();
			String[] parts = clickedItem.split(" ");

			String priceWithoutDollarSign = ChatUtils.stripColorFormatting(parts[parts.length - 1]).substring(1);
			String priceWithoutCommas = priceWithoutDollarSign.replaceAll(",", "");
			double price = Double.parseDouble(priceWithoutCommas);

			if (balance >= price) {
				if (hasMinimumMcmmoTotal(player)) {
					if (hasMinimumMcmmoPerSkill(player)) {
						String rankDisplay = clickedItem.split(" ")[2];
						String aOrAn = "a";

						NumberFormat formatter = NumberFormat.getCurrencyInstance();
						aranarthPlayer.setBalance(balance - price);
						aranarthPlayer.setRank(aranarthPlayer.getRank() + 1);
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

						if (ChatUtils.stripColorFormatting(rankDisplay).equals("Esquire")
								|| ChatUtils.stripColorFormatting(rankDisplay).equals("Emperor")
								|| ChatUtils.stripColorFormatting(rankDisplay).equals("Empress")) {
							aOrAn = "an";
						}
						DiscordUtils.updateRank(player, aranarthPlayer.getRank(), true);

						Bukkit.broadcastMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has become " + aOrAn + " " + rankDisplay + "&7!"));
						PermissionUtils.evaluatePlayerPermissions(player);
						player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
						player.closeInventory();
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not meet the mcMMO requirements per category!"));
						player.closeInventory();
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cYou do not meet the overall mcMMO level requirements!"));
					player.closeInventory();
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough money to rankup!"));
				player.closeInventory();
			}
		}
		// Cancel
		else if (slot == 12) {
			Player player = (Player) e.getWhoClicked();
			player.playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, 0.8F, 0.5F);
			e.getWhoClicked().closeInventory();
		}
	}

	/**
	 * Determines if the player has the minimum amount of overall mcMMO levels to rank up.
	 * @param player The player attempting to rank up.
	 * @return Confirmation if the player has the minimum amount of overall mcMMO levels to rank up.
	 */
	private boolean hasMinimumMcmmoTotal(Player player) {
		McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(player);

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		int minimum = 0;

		switch (aranarthPlayer.getRank()) {
			case 0 -> minimum = 100;
			case 1 -> minimum = 250;
			case 2 -> minimum = 500;
			case 3 -> minimum = 1000;
			case 4 -> minimum = 2500;
			case 5 -> minimum = 7500;
			case 6 -> minimum = 12500;
			case 7 -> minimum = 25000;
			default -> minimum = 50000;
		}

		return mcMMOPlayer.getPowerLevel() >= minimum;
	}

	/**
	 * Determines if the player has the minimum sum of mcMMO levels per category to rank up.
	 * Each of the three categories (Gathering, Combat, Miscellaneous) must independently meet the minimum.
	 * @param player The player attempting to rank up.
	 * @return Confirmation if the player has the minimum sum of mcMMO levels per category to rank up.
	 */
	private boolean hasMinimumMcmmoPerSkill(Player player) {
		McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(player);

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		int minimum = 0;
		switch (aranarthPlayer.getRank()) {
			case 0 -> minimum = 0;
			case 1 -> minimum = 0;
			case 2 -> minimum = 150;
			case 3 -> minimum = 375;
			case 4 -> minimum = 750;
			case 5 -> minimum = 1250;
			case 6 -> minimum = 2500;
			case 7 -> minimum = 5000;
			default -> minimum = 5000;
		}

		if (minimum == 0) {
			return true;
		}

		int gatheringSkillsLevelTotal = mcMMOPlayer.getSkillLevel(PrimarySkillType.EXCAVATION)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.FISHING)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.HERBALISM)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.MINING)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.WOODCUTTING);

		int combatSkillsLevelTotal = mcMMOPlayer.getSkillLevel(PrimarySkillType.ARCHERY)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.AXES)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.CROSSBOWS)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.MACES)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.SWORDS)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.SPEARS)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.TAMING)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.TRIDENTS)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.UNARMED);

		int miscSkillsLevelTotal = mcMMOPlayer.getSkillLevel(PrimarySkillType.ACROBATICS)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.ALCHEMY)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.REPAIR)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.SALVAGE)
				+ mcMMOPlayer.getSkillLevel(PrimarySkillType.SMELTING);

		return gatheringSkillsLevelTotal >= minimum && combatSkillsLevelTotal >= minimum && miscSkillsLevelTotal >= minimum;
	}
}
