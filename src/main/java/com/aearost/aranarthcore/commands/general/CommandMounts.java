package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.Mount;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.MountUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /mounts — shows a summary of all four elemental mounts for the player.
 */
public class CommandMounts implements CommandExecutor {

    private static final String[] ELEMENTS = {"AIR", "WATER", "EARTH", "FIRE"};

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage("&cOnly players can use this command."));
            return true;
        }
        showOverview(player);
        return true;
    }

    private void showOverview(Player player) {
        player.sendMessage(ChatUtils.chatMessage("&8--- &6&lElemental Mounts &8---"));

        UUID activeId = MountUtils.getActiveMountEntityUUID(player.getUniqueId());
        String[] activeInfo = activeId != null ? MountUtils.getActiveMountInfo(activeId) : null;

        for (String element : ELEMENTS) {
            String color = MountUtils.getElementColor(element);
            // Use the player's display name (nickname if set, otherwise species name)
            String displayName = MountUtils.getDisplayName(player.getUniqueId(), element);
            Mount mount = MountUtils.getOrCreate(player.getUniqueId(), element);

            boolean isThisActive = activeInfo != null && activeInfo[1].equals(element);

            String status;
            if (isThisActive) {
                Entity entity = Bukkit.getEntity(activeId);
                if (entity instanceof LivingEntity le) {
                    var maxHpAttr = le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    double maxHp = maxHpAttr != null ? maxHpAttr.getValue() : le.getHealth();
                    status = "§aActive §8(§a" + String.format("%.0f", le.getHealth())
                            + "§7/§a" + String.format("%.0f", maxHp) + " HP§8)";
                } else {
                    status = "§aActive";
                }
            } else if (mount.isRecharging()) {
                status = "§cRecovering §8(&c"
                        + MountUtils.formatRechargeTime(mount.getRechargeRemainingSeconds()) + "§8)";
            } else if (MountUtils.getEntityClassForElement(element) != null) {
                status = "§eReady";
            } else {
                status = "§7Coming Soon";
            }

            String levels = "HP: " + mount.getHealthLevel()
                    + " | SPD: " + mount.getSpeedLevel()
                    + " | " + thirdShortLabel(element) + ": " + mount.getThirdLevel();

            player.sendMessage(ChatUtils.chatMessage(
                    color + displayName + " &8— " + status + " &8(" + color + levels + "&8)"));
        }

        player.sendMessage(ChatUtils.chatMessage("&7Use &e/mount &7to call or dismiss your current mount"));
    }

    private static String thirdShortLabel(String element) {
        return switch (element) {
            case "EARTH" -> "DIG";
            case "FIRE" -> "RAM";
            case "WATER" -> "BITE";
            case "AIR" -> "GUST";
            default -> "SPC";
        };
    }
}
