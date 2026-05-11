package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Increases the amount of patterns able to be placed on a banner up to 15.
 */
public class BannerExtendPatternLimit {

    private static final int MAX_PATTERNS = 15;
    // Strip to 5 so there is always room to add one more through the loom.
    private static final int STRIP_TO = 5;

    private static final NamespacedKey EXTRA_PATTERNS_KEY =
            new NamespacedKey(AranarthCore.getInstance(), "extra_banner_patterns");

    public void execute(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }

        int slot = e.getSlot();
        if (slot == 0) {
            handleBannerSlot(e);
        } else if (slot == 3) {
            handleResultSlot(e);
        }
    }

    // -------------------------------------------------------------------------
    // Slot 0 — banner input
    // -------------------------------------------------------------------------

    private void handleBannerSlot(InventoryClickEvent e) {
        ItemStack current = e.getCurrentItem();
        ItemStack cursor = e.getCursor();

        boolean slotEmpty = current == null || current.getType() == Material.AIR;
        boolean cursorBanner = cursor != null && cursor.getType() != Material.AIR
                && cursor.getItemMeta() instanceof BannerMeta;
        boolean cursorEmpty = cursor == null || cursor.getType() == Material.AIR;

        if (slotEmpty && cursorBanner) {
            onBannerPlaced(e, (BannerMeta) cursor.getItemMeta());
        } else if (!slotEmpty && cursorEmpty && current.getItemMeta() instanceof BannerMeta currentMeta) {
            onBannerRemoved(e, currentMeta);
        }
    }

    /**
     * A banner with extra patterns is being placed into slot 0.
     * Cancel the event, build a stripped copy with extras stored in PDC, and place it
     * in slot 0 right now — all in this tick. Then defer the cursor clear to the next tick.
     */
    private void onBannerPlaced(InventoryClickEvent e, BannerMeta meta) {
        if (meta.getPatterns().size() <= STRIP_TO) {
            return;
        }

        // Build stripped meta
        BannerMeta stripped = (BannerMeta) meta.clone();
        List<Pattern> extras = new ArrayList<>();
        while (stripped.getPatterns().size() > STRIP_TO) {
            int last = stripped.getPatterns().size() - 1;
            extras.add(0, stripped.getPattern(last)); // prepend to preserve order
            stripped.removePattern(last);
        }
        stripped.getPersistentDataContainer().set(EXTRA_PATTERNS_KEY, PersistentDataType.STRING, encode(extras));

        // Build the item, cancel, and place directly — same tick, no scheduler
        ItemStack strippedBanner = e.getCursor().clone();
        strippedBanner.setItemMeta(stripped);

        e.setCancelled(true);
        e.getClickedInventory().setItem(0, strippedBanner);

        // The cancel leaves the original banner on the cursor. Clear it next tick.
        Player player = (Player) e.getWhoClicked();
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                () -> player.setItemOnCursor(new ItemStack(Material.AIR)));
    }

    /**
     * The stripped banner is being removed from slot 0.
     * Let vanilla return it to the cursor or inventory, then restore the extra patterns
     * from PDC on the next tick.
     */
    private void onBannerRemoved(InventoryClickEvent e, BannerMeta currentMeta) {
        if (!currentMeta.getPersistentDataContainer().has(EXTRA_PATTERNS_KEY, PersistentDataType.STRING)) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> restoreToPlayer(player));
    }

    // -------------------------------------------------------------------------
    // Slot 3 — result
    // -------------------------------------------------------------------------

    /**
     * Player is taking the loom result.
     * Read the extra patterns from slot 0's PDC now, before vanilla consumes the banner.
     * For normal clicks, let vanilla handle delivery and ingredient consumption, then
     * upgrade the cursor item on the next tick.
     * For shift-clicks, cancel and deliver the upgraded result manually.
     */
    private void handleResultSlot(InventoryClickEvent e) {
        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }
        if (!(result.getItemMeta() instanceof BannerMeta resultMeta)) {
            return;
        }

        // Read extras from slot 0 NOW — vanilla will consume this item after the event
        ItemStack bannerInSlot = e.getClickedInventory().getItem(0);
        if (bannerInSlot == null || !(bannerInSlot.getItemMeta() instanceof BannerMeta slotMeta)) {
            return;
        }

        String encoded = slotMeta.getPersistentDataContainer()
                .get(EXTRA_PATTERNS_KEY, PersistentDataType.STRING);
        if (encoded == null || encoded.isEmpty()) {
            return;
        }

        List<Pattern> extras = decode(encoded);
        if (extras.isEmpty()) {
            return;
        }

        Player player = (Player) e.getWhoClicked();

        if (!e.isShiftClick()) {
            // Normal click: vanilla delivers result to cursor and consumes ingredients.
            // Capture extras and append to the cursor item on the next tick.
            List<Pattern> captured = List.copyOf(extras);
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                ItemStack onCursor = player.getItemOnCursor();
                if (onCursor == null || onCursor.getType() == Material.AIR) {
                    return;
                }
                if (!(onCursor.getItemMeta() instanceof BannerMeta cursorMeta)) {
                    return;
                }

                // The last pattern in the result is the one just applied via the loom.
                // Extras must be inserted before it so the new pattern stays on top visually.
                List<Pattern> current = cursorMeta.getPatterns();
                if (current.isEmpty()) {
                    return;
                }
                Pattern newPattern = current.get(current.size() - 1);
                cursorMeta.removePattern(current.size() - 1);

                // Reserve one slot for the new pattern when calculating space
                int space = MAX_PATTERNS - cursorMeta.getPatterns().size() - 1;
                for (int i = 0; i < Math.min(captured.size(), space); i++) {
                    cursorMeta.addPattern(captured.get(i));
                }
                cursorMeta.addPattern(newPattern);

                onCursor.setItemMeta(cursorMeta);
                player.setItemOnCursor(onCursor);

                if (cursorMeta.getPatterns().size() > 6) {
                    player.sendMessage(ChatUtils.chatMessage("&7You have exceeded the banner pattern limit of 6 - proceed with caution!"));
                }
            });
        } else {
            // Shift-click: cancel and deliver the upgraded result to inventory manually.
            // The last pattern in the result is the one just applied via the loom.
            // Extras must be inserted before it so the new pattern stays on top visually.
            List<Pattern> resultPatterns = resultMeta.getPatterns();
            Pattern newPattern = resultPatterns.get(resultPatterns.size() - 1);
            resultMeta.removePattern(resultPatterns.size() - 1);

            int space = MAX_PATTERNS - resultMeta.getPatterns().size() - 1;
            for (int i = 0; i < Math.min(extras.size(), space); i++) {
                resultMeta.addPattern(extras.get(i));
            }
            resultMeta.addPattern(newPattern);
            ItemStack finalResult = result.clone();
            finalResult.setItemMeta(resultMeta);

            if (resultMeta.getPatterns().size() > 6) {
                player.sendMessage("You are now exceeding the banner pattern limit of 6 - proceed with caution!");
            }

            e.setCancelled(true);
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                var leftover = player.getInventory().addItem(finalResult);
                leftover.values().forEach(item ->
                        player.getWorld().dropItem(player.getLocation(), item));
            });

            // Consume banner and dye manually (slot 2 pattern item is not consumed in vanilla)
            e.getClickedInventory().setItem(0, null);
            ItemStack dye = e.getClickedInventory().getItem(1);
            if (dye != null && dye.getAmount() > 1) {
                dye.setAmount(dye.getAmount() - 1);
                e.getClickedInventory().setItem(1, dye);
            } else {
                e.getClickedInventory().setItem(1, null);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Searches the player's cursor then their inventory for a banner that has the PDC key
     * and restores the extra patterns, removing the key afterward.
     */
    private void restoreToPlayer(Player player) {
        // Normal click returns the banner to cursor
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() != Material.AIR
                && cursor.getItemMeta() instanceof BannerMeta cursorMeta
                && tryRestore(cursor, cursorMeta)) {
            player.setItemOnCursor(cursor);
            return;
        }
        // Shift-click sends the banner directly to inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || !(item.getItemMeta() instanceof BannerMeta meta)) {
                continue;
            }
            if (tryRestore(item, meta)) {
                player.getInventory().setItem(i, item);
                return;
            }
        }
    }

    /**
     * If the banner has the EXTRA_PATTERNS_KEY PDC entry, appends the stored patterns
     * and removes the key. Returns true if restoration was performed.
     */
    private boolean tryRestore(ItemStack item, BannerMeta meta) {
        String encoded = meta.getPersistentDataContainer()
                .get(EXTRA_PATTERNS_KEY, PersistentDataType.STRING);
        if (encoded == null || encoded.isEmpty()) {
            return false;
        }

        List<Pattern> extras = decode(encoded);
        int space = MAX_PATTERNS - meta.getPatterns().size();
        for (int i = 0; i < Math.min(extras.size(), space); i++) {
            meta.addPattern(extras.get(i));
        }
        meta.getPersistentDataContainer().remove(EXTRA_PATTERNS_KEY);
        item.setItemMeta(meta);
        return true;
    }

    /**
     * Encodes a pattern list to a compact string: "key:COLOR,key:COLOR,..."
     */
    private String encode(List<Pattern> patterns) {
        StringBuilder sb = new StringBuilder();
        for (Pattern p : patterns) {
            if (!sb.isEmpty()) {
                sb.append(",");
            }
            sb.append(Registry.BANNER_PATTERN.getKey(p.getPattern()).getKey()).append(":").append(p.getColor().name());
        }
        return sb.toString();
    }

    /**
     * Decodes a compact string back into a pattern list.
     * Uses Registry.BANNER_PATTERN for Paper 1.21+ where PatternType is registry-backed.
     */
    private List<Pattern> decode(String s) {
        List<Pattern> patterns = new ArrayList<>();
        if (s == null || s.isEmpty()) {
            return patterns;
        }
        for (String part : s.split(",")) {
            String[] kv = part.split(":");
            if (kv.length != 2) {
                continue;
            }
            try {
                PatternType type = Registry.BANNER_PATTERN.get(
                        NamespacedKey.minecraft(kv[0].toLowerCase()));
                org.bukkit.DyeColor color = org.bukkit.DyeColor.valueOf(kv[1].toUpperCase());
                if (type != null) {
                    patterns.add(new Pattern(color, type));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return patterns;
    }
}
