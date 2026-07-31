package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiWrench;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles clicks inside the Wrench GUI.
 */
public class GuiWrenchClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) {
            return;
        }
        if (e.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }
        if (e.getCurrentItem().getType() == Material.KNOWLEDGE_BOOK) {
            return;
        }

        Block block = GuiWrench.openBlocks.get(player.getUniqueId());
        if (block == null) {
            return;
        }

        BlockData data = block.getBlockData();
        String propertyName = ChatUtils.stripColorFormatting(
                e.getCurrentItem().getItemMeta().getDisplayName());

        switch (propertyName) {
            case "Facing" -> cycleFacing(player, block, data);
            case "Axis" -> cycleAxis(player, block, data);
            case "Shape" -> cycleShape(player, block, data);
            case "Half" -> cycleHalf(player, block, data);
            case "Slab Type" -> cycleSlabType(player, block, data);
            case "Attachment" -> cycleAttachment(player, block, data);
            case "Wall Direction" -> cycleWallDirection(player, block, data);
            case "Hinge" -> cycleHinge(player, block, data);
            case "Open" -> cycleOpen(player, block, data);
            case "In Wall" -> cycleInWall(player, block, data);
            case "Rotation" -> cycleRotation(player, block, data);
        }

        GuiWrench.refresh(player);
        player.playSound(player, Sound.BLOCK_METAL_PLACE, 1F, 0.5F);
    }

    private void cycleFacing(Player player, Block block, BlockData data) {
        if (!(data instanceof Directional directional)) {
            return;
        }

        List<BlockFace> faces = new ArrayList<>(directional.getFaces());
        int idx = faces.indexOf(directional.getFacing());
        BlockFace next = faces.get((idx + 1) % faces.size());

        // New facing must have a supporting block
        if (data instanceof TrapDoor) {
            if (!block.getRelative(next).getType().isSolid()) {
                player.sendMessage(ChatUtils.chatMessage(
                        "&cCannot face &e" + next.name() + "&c - no block to attach to there"));
                return;
            }
        }

        directional.setFacing(next);

        // Sync both halves of doors so top and bottom stay consistent
        if (data instanceof Door door) {
            applyDoorChange(block, door);
        } else {
            block.setBlockData(directional, false);
        }
    }

    private void cycleAxis(Player player, Block block, BlockData data) {
        if (!(data instanceof Orientable orientable)) {
            return;
        }
        Axis[] axes = Axis.values();
        int idx = orientable.getAxis().ordinal();
        orientable.setAxis(axes[(idx + 1) % axes.length]);
        block.setBlockData(orientable, false);
    }

    private void cycleShape(Player player, Block block, BlockData data) {
        if (!(data instanceof Stairs stairs)) {
            return;
        }
        Stairs.Shape[] shapes = Stairs.Shape.values();
        int idx = stairs.getShape().ordinal();
        stairs.setShape(shapes[(idx + 1) % shapes.length]);
        block.setBlockData(stairs, false);
    }

    private void cycleHalf(Player player, Block block, BlockData data) {
        if (!(data instanceof Bisected bisected)) {
            return;
        }
        Bisected.Half next = bisected.getHalf() == Bisected.Half.BOTTOM
                ? Bisected.Half.TOP : Bisected.Half.BOTTOM;
        bisected.setHalf(next);
        block.setBlockData(bisected, false);
    }

    private void cycleSlabType(Player player, Block block, BlockData data) {
        if (!(data instanceof Slab slab)) {
            return;
        }
        // Cycle only BOTTOM and TOP - never set DOUBLE
        Slab.Type next = slab.getType() == Slab.Type.BOTTOM ? Slab.Type.TOP : Slab.Type.BOTTOM;
        slab.setType(next);
        block.setBlockData(slab, false);
    }

    private void cycleAttachment(Player player, Block block, BlockData data) {
        if (!(data instanceof Switch switchData)) {
            return;
        }

        Switch.Face[] faces = Switch.Face.values(); // FLOOR, WALL, CEILING
        int idx = switchData.getFace().ordinal();
        Switch.Face next = faces[(idx + 1) % faces.length];

        // Verify the target surface exists
        if (!attachmentSupported(block, switchData, next)) {
            player.sendMessage(ChatUtils.chatMessage(
                    "&cCannot attach to &e" + next.name() + "&c - no block to attach to there"));
            return;
        }

        switchData.setFace(next);
        block.setBlockData(switchData, false);
    }

    private void cycleWallDirection(Player player, Block block, BlockData data) {
        if (!(data instanceof Switch switchData)) {
            return;
        }

        List<BlockFace> faces = new ArrayList<>(switchData.getFaces());
        int idx = faces.indexOf(switchData.getFacing());
        BlockFace next = faces.get((idx + 1) % faces.size());

        // Only validate float when the attachment is currently WALL
        if (switchData.getFace() == Switch.Face.WALL) {
            // When face is WALL, the support is on the opposite side of the facing direction
            if (!block.getRelative(next.getOppositeFace()).getType().isSolid()) {
                player.sendMessage(ChatUtils.chatMessage(
                        "&cCannot face &e" + next.name() + "&c - no block to attach to on that wall"));
                return;
            }
        }

        switchData.setFacing(next);
        block.setBlockData(switchData, false);
    }

    private void cycleHinge(Player player, Block block, BlockData data) {
        if (!(data instanceof Door door)) {
            return;
        }
        Door.Hinge next = door.getHinge() == Door.Hinge.LEFT ? Door.Hinge.RIGHT : Door.Hinge.LEFT;
        door.setHinge(next);
        applyDoorChange(block, door);
    }

    private void cycleOpen(Player player, Block block, BlockData data) {
        if (!(data instanceof Openable openable)) {
            return;
        }
        openable.setOpen(!openable.isOpen());

        if (data instanceof Door door) {
            applyDoorChange(block, door);
        } else {
            block.setBlockData(openable, false);
        }
    }

    private void cycleInWall(Player player, Block block, BlockData data) {
        if (!(data instanceof Gate gate)) {
            return;
        }
        gate.setInWall(!gate.isInWall());
        block.setBlockData(gate, false);
    }

    private void cycleRotation(Player player, Block block, BlockData data) {
        if (!(data instanceof Rotatable rotatable)) {
            return;
        }
        List<BlockFace> rots = GuiWrench.ROTATIONS;
        int idx = rots.indexOf(rotatable.getRotation());
        rotatable.setRotation(rots.get((idx + 1) % rots.size()));
        block.setBlockData(rotatable, false);
    }


    /**
     * Returns whether the proposed attachment face has a supporting block.
     */
    private boolean attachmentSupported(Block block, Switch switchData, Switch.Face proposedFace) {
        return switch (proposedFace) {
            case FLOOR -> block.getRelative(BlockFace.DOWN).getType().isSolid();
            case CEILING -> block.getRelative(BlockFace.UP).getType().isSolid();
            case WALL -> block.getRelative(switchData.getFacing().getOppositeFace()).getType().isSolid();
        };
    }

    /**
     * Applies a door's current BlockData to both the top and bottom halves to stay in sync.
     */
    private void applyDoorChange(Block block, Door door) {
        block.setBlockData(door, false);

        BlockFace otherFace = door.getHalf() == Bisected.Half.BOTTOM ? BlockFace.UP : BlockFace.DOWN;
        Block other = block.getRelative(otherFace);
        if (other.getBlockData() instanceof Door otherDoor) {
            otherDoor.setFacing(door.getFacing());
            otherDoor.setHinge(door.getHinge());
            otherDoor.setOpen(door.isOpen());
            other.setBlockData(otherDoor, false);
        }
    }
}
