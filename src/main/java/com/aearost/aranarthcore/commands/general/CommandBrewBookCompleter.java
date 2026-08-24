package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.BrewRecipeUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tab completer for /brewbook. Only offers completions to council rank 3+ players.
 */
public class CommandBrewBookCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player) {
            AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
            if (ap == null || ap.getCouncilRank() < 3) return List.of();
        }

        if (args.length == 1) {
            return filter(List.of("unlock"), args[0]);
        }

        if (!args[0].equalsIgnoreCase("unlock")) return List.of();

        if (args.length == 2) {
            return AranarthUtils.getNetworkPlayerNames(args[1]);
        }

        // args.length >= 3: complete brew names not yet unlocked by the target player.
        // Brew names can be multi-word (e.g. "Banana Daiquiri"). The full query is args[2..end] joined
        // by spaces. The client replaces the last space-delimited token, so we must return only the
        // portion of the brew name starting at the last confirmed space boundary.
        UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
        String query = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        // Prefix = all brew-name args except the last one being typed (already confirmed portion).
        String prefix = args.length > 3
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length - 1)) + " "
                : "";

        List<String> result = new ArrayList<>();
        for (BrewRecipe recipe : BrewRecipe.values()) {
            String displayName = recipe.getDisplayName();
            if (displayName == null || displayName.isEmpty()) continue;
            if (targetUuid != null && BrewRecipeUtils.isUnlocked(targetUuid, recipe)) continue;
            if (!query.isEmpty() && !displayName.toLowerCase().startsWith(query.toLowerCase())) continue;
            // Return the remaining portion of the name so the client replaces only the last token.
            result.add(displayName.substring(prefix.length()));
        }
        return result;
    }

    private static List<String> filter(List<String> options, String input) {
        if (input.isEmpty()) return new ArrayList<>(options);
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
