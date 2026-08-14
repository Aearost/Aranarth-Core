package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /homepad command.
 */
public class CommandHomePadCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = List.of("create", "delete", "reorder");

    /**
     * @param sender The user that entered the command.
     * @param command The command itself.
     * @param alias The alias of the command.
     * @param args The arguments of the command.
     * @return Confirmation of whether the command was a success or not.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUB_COMMANDS, args[0]);
        }

        int homeCount = AranarthUtils.getHomepads().size();

        return switch (args[0].toLowerCase()) {
            case "delete" -> {
                if (args.length == 2) {
                    yield indexList(homeCount, -1, args[1]);
                }
                yield List.of();
            }
            case "reorder" -> {
                if (args.length == 2) {
                    yield indexList(homeCount, -1, args[1]);
                }
                if (args.length == 3) {
                    int excluded = parseIndex(args[1]);
                    yield indexList(homeCount, excluded, args[2]);
                }
                yield List.of();
            }
            default -> List.of();
        };
    }

    private List<String> indexList(int count, int excluded, String input) {
        List<String> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (i == excluded) continue;
            String s = String.valueOf(i);
            if (input.isEmpty() || s.startsWith(input)) {
                results.add(s);
            }
        }
        return results;
    }

    private int parseIndex(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private List<String> filter(List<String> options, String input) {
        if (input.isEmpty()) return new ArrayList<>(options);
        List<String> results = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(input.toLowerCase())) {
                results.add(option);
            }
        }
        return results;
    }

}
