package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a variety of utility methods for everything related to the text holograms.
 */
public class HologramUtils {

    private static final List<TextDisplay> holograms = new ArrayList<>();

    public static List<TextDisplay> getHolograms() {
        return holograms;
    }

    public static boolean createHologram(Location location, String text) {
        // Prevents use of the separator character
        if (text.contains("||")) {
            return false;
        }

        boolean isLocationAlreadyUsed = false;
        for (TextDisplay textDisplay : holograms) {
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            if (x == textDisplay.getLocation().getBlockX() && y == textDisplay.getLocation().getBlockY()
                    && z == textDisplay.getLocation().getBlockZ()) {
                Bukkit.getLogger().info("Already used!!!");
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
     */
    public static void removeAllHolograms() {
        PersistenceUtils.saveTextHolograms();
        // Manually remove holograms in case a server crash caused them to remain incorrectly
        Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "kill @e[type=minecraft:text_display]");
        holograms.clear();
    }

    /**
     * Adds all holograms to the world.
     */
    public static void initializeAllHolograms() {
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), new Runnable() {
            @Override
            public void run() {
                PersistenceUtils.loadTextHolograms();
            }
        }, 1);
    }
}
