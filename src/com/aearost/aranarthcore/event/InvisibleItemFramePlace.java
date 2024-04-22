package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.ItemFrameUpdateRunnable;
import com.aearost.aranarthcore.items.InvisibleItemFrame;

/**
 * Inspired by tiffany352
 * Source: https://github.com/tiffany352/InvisibleItemFrames/blob/main/src/main/java/com/tiffnix/invisibleitemframes/PluginListener.java
 */
public class InvisibleItemFramePlace implements Listener {
	
	private Location locationToPlace = null;
	private BlockFace faceToPlace = null;

	public InvisibleItemFramePlace(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
     * Handles placing an Item Frame and determining if it is invisible or not.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack is = event.getItem();
        if (is != null) {
        	if (is.getType() == Material.ITEM_FRAME) {
            	if (is.hasItemMeta()) {
            		if (is.getItemMeta().hasLore()) {
            	        Block block = event.getClickedBlock();
            	        if (block != null) {
            	        	locationToPlace = block.getLocation();
            	        	faceToPlace = event.getBlockFace();
            	        }
            		}
            	}
            }
        }
    }

	/**
	 * Sets the placed item frame to be invisible if it is an Invisible Item Frame.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onItemFramePlace(final HangingPlaceEvent e) {
		if (e.getEntity().getType() == EntityType.ITEM_FRAME) {
			Location placedLocation = e.getBlock().getLocation();
			BlockFace placedFace = e.getBlockFace();
			
			if (placedLocation.equals(this.locationToPlace) && placedFace == this.faceToPlace) {
				this.locationToPlace = null;
				this.faceToPlace = null;
				e.getEntity().getPersistentDataContainer().set(InvisibleItemFrame.IS_INVISIBLE, PersistentDataType.BYTE, (byte) 1);
			}
		}
	}
	
	/**
     * Updates the visibility of an item frame.
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (InvisibleItemFrame.isInvisibleItemFrame(entity)) {
        	ItemFrame itemFrame = (ItemFrame) entity;

            new ItemFrameUpdateRunnable(itemFrame).runTask(InvisibleItemFrame.PLUGIN);
        }
    }
	
	
}
