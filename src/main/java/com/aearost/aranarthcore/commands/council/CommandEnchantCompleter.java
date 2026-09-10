package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles tab completion for the /enchant command.
 */
public class CommandEnchantCompleter implements TabCompleter {

    private static final List<String> ENCHANTMENT_NAMES = Arrays.stream(Enchantment.values())
            .map(e -> e.getKey().getKey())
            .sorted()
            .toList();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (AranarthUtils.getPlayer(player.getUniqueId()).getCouncilRank() != 3) {
            return List.of();
        }

        if (args.length == 1) {
            return AranarthUtils.getNetworkPlayerNames(args[0]);
        }

        if (args.length == 2) {
            String input = args[1].toLowerCase();
            return ENCHANTMENT_NAMES.stream()
                    .filter(name -> name.startsWith(input))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            Enchantment enchantment = Enchantment.getByName(args[1].toUpperCase());
            if (enchantment == null) {
                return List.of();
            }
            int vanillaMax = enchantment.getMaxLevel();
            List<String> levels = new ArrayList<>();
            levels.add(String.valueOf(vanillaMax));
            for (int preset : new int[]{10, 20, 50, 100}) {
                if (preset > vanillaMax) {
                    levels.add(String.valueOf(preset));
                }
            }
            String input = args[2];
            if (input.isEmpty()) {
                return levels;
            }
            return levels.stream()
                    .filter(l -> l.startsWith(input))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
