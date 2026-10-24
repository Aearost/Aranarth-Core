package com.aearost.aranarthcore.objects;

/**
 * Represents a single-use store discount coupon with a code and discount percentage.
 */
public class Coupon {

    private final int discountPercentage;
    private final String code;

    public Coupon(int discountPercentage, String code) {
        this.discountPercentage = discountPercentage;
        this.code = code;
    }

    public int getDiscountPercentage() {
        return discountPercentage;
    }

    public String getCode() {
        return code;
    }
}
