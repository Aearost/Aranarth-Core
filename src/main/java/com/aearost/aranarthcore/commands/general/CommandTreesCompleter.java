package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /trees command.
 */
public class CommandTreesCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (!sender.hasPermission("aranarth.trees.regen")) {
            return options;
        }
        if (args.length == 1) {
            options.add("regen");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("regen")) {
            for (World world : Bukkit.getWorlds()) {
                String name = world.getName();
                if (AranarthUtils.isSurvivalWorld(name) && !name.equals("spawn") && !name.equals("shops")
                        && world.getEnvironment() == World.Environment.NORMAL) {
                    options.add(name);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("regen")) {
            options.addAll(List.of("stop", "status", "near"));
        }
        String typed = args[args.length - 1].toLowerCase();
        options.removeIf(option -> !option.toLowerCase().startsWith(typed));
        return options;
    }
}
