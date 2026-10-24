package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandTradeCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player self && p.equals(self)) {
                    continue;
                }
                if (p.getName().toLowerCase().startsWith(partial)) {
                    completions.add(p.getName());
                }
            }
            if (NetworkManager.isActive()) {
                for (NetworkPlayer np : NetworkManager.getInstance().getRemoteRoster().values()) {
                    String name = np.getUsername();
                    if (name.toLowerCase().startsWith(partial)) {
                        completions.add(name);
                    }
                    String nick = ChatUtils.stripColorFormatting(np.getNickname());
                    if (!nick.equalsIgnoreCase(name) && nick.toLowerCase().startsWith(partial)) {
                        completions.add(nick);
                    }
                }
            }
        }
        return completions;
    }
}
