package com.aearost.aranarthcore.objects;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a player-built gate.
 */
public class Gate {

    public enum Axis {X, Z}

    private final UUID id;
    private final UUID owner;

    /**
     * True if this gate plays metal (copper-chest) sounds; false for wooden
     * (fence-gate) sounds. Determined from the first block placed and does not
     * change when mixed materials are added later.
     */
    private final boolean metalGate;

    /**
     * Maps each block location (normalized) to its original material.
     */
    private Map<Location, Material> blockMaterials;

    private Axis axis;
    private boolean open;

    public Gate(UUID id, UUID owner, boolean metalGate, Location firstBlock, Material firstMaterial) {
        this.id = id;
        this.owner = owner;
        this.metalGate = metalGate;
        this.blockMaterials = new HashMap<>();
        this.blockMaterials.put(firstBlock, firstMaterial);
        this.axis = null;
        this.open = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isMetalGate() {
        return metalGate;
    }

    /**
     * Returns the set of block locations tracked by this gate.
     */
    public Set<Location> getBlocks() {
        return blockMaterials.keySet();
    }

    public Map<Location, Material> getBlockMaterials() {
        return blockMaterials;
    }

    public void setBlockMaterials(Map<Location, Material> blockMaterials) {
        this.blockMaterials = blockMaterials;
    }

    /**
     * Returns the original material stored for the given (normalized) location.
     */
    public Material getMaterialAt(Location loc) {
        Material mat = blockMaterials.get(loc);
        if (mat != null) {
            return mat;
        }
        return metalGate ? Material.IRON_BARS : Material.OAK_FENCE;
    }

    public Axis getAxis() {
        return axis;
    }

    public void setAxis(Axis axis) {
        this.axis = axis;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
