package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;

/**
 * Displays the rules of Aranarth.
 */
public class CommandRules {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		sender.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lAranarth Rules &8- - -"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&61&8] &7Be kind and respectful to everyone, regardless of race, orientation, gender, religion, etc."));
		sender.sendMessage(ChatUtils.translateToColor("&8[&62&8] &7Do not spam chat, use caps lock, etc."));
		sender.sendMessage(ChatUtils.translateToColor("&8[&63&8] &7No inappropriate conversation topics or excessive swearing"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&64&8] &7No inappropriate skins, builds, item names or nicknames"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&65&8] &7No advertising or mentioning other servers"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&66&8] &7No glitch abuse, hacking, auto-clickers, lag machines, or any sort of abuse of the game"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&67&8] &7Ask Staff (the Council) before using mods - only client-side cosmetic mods are permitted (OptiFine, Shaders, MiniHUD, etc.)"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&68&8] &7Do not impersonate or disrespect Staff (the Council)"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&69&8] &7Only the use of manually harvested crop farms or spawner farms are permitted"));
		sender.sendMessage(ChatUtils.translateToColor("&8[&610&8] &7Do not grief the world's terrain unnecessarily - use the resource world instead"));
		return true;
	}

}
