package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiRankup;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
			List<Double> configCosts = AranarthCore.getInstance().getConfig().getDoubleList("economy.rankup-costs");
			NumberFormat nf = NumberFormat.getNumberInstance();
			// Index 0 is a placeholder so that rankupCosts[currentRank+1] lines up after the increment below
			String[] rankupCosts = new String[configCosts.size() + 1];
			rankupCosts[0] = Lang.getFor(player, "rank.cost_free");
			for (int i = 0; i < configCosts.size(); i++) {
				rankupCosts[i + 1] = "$" + nf.format(configCosts.get(i).longValue());
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			int currentRank = aranarthPlayer.getRank();

			// If the player is already an Emperor, there are no further ranks
			if (currentRank == 8) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "rank.already_highest")));
				return true;
			}

			// Increase to display the next rank
			currentRank++;
			String rankId = aranarthPlayer.getPronouns().getRankId(currentRank);
			GuiRankup gui = new GuiRankup(player, rankId, rankupCosts[currentRank]);
			gui.openGui();
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
		}
		return true;
	}

}
