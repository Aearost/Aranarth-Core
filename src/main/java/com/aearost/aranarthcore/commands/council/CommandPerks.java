package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Perk;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.HashMap;
import java.util.UUID;

/**
 * Applies or removes a perk that a player has.
 */
public class CommandPerks {

    /**
     * @param sender The user that entered the command.
     * @param args   The arguments of the command.
     */
    public static boolean onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("aranarth.perk.modify")) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
                return true;
            }
        }

        // Lists the player's perks
        if (args.length == 2) {
            UUID uuid = AranarthUtils.getUUIDFromUsername(args[1]);
            if (AranarthUtils.getPlayer(uuid) != null) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
                HashMap<Perk, Integer> perks = aranarthPlayer.getPerks();
                sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "perk.list_header", "player", aranarthPlayer.getNickname())));
                sender.sendMessage(ChatUtils.translateToColor("&6Compressor: &e" + perks.get(Perk.COMPRESSOR)));
                sender.sendMessage(ChatUtils.translateToColor("&6Randomizer: &e" + perks.get(Perk.RANDOMIZER)));
                sender.sendMessage(ChatUtils.translateToColor("&6Blacklist: &e" + perks.get(Perk.BLACKLIST)));
                sender.sendMessage(ChatUtils.translateToColor("&6Tables: &e" + perks.get(Perk.TABLES)));
                sender.sendMessage(ChatUtils.translateToColor("&6ItemName: &e" + perks.get(Perk.ITEMNAME)));
                sender.sendMessage(ChatUtils.translateToColor("&6Chat: &e" + perks.get(Perk.CHAT)));
                sender.sendMessage(ChatUtils.translateToColor("&6Shulker: &e" + perks.get(Perk.SHULKER)));
                sender.sendMessage(ChatUtils.translateToColor("&6Inventory: &e" + perks.get(Perk.INVENTORY)));
                sender.sendMessage(ChatUtils.translateToColor("&6Homes: &e" + perks.get(Perk.HOMES)));
                sender.sendMessage(ChatUtils.translateToColor("&6ItemFrame: &e" + perks.get(Perk.ITEMFRAME)));
                sender.sendMessage(ChatUtils.translateToColor("&6BlueFire: &e" + perks.get(Perk.BLUEFIRE)));
                sender.sendMessage(ChatUtils.translateToColor("&6WhiteFire: &e" + perks.get(Perk.WHITEFIRE)));
                sender.sendMessage(ChatUtils.translateToColor("&6PrismaticFire: &e" + perks.get(Perk.PRISMATICFIRE)));
                sender.sendMessage(ChatUtils.translateToColor("&6Discord: &e" + perks.get(Perk.DISCORD)));
                sender.sendMessage(ChatUtils.translateToColor("&6Nickname: &e" + perks.get(Perk.NICKNAME)));
                sender.sendMessage(ChatUtils.translateToColor("&6InvisibleArmor: &e" + perks.get(Perk.INVISIBLEARMOR)));
                return true;
            } else {
                sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.not_found", "name", args[1])));
                return true;
            }
        }

        boolean isHomesIncrement = args.length >= 3 && args[2].equals("homes") && (args.length == 3 || (args.length == 4 && args[3].equalsIgnoreCase("silent")));
        if (args.length < 4 || isHomesIncrement) {
            // To increase the amount of homes by 3
            if (args.length == 3 || isHomesIncrement) {
                if (args[2].equals("homes")) {
                    UUID uuid = AranarthUtils.getUUIDFromUsername(args[1]);
                    if (AranarthUtils.getPlayer(uuid) != null) {
                        if (isValidPerk(args[2].toLowerCase())) {
                            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
                            HashMap<Perk, Integer> perks = aranarthPlayer.getPerks();

                            if (perks.get(Perk.HOMES) <= 12) {
                                perks.put(Perk.HOMES, perks.get(Perk.HOMES) + 3);

                                aranarthPlayer.setPerks(perks);
                                AranarthUtils.setPlayer(uuid, aranarthPlayer);
                                if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
                                    Player player = Bukkit.getPlayer(uuid);
                                    PermissionUtils.evaluatePlayerPermissions(player);
                                }

                                boolean isSilent = args.length == 4 && args[3].equalsIgnoreCase("silent");
                                String message = Lang.getDefault("perk.purchased_homes", "player", aranarthPlayer.getNickname());
                                if (sender instanceof Player || isSilent || !AranarthCore.isSmpServer()) {
                                    Lang.broadcast("perk.purchased_homes", "player", aranarthPlayer.getNickname());
                                    if (!isSilent) {
                                        DiscordUtils.donationNotification(message, uuid, Color.CYAN);
                                    }
                                }

                                return true;
                            } else {
                                sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "perk.max_homes")));
                                return true;
                            }
                        } else {
                            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "perk.invalid")));
                            return true;
                        }
                    } else {
                        sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.not_found", "name", args[1])));
                        return true;
                    }
                }
            }
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "ac perks <player> <perk> <value>")));
            return true;
        } else {
            UUID uuid = AranarthUtils.getUUIDFromUsername(args[1]);
            if (AranarthUtils.getPlayer(uuid) != null) {
                if (isValidPerk(args[2].toLowerCase())) {
                    AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);

                    HashMap<Perk, Integer> perks = aranarthPlayer.getPerks();
                    if (args[2].equals("compressor") || args[2].equals("randomizer") || args[2].equals("blacklist")
                            || args[2].equals("tables") || args[2].equals("itemname") || args[2].equals("chat")
                            || args[2].equals("shulker") || args[2].equals("inventory") || args[2].equals("itemframe")
                            || args[2].equals("bluefire") || args[2].equals("whitefire")
                            || args[2].equals("prismaticfire") || args[2].equals("nickname")
                            || args[2].equals("invisiblearmor")) {
                        if (args[3].equals("0") || args[3].equals("1")) {
                            // Updates the perk value based on the input
                            perks.put(Perk.valueOf(args[2].toUpperCase()), Integer.parseInt(args[3]));

                            String perk = "";
                            switch (args[2]) {
                                case "compressor", "randomizer", "blacklist", "tables", "itemname", "chat", "shulker", "inventory",
                                     "itemframe", "bluefire", "whitefire", "prismaticfire", "nickname", "invisiblearmor" -> perk = Lang.key("perk.name_" + args[2]);
                            }

                            aranarthPlayer.setPerks(perks);
                            AranarthUtils.setPlayer(uuid, aranarthPlayer);

                            // Enables all materials to be compressed by default
                            if (args[2].equalsIgnoreCase("compressor") && args[3].equals("1")) {
                                AranarthUtils.compressAllMaterials(uuid);
                            }

                            if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
                                Player player = Bukkit.getPlayer(uuid);
                                PermissionUtils.evaluatePlayerPermissions(player);
                            }

                            boolean isSilent = args.length >= 5 && args[4].equalsIgnoreCase("silent");
                            if (!args[3].equals("0")) {
                                String message = Lang.getDefault("perk.purchased", "player", aranarthPlayer.getNickname(), "perk", perk);
                                if (sender instanceof Player || isSilent || !AranarthCore.isSmpServer()) {
                                    Lang.broadcast("perk.purchased", "player", aranarthPlayer.getNickname(), "perk", perk);
                                    if (!isSilent) {
                                        DiscordUtils.donationNotification(ChatUtils.stripColorFormatting(message), uuid, Color.CYAN);
                                    }
                                }
                            } else {
                                sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "perk.updated", "perk", perk, "player", aranarthPlayer.getNickname(), "value", args[3])));
                                // If the sender is either console or if the sender player is not the one the perk is being applied to
                                if (Bukkit.getOfflinePlayer(uuid).isOnline() &&
                                        (!(sender instanceof Player) || sender instanceof Player senderPlayer && !senderPlayer.getUniqueId().equals(uuid))) {
                                    Player player = Bukkit.getPlayer(uuid);
                                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "perk.removed", "perk", perk)));
                                }
                            }
                        } else {
                            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "perk.invalid_value")));
                        }
                        return true;
                    } else if (args[2].equals("homes")) {
                        if (args[3].equals("0") || args[3].equals("3") || args[3].equals("6") || args[3].equals("9")
                                || args[3].equals("12") || args[3].equals("15")) {
                            perks.put(Perk.HOMES, Integer.parseInt(args[3]));
                            aranarthPlayer.setPerks(perks);
                            AranarthUtils.setPlayer(uuid, aranarthPlayer);
                            if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
                                Player player = Bukkit.getPlayer(uuid);
                                PermissionUtils.evaluatePlayerPermissions(player);
                            }

                            boolean isSilent = args.length >= 5 && args[4].equalsIgnoreCase("silent");
                            if (!args[3].equals("0")) {
                                String message = Lang.getDefault("perk.purchased_homes", "player", aranarthPlayer.getNickname());
                                if (sender instanceof Player || isSilent || !AranarthCore.isSmpServer()) {
                                    Lang.broadcast("perk.purchased_homes", "player", aranarthPlayer.getNickname());
                                    if (!isSilent) {
                                        DiscordUtils.donationNotification(ChatUtils.stripColorFormatting(message), uuid, Color.CYAN);
                                    }
                                }
                            }
                        } else {
                            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "perk.invalid_value")));
                        }
                        return true;
                    } else if (args[2].equals("discord")) {
                        perks.put(Perk.DISCORD, Integer.parseInt(args[3]));
                        aranarthPlayer.setPerks(perks);
                        AranarthUtils.setPlayer(uuid, aranarthPlayer);

                        DiscordUtils.updateDiscordRole(Bukkit.getOfflinePlayer(uuid), aranarthPlayer);

                        boolean isSilent = args.length >= 5 && args[4].equalsIgnoreCase("silent");
                        if (!args[3].equals("0")) {
                            String message = Lang.getDefault("perk.purchased_discord", "player", aranarthPlayer.getNickname());
                            if (sender instanceof Player || isSilent || !AranarthCore.isSmpServer()) {
                                Lang.broadcast("perk.purchased_discord", "player", aranarthPlayer.getNickname());
                                if (!isSilent) {
                                    DiscordUtils.donationNotification(ChatUtils.stripColorFormatting(message), uuid, Color.CYAN);
                                }
                            }
                        }

                        return true;
                    } else {
                        sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "perk.error")));
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "perk.invalid")));
                    return true;
                }
            } else {
                sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.not_found", "name", args[1])));
                return true;
            }
        }
    }

    private static boolean isValidPerk(String perk) {
        return perk.equals("compressor") || perk.equals("randomizer") || perk.equals("blacklist") || perk.equals("tables")
                || perk.equals("itemname") || perk.equals("chat") || perk.equals("shulker") || perk.equals("inventory")
                || perk.equals("homes") || perk.equals("itemframe") || perk.equals("bluefire") || perk.equals("discord")
                || perk.equals("whitefire") || perk.equals("prismaticfire") || perk.equals("nickname")
                || perk.equals("invisiblearmor");
    }

}
