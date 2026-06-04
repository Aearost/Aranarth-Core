package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.Mount;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.MountUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Allows the player to summon and dismiss their mounts, as well as rename them and view their skills.
 */
public class CommandMount implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
            return true;
        }

        if (args.length == 0) {
            MountUtils.toggleMount(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "skills", "info" -> {
                String element = args.length >= 2 ? args[1].toUpperCase() : null;
                showSkills(player, element);
            }
            case "nickname" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatUtils.chatMessage(
                            "&7Usage: &f/mount nickname <name|remove>"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("remove")) {
                    removeNickname(player);
                } else {
                    String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                    setNickname(player, name);
                }
            }
            default -> player.sendMessage(ChatUtils.chatMessage(
                    "&cInvalid syntax: &e/mount [skills|remove]"));
        }
        return true;
    }

    private void showSkills(Player player, String requestedElement) {
        String element;
        if (requestedElement != null) {
            element = requestedElement;
            if (!element.equals("AIR") && !element.equals("WATER")
                    && !element.equals("EARTH") && !element.equals("FIRE")) {
                player.sendMessage(ChatUtils.chatMessage(
                        "&6Chiblockers currently do not have a mount"));
                return;
            }
        } else {
            element = MountUtils.getElementForPlayer(player);
            if (element == null) {
                player.sendMessage(ChatUtils.chatMessage("&cYou must have an element in order to use this!"));
                return;
            }
        }

        MountUtils.showAllSkillBars(player, element);
    }

    private void setNickname(Player player, String name) {
        if (ChatUtils.stripColorFormatting(name).length() > 32) {
            player.sendMessage(ChatUtils.chatMessage("&cNicknames cannot exceed 32 characters"));
            return;
        }
        String element = MountUtils.getElementForPlayer(player);
        if (element == null) {
            player.sendMessage(ChatUtils.chatMessage("&cYou must have an element in order to do this!"));
            return;
        }
        Mount mount = MountUtils.getOrCreate(player.getUniqueId(), element);
        mount.setNickname(name);
        updateMountNametag(player.getUniqueId(), name);

        player.sendMessage(ChatUtils.chatMessage(
                MountUtils.getElementColor(element)
                        + MountUtils.getMountNameForElement(element)
                        + " &7has been nicknamed &e" + name));
    }

    private void removeNickname(Player player) {
        String element = MountUtils.getElementForPlayer(player);
        if (element == null) {
            player.sendMessage(ChatUtils.chatMessage("&cYou must have an element in order to do this!"));
            return;
        }
        Mount mount = MountUtils.getOrCreate(player.getUniqueId(), element);
        mount.setNickname(null);
        updateMountNametag(player.getUniqueId(), MountUtils.getMountNameForElement(element));

        player.sendMessage(ChatUtils.chatMessage(MountUtils.getElementColor(element)
                + "Your " + MountUtils.getMountNameForElement(element) + "'s nickname has been removed"));
    }

    private void updateMountNametag(UUID playerUUID, String name) {
        UUID mountId = MountUtils.getActiveMountEntityUUID(playerUUID);
        if (mountId == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(mountId);
        if (entity != null) {
            entity.setCustomName(name);
            entity.setCustomNameVisible(true);
        }
    }
}
