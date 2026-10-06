package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.objects.Coupon;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;

/**
 * Manages the pool of redeemable store discount coupons.
 * When MySQL is active, coupons.txt acts as an import inbox: its codes are moved into the shared
 * server_coupons table so both servers draw from one pool. Otherwise, coupons.txt is the local pool.
 */
public class CouponUtils {

    private static final String HEADER = "#discount|code";
    private static final List<Coupon> coupons = new ArrayList<>();
    private static String filePath;

    public static void initialize(File dataFolder) {
        filePath = dataFolder.getAbsolutePath() + File.separator + "coupons.txt";
        importCoupons();
    }

    /**
     * Reads coupons.txt. With MySQL active, the valid codes are inserted into the shared pool and
     * the file is cleared, keeping only lines that could not be parsed. Without MySQL, the valid
     * codes are loaded into the local pool.
     * Performs blocking I/O, so it should be called off the main thread outside of startup.
     *
     * @return {added, skipped, invalid}, or null if the import failed and the file was left untouched.
     */
    public static int[] importCoupons() {
        File file = new File(filePath);
        if (!file.exists()) {
            writeFile(new ArrayList<>());
            return new int[]{0, 0, 0};
        }

        List<Coupon> parsed = new ArrayList<>();
        List<String> invalidLines = new ArrayList<>();
        try (Scanner reader = new Scanner(file)) {
            Bukkit.getLogger().info("[AC] Attempting to read the coupons file...");
            while (reader.hasNextLine()) {
                String row = reader.nextLine().trim();
                if (row.startsWith("#") || row.isEmpty()) {
                    continue;
                }
                String[] parts = row.split("\\|", 2);
                try {
                    String code = parts[1].trim();
                    if (code.isEmpty()) {
                        throw new IllegalArgumentException();
                    }
                    parsed.add(new Coupon(Integer.parseInt(parts[0].trim()), code));
                } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                    invalidLines.add(row);
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AC] Failed to read coupons.txt: " + e.getMessage());
            return null;
        }

        if (!DatabaseManager.isActive()) {
            coupons.clear();
            coupons.addAll(parsed);
            Bukkit.getLogger().info("[AC] Coupons initialised (" + coupons.size() + " available)");
            return new int[]{parsed.size(), 0, invalidLines.size()};
        }

        if (parsed.isEmpty()) {
            return new int[]{0, 0, invalidLines.size()};
        }

        int[] result = DatabaseManager.getInstance().importCoupons(parsed);
        if (result == null) {
            return null;
        }
        // Only clear the file once the codes are safely committed to the database
        writeFile(invalidLines);
        Bukkit.getLogger().info("[AC] Imported " + result[0] + " coupons into the database ("
                + result[1] + " already stored, " + invalidLines.size() + " invalid)");
        return new int[]{result[0], result[1], invalidLines.size()};
    }

    /**
     * Claims the next available coupon of the given discount tier.
     * With MySQL active, the claim runs asynchronously; the callback always runs on the main thread.
     *
     * @param discountPercentage The discount tier (e.g. 10 or 30).
     * @param callback Receives the claimed coupon (null if none are available) and how many of that tier remain.
     */
    public static void claimCoupon(int discountPercentage, BiConsumer<Coupon, Integer> callback) {
        if (!DatabaseManager.isActive()) {
            Coupon claimed = consumeLocalCoupon(discountPercentage);
            long remaining = coupons.stream().filter(c -> c.getDiscountPercentage() == discountPercentage).count();
            callback.accept(claimed, (int) remaining);
            return;
        }

        AranarthCore plugin = AranarthCore.getInstance();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager.ClaimedCoupon claimed = DatabaseManager.getInstance().claimCoupon(discountPercentage);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (claimed == null) {
                    callback.accept(null, 0);
                } else {
                    callback.accept(new Coupon(discountPercentage, claimed.code()), claimed.remaining());
                }
            });
        });
    }

    /**
     * Removes the first local coupon matching the given discount percentage and persists the removal.
     */
    private static Coupon consumeLocalCoupon(int discountPercentage) {
        Iterator<Coupon> it = coupons.iterator();
        while (it.hasNext()) {
            Coupon coupon = it.next();
            if (coupon.getDiscountPercentage() == discountPercentage) {
                it.remove();
                List<String> lines = new ArrayList<>();
                for (Coupon remaining : coupons) {
                    lines.add(remaining.getDiscountPercentage() + "|" + remaining.getCode());
                }
                writeFile(lines);
                return coupon;
            }
        }
        return null;
    }

    /**
     * Overwrites coupons.txt with the header followed by the given lines.
     */
    private static void writeFile(List<String> lines) {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (!parent.isDirectory()) {
            boolean result = parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(HEADER + "\n");
            for (String line : lines) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AC] There was an error saving coupons.txt: " + e.getMessage());
        }
    }
}
