package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.gui.GuiRankup;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Displays the Rankup GUI pertaining to the player.
 */
public class CommandRankup {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			String[] maleRanks = new String[] { "&a&lPeasant", "&d&lEsquire", "&7&lKnight", "&5&lBaron", "&8&lCount",
					"&6&lDuke", "&b&lPrince", "&9&lKing", "&4&lEmperor" };
			String[] femaleRanks = new String[] { "&a&lPeasant", "&d&lEsquire", "&7&lKnight", "&5&lBaroness",
					"&8&lCountess", "&6&lDuchess", "&b&lPrincess", "&9&lQueen", "&4&lEmpress" };
			String[] rankupCosts = new String[] { "FREE", "$250", "$1,250", "$5,000", "$10,000", "$25,000", "$100,000",
					"$500,000", "$2,500,000" };

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			int currentRank = aranarthPlayer.getRank();

			// If the player is already an Emperor, there are no further ranks
			if (currentRank == 8) {
				player.sendMessage(ChatUtils.chatMessage("&7You are already the highest rank!"));
				return true;
			}

			// Increase to display the next rank
			currentRank++;
			Pronouns pronouns = aranarthPlayer.getPronouns();
			String rankName = "";

			if (pronouns == Pronouns.MALE) {
				rankName = maleRanks[currentRank];
			} else {
				rankName = femaleRanks[currentRank];
			}
			GuiRankup gui = new GuiRankup(player, rankName, rankupCosts[currentRank + 1]);
			gui.openGui();
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
		}
		return true;
	}

}
