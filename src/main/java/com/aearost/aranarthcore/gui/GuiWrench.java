package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuiWrench {

    public static final String TITLE = "Wrench";

    public static final Map<UUID, Block> openBlocks = new HashMap<>();

    // The 16 valid rotation BlockFaces in order (rotation 0–15)
    public static final List<BlockFace> ROTATIONS = List.of(
            BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST,
            BlockFace.WEST_SOUTH_WEST, BlockFace.WEST, BlockFace.WEST_NORTH_WEST,
            BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST, BlockFace.NORTH,
            BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST,
            BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_EAST,
            BlockFace.SOUTH_SOUTH_EAST
    );

    private final Player player;
    private final Block block;
    private final Inventory gui;

    public GuiWrench(Player player, Block block) {
        this.player = player;
        this.block = block;
        this.gui = Bukkit.createInventory(player, 27, ChatUtils.translateToColor("&8&lWrench"));
        populate();
    }

    public void openGui() {
        openBlocks.put(player.getUniqueId(), block);
        player.closeInventory();
        player.openInventory(gui);
    }

    /**
     * Rebuilds the contents of the player's currently-open wrench GUI in-place
     * so the inventory does not need to be closed and reopened.
     */
    public static void refresh(Player player) {
        Block block = openBlocks.get(player.getUniqueId());
        if (block == null) {
            return;
        }
        if (!ChatUtils.stripColorFormatting(player.getOpenInventory().getTitle()).equals(TITLE)) {
            return;
        }
        GuiWrench rebuilt = new GuiWrench(player, block);
        Inventory live = player.getOpenInventory().getTopInventory();
        for (int i = 0; i < live.getSize(); i++) {
            live.setItem(i, rebuilt.gui.getItem(i));
        }
    }


    private void populate() {
        ItemStack filler = makeFiller();
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        // Block name header
        gui.setItem(4, makeInfo());

        BlockData data = block.getBlockData();
        List<ItemStack> props = buildPropertyItems(data);

        if (props.isEmpty()) {
            return;
        }

        // Centre property items in the middle row (slots 9–17)
        int start = 9 + (9 - props.size()) / 2;
        for (int i = 0; i < props.size(); i++) {
            gui.setItem(start + i, props.get(i));
        }
    }

    private ItemStack makeFiller() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = filler.getItemMeta();
        m.setDisplayName(" ");
        filler.setItemMeta(m);
        return filler;
    }

    private ItemStack makeInfo() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(ChatUtils.translateToColor("&e" + ChatUtils.getFormattedItemName(block.getType().name())));
        m.setLore(List.of(
                ChatUtils.translateToColor("&7Click a property to cycle its value")
        ));
        item.setItemMeta(m);
        return item;
    }

    public static List<ItemStack> buildPropertyItems(BlockData data) {
        List<ItemStack> items = new ArrayList<>();

        // Facing
        if (!(data instanceof Switch) && data instanceof Directional directional) {
            boolean needsFloatCheck = data instanceof TrapDoor;
            items.add(buildFacingItem(directional, needsFloatCheck));
        }

        // Axis
        if (data instanceof Orientable orientable) {
            items.add(buildAxisItem(orientable));
        }

        // Stair shape
        if (data instanceof Stairs stairs) {
            items.add(buildShapeItem(stairs));
        }

        // Half
        if ((data instanceof Stairs || data instanceof TrapDoor) && data instanceof Bisected bisected) {
            items.add(buildHalfItem(bisected));
        }

        // Type
        if (data instanceof Slab slab) {
            items.add(buildSlabTypeItem(slab));
        }

        // Lever
        if (data instanceof Switch switchData) {
            items.add(buildAttachmentItem(switchData));
            items.add(buildWallDirectionItem(switchData));
        }

        // Hinge
        if (data instanceof Door door) {
            items.add(buildHingeItem(door));
        }

        // Open
        if (data instanceof Openable openable) {
            items.add(buildOpenItem(openable));
        }

        // Fence gate wall
        if (data instanceof Gate gate) {
            items.add(buildInWallItem(gate));
        }

        // Rotation
        if (data instanceof Rotatable rotatable) {
            items.add(buildRotationItem(rotatable));
        }

        return items;
    }


    private static ItemStack buildFacingItem(Directional data, boolean needsFloatCheck) {
        String current = data.getFacing().name();
        List<String> available = new ArrayList<>();
        for (BlockFace f : data.getFaces()) {
            available.add(f.name());
        }
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Current: &a" + current));
        lore.add(ChatUtils.translateToColor("&7Available: &f" + String.join("&7, &f", available)));
        lore.add("");
        if (needsFloatCheck) {
            lore.add(ChatUtils.translateToColor("&cSome values may be blocked if the"));
            lore.add(ChatUtils.translateToColor("&cblock would lose its support."));
            lore.add("");
        }
        lore.add(ChatUtils.translateToColor("&eClick &7to cycle"));
        return makePropertyItem(Material.COMPASS, "Facing", lore);
    }

    private static ItemStack buildAxisItem(Orientable data) {
        List<String> lore = List.of(
                ChatUtils.translateToColor("&7Current: &a" + data.getAxis().name()),
                ChatUtils.translateToColor("&7Available: &fX&7, &fY&7, &fZ"),
                "",
                ChatUtils.translateToColor("&eClick &7to cycle")
        );
        return makePropertyItem(Material.STICK, "Axis", lore);
    }

    private static ItemStack buildShapeItem(Stairs data) {
        String current = data.getShape().name().replace('_', ' ');
        List<String> lore = List.of(
                ChatUtils.translateToColor("&7Current: &a" + current),
                ChatUtils.translateToColor("&7Available: &fSTRAIGHT&7, &fINNER LEFT&7, &fINNER RIGHT&7,"),
                ChatUtils.translateToColor("            &fOUTER LEFT&7, &fOUTER RIGHT"),
                "",
                ChatUtils.translateToColor("&eClick &7to cycle")
        );
        return makePropertyItem(Material.STONE_STAIRS, "Shape", lore);
    }

    private static ItemStack buildHalfItem(Bisected data) {
        String current = data.getHalf().name();
        List<String> lore = List.of(
                ChatUtils.translateToColor("&7Current: &a" + current),
                ChatUtils.translateToColor("&7Available: &fBOTTOM&7, &fTOP"),
                "",
                ChatUtils.translateToColor("&eClick &7to cycle")
        );
        return makePropertyItem(Material.STONE_SLAB, "Half", lore);
    }

    private static ItemStack buildSlabTypeItem(Slab data) {
        String current = data.getType().name();
        List<String> lore = List.of(
                ChatUtils.translateToColor("&7Current: &a" + current),
                ChatUtils.translateToColor("&7Available: &fBOTTOM&7, &fTOP"),
                "",
                ChatUtils.translateToColor("&eClick &7to cycle")
        );
        return makePropertyItem(Material.STONE_SLAB, "Slab Type", lore);
    }

    private static ItemStack buildAttachmentItem(Switch data) {
        String current = data.getFace().name();
        List<String> lore = List.of(
                ChatUtils.translateToColor("&7Current: &a" + current),
                ChatUtils.translateToColor("&7Available: &fFLOOR&7, &fWALL&7, &fCEILING"),
                "",
                ChatUtils.translateToColor("&cBlocked if block would be unsupported."),
                "",
                ChatUtils.translateToColor("&eClick &7to cycle")
        );
        return makePropertyItem(Material.STONE_BUTTON, "Attachment", lore);
    }

    private static ItemStack buildWallDirectionItem(Switch data) {
        String current = data.getFacing().name();
        List<String> available = new ArrayList<>();
        for (BlockFace f : data.getFaces()) {
            available.add(f.name());
        }
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Current: &a" + current));
        lore.add(ChatUtils.translateToColor("&7Available: &f" + String.join("&7, &f", available)));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Only applies when attachment is &fWALL&7."));
        lore.add(ChatUtils.translateToColor("&cBlocked if no block exists on that wall."));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&eClick &7to cycle"));
        return makePropertyItem(Material.COMPASS, "Wall Direction", lore);
    }

    private static ItemStack buildHingeItem(Door data) {
        String current = data.getHinge().name();
        List<String> lore = List.of(
                ChatUtils.translateToColor("&7Current: &a" + current),
                ChatUtils.translateToColor("&7Available: &fLEFT&7, &fRIGHT"),
                "",
                ChatUtils.translateToColor("&eClick &7to cycle")
        );
        return makePropertyItem(Material.IRON_DOOR, "Hinge", lore);
    }

    private static ItemStack buildOpenItem(Openable data) {
        String current = data.isOpen() ? "OPEN" : "CLOSED";
        List<String> lore = List.of(
                ChatUtils.translateToColor("&7Current: &a" + current),
                ChatUtils.translateToColor("&7Available: &fOPEN&7, &fCLOSED"),
                "",
                ChatUtils.translateToColor("&eClick &7to toggle")
        );
        return makePropertyItem(Material.OAK_TRAPDOOR, "Open", lore);
    }

    private static ItemStack buildInWallItem(Gate data) {
        String current = data.isInWall() ? "TRUE" : "FALSE";
        List<String> lore = List.of(
                ChatUtils.translateToColor("&7Current: &a" + current),
                ChatUtils.translateToColor("&7Available: &fTRUE&7, &fFALSE"),
                "",
                ChatUtils.translateToColor("&eClick &7to toggle")
        );
        return makePropertyItem(Material.COBBLESTONE_WALL, "In Wall", lore);
    }

    private static ItemStack buildRotationItem(Rotatable data) {
        int index = ROTATIONS.indexOf(data.getRotation());
        String current = data.getRotation().name() + " &7(" + index + "/15)";
        List<String> lore = List.of(
                ChatUtils.translateToColor("&7Current: &a" + current),
                ChatUtils.translateToColor("&7Cycles through 16 rotation steps."),
                "",
                ChatUtils.translateToColor("&eClick &7to cycle")
        );
        return makePropertyItem(Material.CLOCK, "Rotation", lore);
    }

    private static ItemStack makePropertyItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(ChatUtils.translateToColor("&e" + name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(line.startsWith("\u00A7") || line.isEmpty() ? line : ChatUtils.translateToColor(line));
        }
        m.setLore(colored);
        item.setItemMeta(m);
        return item;
    }
}
