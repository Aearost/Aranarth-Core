package com.aearost.aranarthcore.objects;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Handles all necessary functionality relating to a shop.
 */
public class Shop {

    private UUID uuid;
    private Location location;
    private ItemStack item;
    private int quantity;
    private double buyPrice;
    private double sellPrice;

    public Shop(UUID uuid, Location location, ItemStack item, int quantity, double buyPrice, double sellPrice) {
        this.uuid = uuid;
        this.location = location;
        this.item = item;
        this.quantity = quantity;
        this.buyPrice = buyPrice;
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
    public double getBuyPrice() {
        return buyPrice;
    }

    /**
     * Sets the shop's purchase price.
     * @param buyPrice The shop's stored quantity.
     */
    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
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
