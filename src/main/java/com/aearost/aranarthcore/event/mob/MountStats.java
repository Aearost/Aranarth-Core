package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Displays the mount's stats when shift right-clicked.
 */
public class MountStats {
	public void execute(PlayerInteractEntityEvent e) {
		Player player = e.getPlayer();

		if (player.isSneaking()) {
			if (e.getRightClicked() instanceof AbstractHorse mount) {
				if (player.getInventory().getItemInMainHand().getType() != Material.GOAT_HORN) {
					// Prevent opening inventory / mounting
					e.setCancelled(true);

					// Get stats
					double jumpBlocks = getJumpHeight(mount);
					double speed = getSpeedMetersPerSecond(mount);
					double health = mount.getAttribute(Attribute.MAX_HEALTH).getValue();

					// Convert health to half-hearts (1 heart = 2 HP)
					int halfHearts = (int) Math.round(health);

					String mountName = mount.getName();
					String ownerName = mount.getOwner() != null ? AranarthUtils.getPlayer(mount.getOwner().getUniqueId()).getNickname() : "None";
					player.sendMessage(ChatUtils.translateToColor("&8      - - - &e" + mountName + "&e's Stats &8- - -"));
					player.sendMessage(ChatUtils.translateToColor("&7Owner: &e" + ownerName));
					player.sendMessage(ChatUtils.translateToColor("&7Health: &e" + halfHearts));
					player.sendMessage(ChatUtils.translateToColor("&7Jump: &e" + String.format("%.2f blocks", jumpBlocks)));
					player.sendMessage(ChatUtils.translateToColor("&7Speed: &e" + String.format("%.2f m/s", speed)));
				}
			}
		}
	}

	/**
	 * Provides the translated jump height of a mount.
	 * @param mount The mount.
	 * @return The translated jump height of a mount.
	 */
	private double getJumpHeight(AbstractHorse mount) {
		double strength = mount.getJumpStrength();

		// Vanilla-derived approximation
		return -0.1817584952 * Math.pow(strength, 3)
				+ 3.689713992 * Math.pow(strength, 2)
				+ 2.128599134 * strength
				- 0.343930367;
	}

	/**
	 * Provides the translated speed of a mount.
	 * @param mount The mount.
	 * @return The translated speed of a mount.
	 */
	private double getSpeedMetersPerSecond(AbstractHorse mount) {
		double speed = mount.getAttribute(Attribute.MOVEMENT_SPEED).getValue();

		// Convert to blocks/sec (≈ meters/sec)
		return speed * 43.17;
	}
}
