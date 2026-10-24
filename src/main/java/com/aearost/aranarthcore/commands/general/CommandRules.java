package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Displays the rules of Aranarth.
 */
public class CommandRules implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "rules.guide_title")));
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "rules.rule_1")));
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "rules.rule_2")));
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "rules.rule_3")));
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "rules.rule_4")));
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "rules.rule_5")));
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "rules.rule_6")));
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "rules.rule_7")));
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "rules.rule_8")));
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "rules.rule_9")));
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "rules.rule_10")));
		return true;
	}

}
