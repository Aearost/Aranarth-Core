package com.aearost.aranarthcore.objects;

/**
 * Holds the dynamic pricing state for a single server shop.
 */
public class MarketDynamics {

    private String shopKey;
    private double defaultSellPrice;
    private double currentPriceModifier;
    private double sellPressure;
    // In-memory only - the pressure already priced in by the last price tick, used to isolate new sales
    private double appliedPressure;

    public MarketDynamics(String shopKey, double defaultSellPrice, double currentPriceModifier, double sellPressure) {
        this.shopKey = shopKey;
        this.defaultSellPrice = defaultSellPrice;
        this.currentPriceModifier = currentPriceModifier;
        this.sellPressure = sellPressure;
        this.appliedPressure = sellPressure;
    }

    /** Returns the unique key for this shop. */
    public String getShopKey() {
        return shopKey;
    }

    public void setShopKey(String shopKey) {
        this.shopKey = shopKey;
    }

    /** Returns the admin-configured default sell price for this shop. */
    public double getDefaultSellPrice() {
        return defaultSellPrice;
    }

    public void setDefaultSellPrice(double defaultSellPrice) {
        this.defaultSellPrice = defaultSellPrice;
    }

    /**
     * Returns the current price modifier (0.25 to 2.5).
     * The actual sell price equals defaultSellPrice * currentPriceModifier.
     */
    public double getCurrentPriceModifier() {
        return currentPriceModifier;
    }

    public void setCurrentPriceModifier(double currentPriceModifier) {
        this.currentPriceModifier = currentPriceModifier;
    }

    /** Returns the accumulated sell pressure (decays over time). */
    public double getSellPressure() {
        return sellPressure;
    }

    public void setSellPressure(double sellPressure) {
        this.sellPressure = sellPressure;
    }

    /** Returns the sell pressure that was already priced in by the last price tick. */
    public double getAppliedPressure() {
        return appliedPressure;
    }

    public void setAppliedPressure(double appliedPressure) {
        this.appliedPressure = appliedPressure;
    }

    /**
     * Adds the given delta to the current sell pressure.
     *
     * @param delta The amount of pressure to add (quantitySold / a full inventory of the item).
     */
    public void addPressure(double delta) {
        this.sellPressure += delta;
    }
}
