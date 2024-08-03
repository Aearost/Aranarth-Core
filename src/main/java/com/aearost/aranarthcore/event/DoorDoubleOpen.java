package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Door.Hinge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.aearost.aranarthcore.AranarthCore;

import java.util.Objects;

public class DoorDoubleOpen implements Listener {

	public DoorDoubleOpen(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Results in both doors opening at the same time if they are side by side.
	 * @param e The event.
	 */
	@EventHandler
	public void onDoorOpen(final PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Block block = e.getClickedBlock();
			if (Objects.requireNonNull(block).getBlockData() instanceof Door) {
				
				Door door = (Door) block.getBlockData();
				Block sideBlock = null;
				if (door.getFacing() == BlockFace.NORTH || door.getFacing() == BlockFace.SOUTH) {
					if (door.getHinge() == Hinge.LEFT) {
						if (door.getFacing() == BlockFace.NORTH) {
							sideBlock = block.getWorld().getBlockAt(block.getX() + 1, block.getY(), block.getZ());
						} else {
							sideBlock = block.getWorld().getBlockAt(block.getX() - 1, block.getY(), block.getZ());
						}
					} else {
						if (door.getFacing() == BlockFace.NORTH) {
							sideBlock = block.getWorld().getBlockAt(block.getX() - 1, block.getY(), block.getZ());
						} else {
							sideBlock = block.getWorld().getBlockAt(block.getX() + 1, block.getY(), block.getZ());
						}
					}
				} else {
					if (door.getHinge() == Hinge.LEFT) {
						if (door.getFacing() == BlockFace.EAST) {
							sideBlock = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() + 1);
						} else {
							sideBlock = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() - 1);
						}
					} else {
						if (door.getFacing() == BlockFace.EAST) {
							sideBlock = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() - 1);
						} else {
							sideBlock = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() + 1);
						}
					}
				}
				
				if (sideBlock.getBlockData() instanceof Door) {
					Door sideDoor = (Door) sideBlock.getBlockData();
					sideDoor.setOpen(!sideDoor.isOpen());
					sideBlock.setBlockData(sideDoor, true);
				}
			}
		}
	}
}
