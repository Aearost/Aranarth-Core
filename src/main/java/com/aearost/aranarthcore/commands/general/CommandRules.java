package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
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
		sender.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lAranarth Rules &8- - -"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&61&8] &eBe kind and respectful to everyone, regardless of race, orientation, gender, religion, etc."));
		sender.sendMessage(ChatUtils.translateToColor("&8[&62&8] &7Do not spam chat, use caps lock, etc."));
		sender.sendMessage(ChatUtils.translateToColor("&8[&63&8] &eNo inappropriate conversation topics or excessive swearing"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&64&8] &7No inappropriate skins, builds, item names or nicknames"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&65&8] &eNo advertising or mentioning other servers"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&66&8] &7No intentional destruction of Dominion blocks - minimal/collateral griefing while raiding is tolerated"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&67&8] &eDo not grief the world's terrain unnecessarily - use the resource world instead"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&68&8] &7No glitch abuse, hacking, auto-clickers, lag machines, or any sort of abuse of the game"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&69&8] &eAsk Staff (the Council) before using mods - only client-side cosmetic mods are permitted (OptiFine, Shaders, MiniHUD, etc.)"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&610&8] &7Do not impersonate or disrespect Staff (the Council)"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&611&8] &eOnly the use of manually harvested crop, spawner, and farms are permitted"));
		return true;
	}

}
