package com.aearost.aranarthcore.crops;

import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The leaves a vine has grown over its trellis, the fruit each one holds, and the spots it was trimmed from, which it never
 * grows back into.
 * its own.
 */
public class Vine {

    public static final int MAX_LEAVES = 15;
    public static final int MAX_FRUIT = 24;
    public static final int MAX_FRUIT_PER_LEAF = 3;
    public static final double FRUIT_PER_LEAF = 1.5;
    private static final int MAX_TRIMMED = 100;
    private static final String VERSION = "v2";

    /**
     * A leaf of the vine.
     * @param face The side its fruit shows on, DOWN for hanging below it, or null when it holds none.
     * @param slots Each piece of fruit it holds: 0 when it is there, or else when it grows back.
     */
    public record Leaf(CropLayout.Offset at, BlockFace face, List<Long> slots) {
        /**
         * Provides how much fruit is on it now.
         */
        public int fruit() {
            int count = 0;
            for (long slot : slots) {
                if (slot == 0) {
                    count++;
                }
            }
            return count;
        }
    }

    private final List<Leaf> leaves = new ArrayList<>();
    private final Set<CropLayout.Offset> trimmed = new LinkedHashSet<>();

    public List<Leaf> getLeaves() {
        return leaves;
    }

    public int size() {
        return leaves.size();
    }

    public boolean has(CropLayout.Offset at) {
        return find(at) != null;
    }

    public boolean isTrimmed(CropLayout.Offset at) {
        return trimmed.contains(at);
    }

    public Leaf find(CropLayout.Offset at) {
        for (Leaf leaf : leaves) {
            if (leaf.at().equals(at)) {
                return leaf;
            }
        }
        return null;
    }

    public void add(CropLayout.Offset at) {
        leaves.add(new Leaf(at, null, new ArrayList<>()));
    }

    public void set(int index, Leaf leaf) {
        leaves.set(index, leaf);
    }

    /**
     * Removes a leaf.
     * @param trim Whether it was trimmed, so the vine never grows back into its spot.
     * @return The leaf removed, or null if there was none there.
     */
    public Leaf remove(CropLayout.Offset at, boolean trim) {
        Leaf leaf = find(at);
        if (leaf == null) {
            return null;
        }
        leaves.remove(leaf);
        if (trim) {
            trimmed.remove(at);
            trimmed.add(at);
            Iterator<CropLayout.Offset> oldest = trimmed.iterator();
            while (trimmed.size() > MAX_TRIMMED) {
                oldest.next();
                oldest.remove();
            }
        }
        return leaf;
    }

    /**
     * Provides how much fruit is on the vine now.
     */
    public int fruitCount() {
        int count = 0;
        for (Leaf leaf : leaves) {
            count += leaf.fruit();
        }
        return count;
    }

    /**
     * Provides how much fruit the vine holds when all of it has grown.
     */
    public int slotCount() {
        int count = 0;
        for (Leaf leaf : leaves) {
            count += leaf.slots().size();
        }
        return count;
    }

    /**
     * Saves the vine as a serialized text.
     */
    public String serialize() {
        StringBuilder text = new StringBuilder(VERSION).append('#');
        boolean first = true;
        for (Leaf leaf : leaves) {
            if (!first) {
                text.append('/');
            }
            first = false;
            CropLayout.Offset at = leaf.at();
            text.append(at.x()).append(',').append(at.y()).append(',').append(at.z()).append(',')
                    .append(leaf.face() == null ? "-" : leaf.face().name()).append(',');
            for (int i = 0; i < leaf.slots().size(); i++) {
                if (i > 0) {
                    text.append(':');
                }
                text.append(leaf.slots().get(i));
            }
        }
        text.append('|');
        first = true;
        for (CropLayout.Offset at : trimmed) {
            if (!first) {
                text.append('/');
            }
            first = false;
            text.append(at.x()).append(',').append(at.y()).append(',').append(at.z());
        }
        return text.toString();
    }

    /**
     * Reads a saved vine, skipping anything malformed.
     */
    public static Vine parse(String text) {
        Vine vine = new Vine();
        if (text == null || !text.startsWith(VERSION + "#")) {
            return vine;
        }
        String[] halves = text.substring(VERSION.length() + 1).split("\\|", -1);
        for (String part : halves[0].split("/")) {
            String[] p = part.split(",", -1);
            if (p.length < 5) {
                continue;
            }
            try {
                CropLayout.Offset at = new CropLayout.Offset(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                BlockFace face = p[3].equals("-") ? null : BlockFace.valueOf(p[3]);
                List<Long> slots = new ArrayList<>();
                if (face != null && !p[4].isEmpty()) {
                    for (String slot : p[4].split(":")) {
                        if (slots.size() < MAX_FRUIT_PER_LEAF) {
                            slots.add(Math.max(0, Long.parseLong(slot)));
                        }
                    }
                }
                if (!vine.has(at) && vine.size() < MAX_LEAVES) {
                    vine.leaves.add(new Leaf(at, face, slots));
                }
            } catch (IllegalArgumentException ignored) {
                // A malformed leaf is skipped
            }
        }
        if (halves.length > 1) {
            for (String part : halves[1].split("/")) {
                String[] p = part.split(",");
                if (p.length < 3) {
                    continue;
                }
                try {
                    vine.trimmed.add(new CropLayout.Offset(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
                } catch (NumberFormatException ignored) {
                    // A malformed spot is skipped
                }
            }
        }
        return vine;
    }
}
