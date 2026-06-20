package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.MailUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the auto complete functionality while using the /mail command.
 */
public class CommandMailCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("clear", "clearall", "read", "send");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && sender instanceof Player player) {
            String input = args[1];
            String sub = args[0].toLowerCase();

            if (sub.equals("read")) {
                if (!input.isEmpty() && !input.chars().allMatch(Character::isDigit)) {
                    return List.of();
                }
                int mailCount = MailUtils.getMail(player.getUniqueId()).size();
                if (mailCount == 0) {
                    return List.of();
                }
                int totalPages = (int) Math.ceil(mailCount / (double) 5);
                List<String> pages = new ArrayList<>();
                for (int i = 1; i <= totalPages; i++) {
                    pages.add(String.valueOf(i));
                }
                return input.isEmpty() ? pages : pages.stream()
                        .filter(p -> p.startsWith(input))
                        .collect(Collectors.toList());
            }

            if (sub.equals("clear")) {
                if (!input.isEmpty() && !input.chars().allMatch(Character::isDigit)) {
                    return List.of();
                }
                int mailCount = MailUtils.getMail(player.getUniqueId()).size();
                if (mailCount == 0) {
                    return List.of();
                }
                List<String> numbers = new ArrayList<>();
                for (int i = 1; i <= mailCount; i++) {
                    numbers.add(String.valueOf(i));
                }
                return input.isEmpty() ? numbers : numbers.stream()
                        .filter(n -> n.startsWith(input))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String input) {
        if (input.isEmpty()) {
            return new ArrayList<>(options);
        }
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
