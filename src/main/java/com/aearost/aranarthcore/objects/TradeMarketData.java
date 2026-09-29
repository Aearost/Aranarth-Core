package com.aearost.aranarthcore.objects;

/**
 * Holds trade-derived market pricing data for a single item type.
 */
public class TradeMarketData {

    private final String marketKey;
    private final String displayName;
    private final String material;
    private final String itemModelKey; // null for vanilla items
    private double totalValue;
    private long totalUnits;
    private double firstPrice;

    public TradeMarketData(String marketKey, String displayName, String material,
                           String itemModelKey, double totalValue, long totalUnits, double firstPrice) {
        this.marketKey = marketKey;
        this.displayName = displayName;
        this.material = material;
        this.itemModelKey = itemModelKey;
        this.totalValue = totalValue;
        this.totalUnits = totalUnits;
        this.firstPrice = firstPrice;
    }

    public String getMarketKey() { return marketKey; }
    public String getDisplayName() { return displayName; }
    public String getMaterial() { return material; }
    public String getItemModelKey() { return itemModelKey; }
    public double getTotalValue() { return totalValue; }
    public long getTotalUnits() { return totalUnits; }
    public double getFirstPrice() { return firstPrice; }

    public void setFirstPrice(double firstPrice) { this.firstPrice = firstPrice; }

    /**
     * Returns the current weighted-average market price per unit.
     */
    public double getCurrentPrice() {
        return totalUnits > 0 ? totalValue / totalUnits : firstPrice;
    }

    /**
     * Records a new transaction into the weighted average.
     * @param allocatedMoney Total money attributed to this item type for this transaction.
     * @param units          Number of units traded.
     */
    public void record(double allocatedMoney, long units) {
        if (units <= 0 || allocatedMoney <= 0) return;
        if (totalUnits == 0) {
            firstPrice = allocatedMoney / units;
        }
        totalValue += allocatedMoney;
        totalUnits += units;
    }
}
