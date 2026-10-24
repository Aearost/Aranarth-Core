package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.Mount;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.MountUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

/**
 * Allows the player to summon and dismiss their mounts, as well as rename them and view their skills.
 */
public class CommandMount implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
            return true;
        }

        if (args.length == 0) {
            MountUtils.toggleMount(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "skills" -> {
                String element = args.length >= 2 ? args[1].toUpperCase() : null;
                showSkills(player, element);
            }
            case "info" -> sendInfo(player);
            case "nickname" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.invalid_syntax", "usage", "mount nickname <name|remove>")));
                    return true;
                }
                if (args[1].equalsIgnoreCase("remove")) {
                    removeNickname(player);
                } else {
                    String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    setNickname(player, name);
                }
            }
            default -> player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.invalid_syntax", "usage", "mount [info|skills|nickname]")));
        }
        return true;
    }

    private void sendInfo(Player player) {
        String element = MountUtils.getElementForPlayer(player);
        if (element == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "bending.need_element_use")));
            return;
        }

        Mount mount = MountUtils.getOrCreate(player.getUniqueId(), element);
        String color = MountUtils.getElementColor(element);
        String displayName = MountUtils.getDisplayName(player.getUniqueId(), element);
        String thirdLabel = MountUtils.getThirdAttrLabel(player, element);

        String healthFoodHint = Lang.getFor(player, switch (element) {
            case "AIR" -> "mount.food_air";
            case "FIRE" -> "mount.food_fire";
            case "WATER" -> "mount.food_water";
            default -> "mount.food_earth";
        });
        String thirdHowTo = Lang.getFor(player, switch (element) {
            case "EARTH" -> "mount.howto_earth";
            case "FIRE" -> "mount.howto_fire";
            case "WATER" -> "mount.howto_water";
            case "AIR" -> "mount.howto_air";
            default -> "mount.howto_none";
        });

        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.info_header", "mount", displayName)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.info_skill", "skill", Lang.getFor(player, "mount.skill_health"), "progress", skillProgress(mount.getHealthLevel(), mount.getHealthXp(), mount.xpNeededForNextHealthLevel()))));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.info_health_hint", "food", healthFoodHint)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.info_skill", "skill", Lang.getFor(player, "mount.skill_speed"), "progress", skillProgress(mount.getSpeedLevel(), mount.getSpeedXp(), mount.xpNeededForNextSpeedLevel()))));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.info_speed_hint")));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.info_skill", "skill", thirdLabel, "progress", skillProgress(mount.getThirdLevel(), mount.getThirdXp(), mount.xpNeededForNextThirdLevel()))));
        player.sendMessage(ChatUtils.translateToColor("  &7&o" + thirdHowTo));
    }

    private String skillProgress(int level, long xp, long needed) {
        return level >= Mount.MAX_LEVEL
                ? level + " &a(MAX)"
                : level + " &8(&e" + xp + "&8/&e" + needed + " XP&8)";
    }

    private void showSkills(Player player, String requestedElement) {
        String element;
        if (requestedElement != null) {
            element = requestedElement;
            if (!element.equals("AIR") && !element.equals("WATER")
                    && !element.equals("EARTH") && !element.equals("FIRE")) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "mount.no_chi_mount")));
                return;
            }
        } else {
            element = MountUtils.getElementForPlayer(player);
            if (element == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "bending.need_element")));
                return;
            }
        }

        MountUtils.showAllSkillBars(player, element);
    }

    private void setNickname(Player player, String name) {
        if (!ChatUtils.isSafeStoredText(name)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "nickname.invalid_characters")));
            return;
        }
        if (ChatUtils.stripColorFormatting(name).length() > 32) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "mount.nickname_too_long")));
            return;
        }
        String element = MountUtils.getElementForPlayer(player);
        if (element == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "bending.need_element")));
            return;
        }
        Mount mount = MountUtils.getOrCreate(player.getUniqueId(), element);
        mount.setNickname(name);
        PersistenceUtils.saveMountsForPlayer(player.getUniqueId());
        updateMountNametag(player.getUniqueId(), name);

        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "mount.nicknamed", "mount", MountUtils.getElementColor(element) + MountUtils.getMountNameForElement(element), "name", name)));
    }

    private void removeNickname(Player player) {
        String element = MountUtils.getElementForPlayer(player);
        if (element == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "bending.need_element")));
            return;
        }
        Mount mount = MountUtils.getOrCreate(player.getUniqueId(), element);
        mount.setNickname(null);
        PersistenceUtils.saveMountsForPlayer(player.getUniqueId());
        updateMountNametag(player.getUniqueId(), MountUtils.getMountNameForElement(element));

        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "mount.nickname_removed", "mount", MountUtils.getElementColor(element) + MountUtils.getMountNameForElement(element))));
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
