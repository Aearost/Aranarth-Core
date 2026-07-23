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
		sender.sendMessage(ChatUtils.translateToColor("&8[&61&8] &eTreat everyone with respect. Harassment, discrimination, bullying, or personal attacks are not tolerated"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&62&8] &7Do not spam chat or intentionally disrupt it with excessive caps, chat flooding, or similar behavior"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&63&8] &eKeep chat appropriate for a 13+ community. Excessive swearing, sexual discussions, graphic content, or other inappropriate conversations are not allowed"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&64&8] &7Keep nicknames, skins, builds, and item names appropriate. Offensive or explicit content is prohibited"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&65&8] &eKeep destruction during Dominion raids limited. The deliberate or excessive destruction of Dominion blocks is prohibited"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&66&8] &7Do not unnecessarily damage or litter the overworld. Use the Resource World for large-scale gathering"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&67&8] &eDo not use hacks, exploits, auto-clickers, lag machines, or anything that provides an unfair advantage"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&68&8] &7Only client-side mods approved by the Council (staff) are permitted (i.e OptiFine, Shaders, MiniHUD, Litematica, etc)"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&69&8] &eRespect the Council and moderation decisions. Do not impersonate staff or intentionally interfere with moderation"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&610&8] &7Only small-scaled automatic crop, mob, and spawner farms are permitted to be built"));
		return true;
	}

}
