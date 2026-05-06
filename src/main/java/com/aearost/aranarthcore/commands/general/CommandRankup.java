package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.gui.GuiRankup;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.List;

/**
 * Displays the Rankup GUI pertaining to the player.
 */
public class CommandRankup implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
			String[] maleRanks = new String[] { "&a&lPeasant", "&d&lEsquire", "&7&lKnight", "&5&lBaron", "&8&lCount",
					"&6&lDuke", "&b&lPrince", "&9&lKing", "&4&lEmperor" };
			String[] femaleRanks = new String[] { "&a&lPeasant", "&d&lEsquire", "&7&lKnight", "&5&lBaroness",
					"&8&lCountess", "&6&lDuchess", "&b&lPrincess", "&9&lQueen", "&4&lEmpress" };
			List<Double> configCosts = AranarthCore.getInstance().getConfig().getDoubleList("economy.rankup-costs");
			NumberFormat nf = NumberFormat.getNumberInstance();
			// Index 0 is a placeholder so that rankupCosts[currentRank+1] lines up after the increment below
			String[] rankupCosts = new String[configCosts.size() + 1];
			rankupCosts[0] = "FREE";
			for (int i = 0; i < configCosts.size(); i++) {
				rankupCosts[i + 1] = "$" + nf.format(configCosts.get(i).longValue());
			}

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
			GuiRankup gui = new GuiRankup(player, rankName, rankupCosts[currentRank]);
			gui.openGui();
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
		}
		return true;
	}

}
