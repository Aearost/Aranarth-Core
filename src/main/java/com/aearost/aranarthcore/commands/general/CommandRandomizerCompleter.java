package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.RandomizerPreset;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /randomizer command.
 */
public class CommandRandomizerCompleter implements TabCompleter {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			return List.of();
		}
		if (args.length == 1) {
			if (args[0].isEmpty()) {
				return List.of("preset", "#%material,#%material", "material,material");
			}
			if ("preset".startsWith(args[0].toLowerCase())) {
				return List.of("preset");
			}
			return List.of();
		}
		if (!args[0].equalsIgnoreCase("preset")) {
			return List.of();
		}
		if (args.length == 2) {
			return filter(List.of("bind", "create", "delete", "list"), args[1]);
		}
		String subcommand = args[1].toLowerCase();
		if (subcommand.equals("bind") || subcommand.equals("delete")) {
			return completeName(player, args);
		}
		if (subcommand.equals("create")) {
			if (args.length == 3) {
				List<String> names = new ArrayList<>(completeName(player, args));
				if (args[2].isEmpty()) {
					names.add(0, "<name>");
				}
				return names;
			}
			if (args[args.length - 1].isEmpty()) {
				return List.of("#%material,#%material", "material,material");
			}
		}
		return List.of();
	}

	/**
	 * Suggests the next word of the player's preset names, since a name can contain spaces.
	 */
	private List<String> completeName(Player player, String[] args) {
		int wordIndex = args.length - 3;
		String typedBefore = String.join(" ", Arrays.copyOfRange(args, 2, args.length - 1)).toLowerCase();
		String typedWord = args[args.length - 1];
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		List<String> words = new ArrayList<>();
		for (RandomizerPreset preset : aranarthPlayer.getRandomizerPresets()) {
			String[] nameWords = ChatUtils.stripColorFormatting(preset.getName()).trim().split(" +");
			if (wordIndex >= nameWords.length) {
				continue;
			}
			String nameBefore = String.join(" ", Arrays.copyOfRange(nameWords, 0, wordIndex)).toLowerCase();
			if (nameBefore.equals(typedBefore) && !words.contains(nameWords[wordIndex])) {
				words.add(nameWords[wordIndex]);
			}
		}
		return filter(words, typedWord);
	}

	private List<String> filter(List<String> options, String typed) {
		List<String> results = new ArrayList<>();
		for (String option : options) {
			if (option.toLowerCase().startsWith(typed.toLowerCase())) {
				results.add(option);
			}
		}
		return results;
	}
}
