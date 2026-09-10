package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.enums.WorldEvent;
import java.util.Random;
import com.aearost.aranarthcore.event.world.WorldEventManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Manually starts a world event.
 */
public class CommandWorldEvent {

    /**
     * @param sender The user that entered the command.
     * @param args   The arguments of the command.
     */
    public static boolean onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            if (aranarthPlayer.getCouncilRank() < 3) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
                return true;
            }
        }

        if (args.length < 2) {
            sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac worldevent <name> [intensity]"));
            return true;
        }

        WorldEvent event;
        try {
            event = WorldEvent.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatUtils.chatMessage("&cUnknown world event: &e" + args[1]));
            return true;
        }

        // Parse optional intensity: user inputs 1/2/3, internally 0/1/2
        int intensity;
        if (args.length >= 3) {
            try {
                int userIntensity = Integer.parseInt(args[2]);
                if (userIntensity < 1 || userIntensity > 3) {
                    sender.sendMessage(ChatUtils.chatMessage("&cIntensity must be &e1&c, &e2&c, or &e3&c."));
                    return true;
                }
                intensity = userIntensity - 1;
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatUtils.chatMessage("&cIntensity must be &e1&c, &e2&c, or &e3&c."));
                return true;
            }
        } else {
            intensity = new Random().nextInt(3);
        }

        WorldEvent active = AranarthUtils.getActiveWorldEvent();

        if (active == event) {
            sender.sendMessage(ChatUtils.chatMessage(event.getColor() + event.getName(AranarthUtils.getActiveWorldEventIntensity()) + " &7is already active!"));
            return true;
        }

        // End any currently active event before starting the new one
        if (active != null) {
            WorldEventManager.getInstance().endEvent();
        }

        WorldEventManager.getInstance().startEvent(event, intensity);
        return true;
    }
}
