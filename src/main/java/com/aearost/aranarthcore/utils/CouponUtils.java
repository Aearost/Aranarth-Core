package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.Coupon;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Manages the pool of redeemable store discount coupons loaded from coupons.txt.
 */
public class CouponUtils {

    private static final List<Coupon> coupons = new ArrayList<>();
    private static String filePath;

    public static void initialize(File dataFolder) {
        filePath = dataFolder.getAbsolutePath() + File.separator + "coupons.txt";
        load();
    }

    /**
     * Loads coupons from coupons.txt into memory.
     */
    public static void load() {
        coupons.clear();
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        try {
            Scanner reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the coupons file...");
            while (reader.hasNextLine()) {
                String row = reader.nextLine().trim();
                if (row.startsWith("#") || row.isEmpty()) {
                    continue;
                }
                String[] parts = row.split("\\|", 2);
                if (parts.length < 2) {
                    continue;
                }
                try {
                    int discount = Integer.parseInt(parts[0].trim());
                    String code = parts[1].trim();
                    coupons.add(new Coupon(discount, code));
                } catch (NumberFormatException ignored) {
                }
            }
            reader.close();
            Bukkit.getLogger().info("[AC] Coupons initialised (" + coupons.size() + " available)");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to load coupons.txt: " + e.getMessage());
        }
    }

    /**
     * Saves the current coupon pool back to coupons.txt, overwriting it.
     */
    public static void save() {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (!parent.isDirectory()) {
            boolean result = parent.mkdirs();
        }
        try {
            if (file.createNewFile()) {
                Bukkit.getLogger().info("[AC] A new coupons.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] An error occurred in the creation of coupons.txt");
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("#discount|code\n");
            for (Coupon coupon : coupons) {
                writer.write(coupon.getDiscountPercentage() + "|" + coupon.getCode() + "\n");
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AC] There was an error saving coupons.txt: " + e.getMessage());
        }
    }

    /**
     * Consumes and returns the first available coupon matching the given discount percentage,
     * immediately persisting the removal to disk.
     *
     * @param discountPercentage The discount tier (e.g. 10 or 30).
     * @return The consumed coupon, or empty if none are available for that tier.
     */
    public static Optional<Coupon> consumeCoupon(int discountPercentage) {
        Iterator<Coupon> it = coupons.iterator();
        while (it.hasNext()) {
            Coupon coupon = it.next();
            if (coupon.getDiscountPercentage() == discountPercentage) {
                it.remove();
                save();
                long remaining = coupons.stream()
                        .filter(c -> c.getDiscountPercentage() == discountPercentage)
                        .count();
                if (remaining <= 10) {
                    DiscordUtils.createNotification(
                            "There are only " + remaining + " remaining " + discountPercentage + "% Coupons", null);
                }
                return Optional.of(coupon);
            }
        }
        return Optional.empty();
    }

    public static List<Coupon> getCoupons() {
        return coupons;
    }
}
