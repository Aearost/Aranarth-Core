package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.LangManager;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows players to view and change their preferred display language.
 */
public class CommandLanguage implements CommandExecutor {

    /**
     * Maps a player-typed name (lowercase) to a canonical locale code.
     */
    private static String resolveLocale(String input) {
        String lower = input.toLowerCase();
        for (java.util.Map.Entry<String, String> entry : LangManager.LOCALE_DISPLAY_NAMES.entrySet()) {
            String code = entry.getKey();
            String displayName = entry.getValue();
            if (lower.equals(displayName.toLowerCase()) || lower.equals(code.toLowerCase())) {
                return code;
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());

        // /language - show invalid syntax with available options
        if (args.length == 0) {
            String list = String.join(" | ", LangManager.LOCALE_DISPLAY_NAMES.values());
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.invalid_syntax",
                    "usage", "language <" + list + ">")));
            return true;
        }

        // /language <name>
        String locale = resolveLocale(args[0]);
        if (locale == null) {
            String list = String.join(", ", LangManager.LOCALE_DISPLAY_NAMES.values());
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "language.invalid",
                    "name", args[0], "list", list)));
            return true;
        }

        ap.setLanguage(locale);
        AranarthUtils.setPlayer(player.getUniqueId(), ap);

        // language.name resolves in the newly set locale so the name is in that language
        String nativeName = LangManager.getInstance().get(locale, "language.name");
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "language.set",
                "language", nativeName)));

        // Immediately persist to MySQL so the preference survives cross-server transfers
        if (DatabaseManager.isActive()) {
            final String toggleJson = PersistenceUtils.buildPlayerToggleJson(player.getUniqueId());
            if (toggleJson != null) {
                Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                        DatabaseManager.getInstance().savePlayerToggles(player.getUniqueId(), toggleJson));
            }
        }

        return true;
    }
}
