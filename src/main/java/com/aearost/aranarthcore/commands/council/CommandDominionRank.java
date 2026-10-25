package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionLevelUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/**
 * Temporarily sets a dominion's level until its next re-evaluation.
 */
public class CommandDominionRank {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length < 3) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "ac dominionrank <dominion name> <rank>")));
			return true;
		}

		int level;
		try {
			level = Integer.parseInt(args[args.length - 1]);
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_number")));
			return true;
		}
		if (level < 1 || level > DominionLevelUtils.MAX_LEVEL) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "ac.dominionrank_invalid", "max", DominionLevelUtils.MAX_LEVEL)));
			return true;
		}

		// Dominion names may contain spaces, so everything between the sub-command and the rank is the name
		String targetName = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
		Dominion target = DominionUtils.getDominions().stream()
				.filter(d -> ChatUtils.stripColorFormatting(d.getName()).equalsIgnoreCase(targetName))
				.findFirst().orElse(null);
		if (target == null) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "dominion.not_found", "name", targetName)));
			return true;
		}

		if (DominionUtils.isHostedHere(target)) {
			DominionLevelUtils.overrideLevel(target, level);
		} else if (NetworkManager.isActive()) {
			// Levels are only re-evaluated on the hosting server, so the override must live there
			NetworkManager.getInstance().publishDominionLevelOverride(target.getId(), level);
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "ac.dominionrank_wrong_server", "server", DominionUtils.getHostingServerDisplayName(target))));
			return true;
		}
		sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "ac.dominionrank_success", "name", target.getName(), "level", level)));
		return true;
	}
}
