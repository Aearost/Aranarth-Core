package com.aearost.aranarthcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Marks an inventory as a GUI created by AranarthCore.
 */
public class GuiHolder implements InventoryHolder {

    private final UUID owner;
    private Inventory inventory;
    private Object data;
    private boolean dragAllowed;

    private GuiHolder(Player owner) {
        this.owner = owner == null ? null : owner.getUniqueId();
    }

    /**
     * Creates a chest GUI owned by the given player.
     */
    public static Inventory create(Player owner, int size, String title) {
        GuiHolder holder = new GuiHolder(owner);
        holder.inventory = Bukkit.createInventory(holder, size, title);
        return holder.inventory;
    }

    /**
     * Creates a GUI of the given type owned by the given player.
     */
    public static Inventory create(Player owner, InventoryType type, String title) {
        GuiHolder holder = new GuiHolder(owner);
        holder.inventory = Bukkit.createInventory(holder, type, title);
        return holder.inventory;
    }

    /**
     * Whether the inventory is a GUI created by AranarthCore.
     */
    public static boolean isGui(Inventory inventory) {
        return inventory != null && inventory.getHolder(false) instanceof GuiHolder;
    }

    /**
     * Whether the top inventory of the view is a GUI created by AranarthCore.
     */
    public static boolean isGui(InventoryView view) {
        return view != null && isGui(view.getTopInventory());
    }

    /**
     * Gets the GuiHolder of the inventory, or null if it is not an AranarthCore GUI.
     */
    public static GuiHolder of(Inventory inventory) {
        if (inventory != null && inventory.getHolder(false) instanceof GuiHolder holder) {
            return holder;
        }
        return null;
    }

    /**
     * The UUID of the player the GUI was created for, or null if it has no owner.
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * Server-side state attached to the GUI when it was created, so handlers never need to read it
     * back from item names or lore.
     */
    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    /**
     * Whether items may be dragged across the GUI's slots. Only plain containers allow it, since a
     * drag skips every click handler.
     */
    public boolean isDragAllowed() {
        return dragAllowed;
    }

    public void allowDrag() {
        this.dragAllowed = true;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
