package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Overrides portal teleport behaviour between survival worlds.
 */
public class PortalEventListener implements Listener {

	public PortalEventListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onPlayerPortal(PlayerPortalEvent e) {
		Location destination = determinePortalDestinationLocation(e.getFrom(), e.getPlayer(), e.getCause(), e);
		if (destination != null) {
			e.setTo(destination);
		}
	}

	@EventHandler
	public void onEntityPortal(EntityPortalEvent e) {
		// No cause for entities, must get it from the environment
		Entity entity = e.getEntity();
		World fromWorld = e.getFrom().getWorld();
		World toWorld = e.getTo().getWorld();
		TeleportCause cause = determinePortalType(fromWorld, toWorld);
		Location destination = determinePortalDestinationLocation(e.getFrom(), entity, cause, e);
		if (destination != null) {
			e.setTo(destination);
		}
	}

	/**
	 * Determines the type of portal that was used.
	 * @param fromWorld The world that the portal was used in.
	 * @param toWorld The world that the portal is going to.
	 * @return The cause of the teleportation.
	 */
	private TeleportCause determinePortalType(World fromWorld, World toWorld) {
		Environment environment = fromWorld.getEnvironment();
		if (fromWorld.getEnvironment() == Environment.NORMAL) {
			if (toWorld.getEnvironment() == Environment.NETHER) {
				return TeleportCause.NETHER_PORTAL;
			} else if (toWorld.getEnvironment() == Environment.THE_END) {
				return TeleportCause.END_PORTAL;
			}
		} else if (fromWorld.getEnvironment() == Environment.NETHER) {
			if (toWorld.getEnvironment() == Environment.NORMAL) {
				return TeleportCause.NETHER_PORTAL;
			}
		} else if (fromWorld.getEnvironment() == Environment.THE_END) {
			if (toWorld.getEnvironment() == Environment.NORMAL) {
				return TeleportCause.END_PORTAL;
			}
		}
		return TeleportCause.UNKNOWN;
	}

	/**
	 * Addresses updating the destination world/location of a portal
	 * @param from The location the player is teleporting from.
	 * @param entity The player or entity that is being teleported.
	 * @param cause The portal type that was used.
	 * @param rawEvent The base event
	 */
	private Location determinePortalDestinationLocation(Location from, Entity entity, TeleportCause cause, Event rawEvent) {
		World fromWorld = from.getWorld();
		if (fromWorld == null) {
			return null;
		}

		String fromName = fromWorld.getName();
		String baseName = fromName.replace("_nether", "").replace("_the_end", "");

		World targetWorld = null;
		Location to = from.clone();

		switch (cause) {
			case NETHER_PORTAL -> {
				// Overworld to Nether
				if (fromWorld.getEnvironment() == Environment.NORMAL) {
					targetWorld = Bukkit.getWorld(baseName + "_nether");
					// If the world cannot be found, stop logic
					if (targetWorld == null) {
						Bukkit.getLogger().info("The target nether world " + baseName + "_nether could not be found");
						return null;
					}

					to.setWorld(targetWorld);
					to.setX(to.getX() / 8.0);
					to.setZ(to.getZ() / 8.0);
				}
				// Nether to Overworld
				else if (fromWorld.getEnvironment() == Environment.NETHER) {
					targetWorld = Bukkit.getWorld(baseName);
					// If the world cannot be found, stop logic
					if (targetWorld == null) {
						Bukkit.getLogger().info("The target overworld " + baseName + " could not be found");
						return null;
					}

					to.setWorld(targetWorld);
					to.setX(to.getX() * 8.0);
					to.setZ(to.getZ() * 8.0);
				}
			}
			case END_PORTAL -> {
				// Overworld to End
				if (fromWorld.getEnvironment() == Environment.NORMAL) {
					targetWorld = Bukkit.getWorld(baseName + "_the_end");
					// If the world cannot be found, stop logic
					if (targetWorld == null) {
						Bukkit.getLogger().info("The target end " + baseName + " could not be found");
						return null;
					}
				}
				// End to Overworld
				else if (fromWorld.getEnvironment() == Environment.THE_END) {
					targetWorld = Bukkit.getWorld(baseName);
					// If the world cannot be found, stop logic
					if (targetWorld == null) {
						Bukkit.getLogger().info("The target overworld " + baseName + " could not be found");
						return null;
					}
				}

				// Consistently at this location
				to.setWorld(targetWorld);
				to.setX(100.5);
				to.setY(49.1);
				to.setZ(0.5);
				to.setYaw(90);
				to.setPitch(0);
			}
		}

		return to;
	}
	
}
