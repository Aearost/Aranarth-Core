package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Handles the auto complete functionality while using the /ac command.
 */
public class CommandACCompleter implements TabCompleter {

    private static final List<String> COUNCIL_OPTIONS = List.of(
            "admin", "ban", "broadcast", "clearchat", "dateset", "discordreload", "give",
            "home", "invsee", "invswap", "msg", "mute", "perks", "punishments", "questnpc", "rankset",
            "speed", "sudo", "time", "tp", "tpf", "tpw", "unban", "unmute",
            "vanish", "vpedit", "warn", "weather", "whereis", "skull"
    );

    private static final List<String> ITEM_NAMES;

    static {
        List<String> names = new ArrayList<>();
        String packagePath = "com/aearost/aranarthcore/items/";
        try {
            URL location = CommandACCompleter.class.getProtectionDomain().getCodeSource().getLocation();
            try (JarFile jar = new JarFile(new File(location.toURI()))) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String entryName = entries.nextElement().getName();
                    if (entryName.startsWith(packagePath) && entryName.endsWith(".class") && !entryName.contains("$")) {
                        String className = entryName.replace('/', '.').replace(".class", "");
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                                names.add(clazz.getSimpleName());
                            }
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        Collections.sort(names);
        ITEM_NAMES = Collections.unmodifiableList(names);
    }

    private static final List<String> PERK_OPTIONS = List.of(
            "blacklist", "bluefire", "chat", "compressor", "discord",
            "homes", "inventory", "itemframe", "itemname", "randomizer",
            "shulker", "tables"
    );

    /**
     * @param sender  The user that entered the command.
     * @param command The command itself.
     * @param alias   The alias of the command.
     * @param args    The arguments of the command.
     * @return Confirmation of whether the command was a success or not.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean isAuthorized = false;
        boolean isArchitectOnly = false;
        if (sender instanceof Player player) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            boolean isCouncil = aranarthPlayer.getCouncilRank() > 0;
            boolean isArchitect = aranarthPlayer.getArchitectRank() >= 1;
            isAuthorized = isCouncil || isArchitect;
            isArchitectOnly = !isCouncil && isArchitect;
        } else if (sender instanceof ConsoleCommandSender) {
            isAuthorized = true;
        }
        if (!isAuthorized) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(isArchitectOnly ? List.of("msg") : COUNCIL_OPTIONS, args[0]);
        }
        return councilArgs(sender, args);
    }

    /**
     * Provides completions for the second argument and beyond based on the sub-command.
     *
     * @param sender The user that entered the command.
     * @param args   The arguments of the command.
     * @return The list of completions to display.
     */
    private List<String> councilArgs(CommandSender sender, String[] args) {
        return switch (args[0].toLowerCase()) {
            case "whereis", "give", "mute", "unmute", "ban", "unban", "invsee", "warn", "punishments", "perks",
                 "sudo", "skull" -> {
                if (args.length == 2) {
                    yield filterPlayers(args[1]);
                }
                yield switch (args[0].toLowerCase()) {
                    case "give" -> args.length == 3 ? filter(ITEM_NAMES, args[2]) : List.of();
                    case "mute", "ban" -> {
                        if (args.length == 3) {
                            yield args[2].isEmpty() ? List.of("1m", "1h", "1d", "1w", "-1") : List.of();
                        }
                        if (args.length == 4) {
                            yield args[3].isEmpty() ? List.of("reason") : List.of();
                        }
                        yield List.of();
                    }
                    case "warn" -> args[2].isEmpty() ? List.of("reason") : List.of();
                    case "punishments" -> {
                        if (args.length == 3) {
                            yield List.of("remove");
                        }
                        if (args.length == 4) {
                            yield args[3].isEmpty() ? List.of("number") : List.of();
                        }
                        yield List.of();
                    }
                    case "perks" -> args.length == 3 ? filter(PERK_OPTIONS, args[2]) : List.of();
                    case "sudo" -> args[2].isEmpty() ? List.of("command") : List.of();
                    default -> List.of();
                };
            }
            case "avatar" -> filter(List.of("set"), args[1]);
            case "broadcast" -> args[1].isEmpty() ? List.of("msg") : List.of();
            case "boosts" -> {
                if (args.length == 2) {
                    yield filter(List.of("add", "remove"), args[1]);
                }
                if (args.length == 3) {
                    yield new ArrayList<>(List.of("CHI", "HARVEST", "HUNTER", "MINER"));
                }
                if (args.length == 4) {
                    yield filterPlayers(args[3]);
                }
                yield List.of();
            }
            case "home" -> {
                if (args.length == 2) {
                    yield filterPlayers(args[1]);
                }
                if (args.length >= 3) {
                    OfflinePlayer homeTarget = Bukkit.getOfflinePlayer(AranarthUtils.getUUIDFromUsername(args[1]));
                    if (homeTarget != null) {
                        AranarthPlayer homeTargetAranarthPlayer = AranarthUtils.getPlayer(homeTarget.getUniqueId());
                        if (homeTargetAranarthPlayer != null) {
                            String query = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                            yield homeTargetAranarthPlayer.getHomes().stream()
                                    .map(h -> ChatUtils.stripColorFormatting(h.getName()))
                                    .filter(name -> query.isEmpty() || name.toLowerCase().startsWith(query.toLowerCase()))
                                    .collect(Collectors.toList());
                        }
                    }
                }
                yield List.of();
            }
            case "invswap" -> {
                if (args.length == 2) {
                    yield filter(List.of("ARENA", "CREATIVE", "SURVIVAL"), args[1]);
                }
                if (args.length == 3) {
                    yield filterPlayers(args[2]);
                }
                yield List.of();
            }
            case "questnpc" -> args.length == 2 ? filter(List.of("spawn", "remove"), args[1]) : List.of();
            case "vote" -> args.length == 2 ? filter(List.of("test"), args[1]) : List.of();
            case "vpedit" -> {
                if (args.length == 2) {
                    yield filterPlayers(args[1]);
                }
                if (args.length == 3) {
                    yield args[2].isEmpty() ? List.of("+10", "-10") : List.of();
                }
                yield List.of();
            }

            case "msg" -> args[1].isEmpty() ? List.of("message") : List.of();
            case "speed" -> args.length == 2 ? filter(List.of("1", "10"), args[1]) : List.of();
            case "time" -> args.length == 2 ? filter(List.of("day", "midnight", "night", "noon"), args[1]) : List.of();
            case "dateset" ->
                    args.length == 2 ? filter(List.of("day", "month", "weekday", "year"), args[1]) : List.of();
            case "weather" -> {
                if (args.length == 2) {
                    yield filter(List.of("CLEAR", "DELAY", "DURATION", "RAIN", "THUNDER"), args[1]);
                }
                if (args.length == 3 && (args[1].equalsIgnoreCase("DURATION") || args[1].equalsIgnoreCase("DELAY"))) {
                    yield args[2].isEmpty() ? List.of("100", "1200", "6000") : List.of();
                }
                yield List.of();
            }
            case "tpw" -> {
                if (args.length == 2) {
                    yield Bukkit.getWorlds().stream()
                            .map(w -> w.getName())
                            .filter(name -> args[1].isEmpty() || name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                yield List.of();
            }
            case "tp", "tpf" -> {
                if (args.length == 2) {
                    yield List.of("username", "x");
                }
                if (args.length == 3) {
                    if (isOnlinePlayer(args[1])) {
                        List<String> options = new ArrayList<>();
                        if (args[0].equalsIgnoreCase("tp")) {
                            options.add("username");
                        }
                        options.add("x");
                        yield options;
                    }
                    yield List.of("y");
                }
                if (args.length == 4) {
                    if (isOnlinePlayer(args[1])) {
                        yield !isOnlinePlayer(args[2]) ? List.of("y") : List.of();
                    }
                    yield List.of("z");
                }
                if (args.length == 5) {
                    yield isOnlinePlayer(args[1]) ? List.of("z") : List.of("yaw");
                }
                if (args.length == 6) {
                    yield isOnlinePlayer(args[1]) ? List.of("yaw") : List.of("pitch");
                }
                if (args.length == 7) {
                    yield isOnlinePlayer(args[1]) ? List.of("pitch") : List.of();
                }
                yield List.of();
            }
            default -> List.of();
        };
    }

    private static boolean isOnlinePlayer(String name) {
        return Bukkit.getOnlinePlayers().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name));
    }

    private static List<String> filter(List<String> options, String input) {
        if (input.isEmpty()) {
            return new ArrayList<>(options);
        }
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private static List<String> filterPlayers(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
