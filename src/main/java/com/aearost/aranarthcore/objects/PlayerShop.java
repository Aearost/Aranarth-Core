package com.aearost.aranarthcore.objects;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Handles all necessary functionality relating to a player shop.
 */
public class PlayerShop {

    private UUID uuid;
    private Location location;
    private ItemStack item;
    private int quantity;
    private double purchasePrice;
    private double sellPrice;

    public PlayerShop(UUID uuid, Location location, ItemStack item, int quantity, double purchasePrice, double sellPrice) {
        this.uuid = uuid;
        this.location = location;
        this.item = item;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.sellPrice = sellPrice;
    }

    /**
     * Provides the player's UUID.
     * @return The player's UUID.
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Sets the player's UUID.
     * @param uuid The player's UUID.
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Provides the shop's sign Location.
     * @return The shop's sign location.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the shop's sign location.
     * @param location The shop's sign location.
     */
    public void setLocation(Location location) {
        this.location = location;
    }

//    /**
//     * Provides the shop's X-coordinate.
//     * @return The shop's X-coordinate.
//     */
//    public int getX() {
//        return x;
//    }
//
//    /**
//     * Sets the shop's X-coordinate.
//     * @param x The shop's X-coordinate.
//     */
//    public void setX(int x) {
//        this.x = x;
//    }
//
//    /**
//     * Provides the shop's Y-coordinate.
//     * @return The shop's Y-coordinate.
//     */
//    public int getY() {
//        return y;
//    }
//
//    /**
//     * Sets the shop's Y-coordinate.
//     * @param y The shop's Y-coordinate.
//     */
//    public void setY(int y) {
//        this.y = y;
//    }
//
//    /**
//     * Provides the shop's Z-coordinate.
//     * @return The shop's Z-coordinate.
//     */
//    public int getZ() {
//        return z;
//    }
//
//    /**
//     * Sets the shop's Z-coordinate.
//     * @param z The shop's Z-coordinate.
//     */
//    public void setZ(int z) {
//        this.z = z;
//    }

    /**
     * Provides the shop's item.
     * @return The shop's item.
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Sets the shop's item.
     * @param item The shop's item.
     */
    public void setItem(ItemStack item) {
        this.item = item;
    }

    /**
     * Provides the shop's stored quantity.
     * @return The shop's stored quantity.
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Sets the shop's stored quantity.
     * @param quantity The shop's stored quantity.
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Provides the shop's purchase price.
     * @return The shop's purchase price.
     */
    public double getPurchasePrice() {
        return purchasePrice;
    }

    /**
     * Sets the shop's purchase price.
     * @param purchasePrice The shop's stored quantity.
     */
    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    /**
     * Provides the shop's sell price.
     * @return The shop's sell price.
     */
    public double getSellPrice() {
        return sellPrice;
    }

    /**
     * Sets the shop's sell price.
     * @param sellPrice The shop's sell price.
     */
    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }
}
