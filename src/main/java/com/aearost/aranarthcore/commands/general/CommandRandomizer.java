package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiRandomizer;
import com.aearost.aranarthcore.objects.RandomItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.RandomizerUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Allows players to input a pattern and randomize their block placements, and to save patterns as presets.
 */
public class CommandRandomizer implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
			return true;
		}
		if (!player.hasPermission("aranarth.randomizer")) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
			return true;
		}

		// /randomizer opens the GUI
		if (args.length == 0) {
			new GuiRandomizer(player).openGui();
			return true;
		}

		if (args[0].equalsIgnoreCase("preset")) {
			handlePreset(player, args);
			return true;
		}

		// /randomizer <pattern> sets the active pattern
		List<RandomItem> randomItems = RandomizerUtils.parsePattern(player, RandomizerUtils.joinPattern(args, 0));
		if (randomItems != null) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.pattern_updated")));
			RandomizerUtils.applyPattern(player, randomItems);
		}
		return true;
	}

	/**
	 * Handles /randomizer preset <create|bind|delete|list>.
	 */
	private void handlePreset(Player player, String[] args) {
		String usage = "randomizer preset <create|bind|delete|list>";
		if (args.length < 2) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.invalid_syntax", "usage", usage)));
			return;
		}
		switch (args[1].toLowerCase()) {
			case "create" -> {
				if (args.length < 4) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.invalid_syntax",
							"usage", "randomizer preset create <name> <pattern>")));
					return;
				}
				// The name can have spaces, so the pattern starts at the first argument with a comma,
				// or at the last argument if none has one
				int patternStart = args.length - 1;
				for (int i = 3; i < args.length; i++) {
					if (args[i].contains(",")) {
						// A pattern typed as "stone ,andesite" starts on the argument before the comma
						patternStart = args[i].startsWith(",") ? i - 1 : i;
						break;
					}
				}
				patternStart = Math.max(patternStart, 3);
				String name = String.join(" ", Arrays.copyOfRange(args, 2, patternStart));
				List<RandomItem> randomItems = RandomizerUtils.parsePattern(player, RandomizerUtils.joinPattern(args, patternStart));
				if (randomItems != null) {
					RandomizerUtils.savePreset(player, name, randomItems);
				}
			}
			case "bind" -> {
				if (args.length < 3) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.invalid_syntax",
							"usage", "randomizer preset bind <name>")));
					return;
				}
				RandomizerUtils.bindPreset(player, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
			}
			case "delete" -> {
				if (args.length < 3) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.invalid_syntax",
							"usage", "randomizer preset delete <name>")));
					return;
				}
				RandomizerUtils.deletePreset(player, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
			}
			case "list" -> RandomizerUtils.listPresets(player);
			default -> player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.invalid_syntax", "usage", usage)));
		}
	}

}
