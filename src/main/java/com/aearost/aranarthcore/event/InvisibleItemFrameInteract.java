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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.InvisibleItemFrame;
import com.aearost.aranarthcore.runnable.ItemFrameUpdateRunnable;

import java.util.Objects;

/**
 * Inspired by tiffany352
 * Source: <a href="https://github.com/tiffany352/InvisibleItemFrames/blob/main/src/main/java/com/tiffnix/invisibleitemframes/PluginListener.java">tiffany352 GitHub</a>
 */
public class InvisibleItemFrameInteract implements Listener {
	
	private Location locationToPlace = null;
	private BlockFace faceToPlace = null;
	private boolean isInvisibleItemFrameDestroyed = false;

	public InvisibleItemFrameInteract(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
     * Handles placing an Invisible Item Frame.
	 * @param e The event.
     */
    @EventHandler
    public void onPlayerItemFrameInteract(PlayerInteractEvent e) {
        ItemStack is = e.getItem();
        if (is != null) {
        	if (is.getType() == Material.ITEM_FRAME) {
            	if (is.hasItemMeta()) {
            		if (Objects.requireNonNull(is.getItemMeta()).hasLore()) {
            	        Block block = e.getClickedBlock();
            	        if (block != null) {
            	        	locationToPlace = block.getLocation();
            	        	faceToPlace = e.getBlockFace();
            	        }
            		}
            	}
            }
        }
    }

	/**
	 * Sets the item in the item frame which is not yet hidden.
	 * @param e The event.
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
     * Adds the item to the invisible item frame and makes the frame invisible.
     * @param e The event.
     */
    @EventHandler
    public void onPlayerItemFrameEntityInteract(final PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        if (InvisibleItemFrame.isInvisibleItemFrame(entity)) {
        	ItemFrame itemFrame = (ItemFrame) entity;
            new ItemFrameUpdateRunnable(itemFrame).runTask(InvisibleItemFrame.PLUGIN);
        }
    }
	
    /**
     * Removes the item from the invisible item frame and makes the frame visible again.
     * @param e The event.
     */
    @EventHandler
    public void onPlayerInteractEntity(final EntityDamageByEntityEvent e) {
    	Entity entity = e.getEntity();
        if (InvisibleItemFrame.isInvisibleItemFrame(entity)) {
        	ItemFrame itemFrame = (ItemFrame) e.getEntity();
        	new ItemFrameUpdateRunnable(itemFrame).runTask(InvisibleItemFrame.PLUGIN);
        }
    }
    
    /**
     * Trigger update to the item frame when it is destroyed.
	 * @param e The event.
     */
    @EventHandler
    public void onPlayerItemFrameEntityInteract(final HangingBreakByEntityEvent e) {
        Entity entity = e.getEntity();
        if (InvisibleItemFrame.isInvisibleItemFrame(entity)) {
        	ItemFrame itemFrame = (ItemFrame) entity;
            new ItemFrameUpdateRunnable(itemFrame).runTask(InvisibleItemFrame.PLUGIN);
            this.isInvisibleItemFrameDestroyed = true;
        }
    }
    
    /**
     * Drops an invisible item frame if it is destroyed.
     * @param e The event.
     */
    @EventHandler
    public void onItemFrameDrop(final ItemSpawnEvent e) {
    	if (e.getEntity().getItemStack().getType() == Material.ITEM_FRAME) {
    		if (this.isInvisibleItemFrameDestroyed) {
    			e.getEntity().setItemStack(InvisibleItemFrame.getInvisibleItemFrame());
    		}
    	}
    }
	
}
