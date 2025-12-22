package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides a variety of utility methods for everything related to the text holograms.
 */
public class HologramUtils {

    private static final List<TextDisplay> holograms = new ArrayList<>();

    public static List<TextDisplay> getHolograms() {
        return holograms;
    }

    public static boolean createHologram(Location location, String text, boolean isFromAutomaticRefresh) {
        // Prevents use of the separator character
        if (text.contains("||")) {
            return false;
        }

        if (isFromAutomaticRefresh && !hasPlayerNearbyHologram(location)) {
            return false;
        }

        BoundingBox box = BoundingBox.of(location, 0.5, 3, 0.5);
        Collection<Entity> nearby = location.getWorld().getNearbyEntities(box);
        for (Entity entity : nearby) {
            if (entity instanceof TextDisplay) {
                entity.remove();
            }
        }

        boolean isLocationAlreadyUsed = false;
        for (TextDisplay textDisplay : holograms) {
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            if (x == textDisplay.getLocation().getBlockX() && y == textDisplay.getLocation().getBlockY()
                    && z == textDisplay.getLocation().getBlockZ()) {
                isLocationAlreadyUsed = true;
                break;
            }
        }

        if (!isLocationAlreadyUsed) {
            Location hologramLoc = location.add(0.5, 0, 0.5);
            TextDisplay hologram = hologramLoc.getWorld().spawn(hologramLoc, TextDisplay.class);
            hologram.setBillboard(Display.Billboard.CENTER);
            hologram.setGravity(false);
            hologram.setVelocity(new Vector(0, 0, 0));
            hologram.setInvulnerable(true);
            hologram.setPersistent(true);
            hologram.setBackgroundColor(null);
            String coloredText = ChatUtils.translateToColor(text);
            String textWithNewLines = coloredText.replaceAll("\\\\n", "\n");
            hologram.setText(textWithNewLines);
            holograms.add(hologram);
            return true;
        } else {
            return false;
        }
    }

    public static boolean removeHologram(Location location) {
        int indexToRemove = -1;
        for (int i = 0; i < holograms.size(); i++) {
            int x = location.getBlockX();
            double y = location.getY();
            int z = location.getBlockZ();
            TextDisplay hologram = holograms.get(i);
            if (x == hologram.getLocation().getBlockX() && y == hologram.getLocation().getBlockY()
                    && z == hologram.getLocation().getBlockZ()) {
                indexToRemove = i;
                break;
            }
        }

        if (indexToRemove != -1) {
            holograms.get(indexToRemove).remove();
            holograms.remove(indexToRemove);
            return true;
        }
        return false;
    }

    /**
     * Removes all holograms from the world.
     * @param isFromAutomaticRefresh If the method was called by the automatic refresh of persisting files.
     */
    public static void removeAllHolograms(boolean isFromAutomaticRefresh) {
        PersistenceUtils.saveTextHolograms();

        List<Integer> toRemove = new ArrayList<>();
        for (int i = 0; i < holograms.size(); i++) {
            TextDisplay hologram = holograms.get(i);
            if (isFromAutomaticRefresh && !hasPlayerNearbyHologram(hologram.getLocation())) {
                continue;
            }

            // Verifies there are no leftover holograms at the location
            Location location = hologram.getLocation();
            BoundingBox box = BoundingBox.of(location, 0.5, 3, 0.5);
            Collection<Entity> nearby = location.getWorld().getNearbyEntities(box);
            for (Entity entity : nearby) {
                if (entity instanceof TextDisplay) {
                    entity.remove();
                }
            }
            toRemove.add(i);
        }

        // Remove from last to first
        for (int i = holograms.size() - 1; i > 0; i--) {
            if (toRemove.contains(i)) {
                holograms.remove(i);
            }
        }
    }

    /**
     * Refreshes all holograms to the world.
     */
    public static void refreshHolograms() {
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), new Runnable() {
            @Override
            public void run() {
                PersistenceUtils.loadTextHolograms(true);
            }
        }, 1);
    }

    /**
     * Determines if there are players nearby the hologram.
     * @param location The location of the hologram.
     * @return Confirmation if there are players nearby the hologram.
     */
    public static boolean hasPlayerNearbyHologram(Location location) {
        BoundingBox box = BoundingBox.of(location, 200, 150, 200);
        Collection<Entity> nearby = location.getWorld().getNearbyEntities(box);
        int playerNum = 0;
        for (Entity entity : nearby) {
            if (entity instanceof Player) {
                playerNum++;
            }
        }

        if (playerNum == 0) {
            return false;
        } else {
            return true;
        }
    }
}
