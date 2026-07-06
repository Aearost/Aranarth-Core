package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Allows for the player to lock the next clicked container.
 */
public class CommandLock implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.isLockingContainer()) {
				aranarthPlayer.setLockingContainer(false);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				sender.sendMessage(ChatUtils.chatMessage("&7You are no longer locking containers"));
			} else {
				aranarthPlayer.setTrustedPlayerUUID(null);
				aranarthPlayer.setUntrustedPlayerUUID(null);
				aranarthPlayer.setUnlockingContainer(false);
				aranarthPlayer.setLockingContainer(true);
				aranarthPlayer.setContainerToggleExpiry(System.currentTimeMillis() + 5000);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				sender.sendMessage(ChatUtils.chatMessage("&7You are now locking containers - right-click to lock them"));
				sender.sendMessage(ChatUtils.chatMessage("&7Run &e/lock &7again to exit locking mode"));
				scheduleToggleExpiry(player.getUniqueId());
			}
        } else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis command can only be executed in-game!"));
        }
        return true;
    }

    public static void scheduleToggleExpiry(UUID uuid) {
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
            if (ap == null) return;
            long remaining = ap.getContainerToggleExpiry() - System.currentTimeMillis();
            if (remaining > 0) {
                scheduleToggleExpiry(uuid);
                return;
            }
            boolean wasActive = ap.isLockingContainer() || ap.isUnlockingContainer()
                    || ap.getTrustedPlayerUUID() != null || ap.getUntrustedPlayerUUID() != null;
            if (!wasActive) return;
            ap.setLockingContainer(false);
            ap.setUnlockingContainer(false);
            ap.setTrustedPlayerUUID(null);
            ap.setUntrustedPlayerUUID(null);
            AranarthUtils.setPlayer(uuid, ap);
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                online.sendMessage(ChatUtils.chatMessage("&7Your container toggle has been automatically disabled"));
            }
        }, 100L);
    }

}
