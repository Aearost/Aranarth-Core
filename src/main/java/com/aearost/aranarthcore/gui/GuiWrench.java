package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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

    public static final String TITLE_KEY = "gui.wrench.title";

    public static final Map<UUID, Block> openBlocks = new HashMap<>();

    // Cardinal + vertical faces used for fence/wall connection ordering
    public static final List<BlockFace> CARDINAL_FACES = List.of(
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN
    );

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
        this.gui = Bukkit.createInventory(player, 27, Lang.getFor(player, TITLE_KEY));
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
        if (!ChatUtils.stripColorFormatting(player.getOpenInventory().getTitle()).equals(Lang.getFor(player, TITLE_KEY))) {
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
                Lang.get("gui.wrench.click_info")
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

        // Wall heights and post
        if (data instanceof Wall wall) {
            items.add(buildWallHeightItem(wall, BlockFace.NORTH));
            items.add(buildWallHeightItem(wall, BlockFace.EAST));
            items.add(buildWallHeightItem(wall, BlockFace.SOUTH));
            items.add(buildWallHeightItem(wall, BlockFace.WEST));
            items.add(buildWallUpItem(wall));
        }

        // Fence connections
        if (data instanceof MultipleFacing multiFacing) {
            for (BlockFace face : CARDINAL_FACES) {
                if (multiFacing.getAllowedFaces().contains(face)) {
                    items.add(buildFaceConnectionItem(multiFacing, face));
                }
            }
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
        lore.add(Lang.get("gui.wrench.current", "value", current));
        lore.add(Lang.get("gui.wrench.available", "values", String.join(", ", available)));
        lore.add("");
        if (needsFloatCheck) {
            lore.add(Lang.get("gui.wrench.float_warn1"));
            lore.add(Lang.get("gui.wrench.float_warn2"));
            lore.add("");
        }
        lore.add(Lang.get("gui.wrench.cycle"));
        return makePropertyItem(Material.COMPASS, Lang.get("gui.wrench.prop_facing"), lore);
    }

    private static ItemStack buildAxisItem(Orientable data) {
        List<String> lore = List.of(
                Lang.get("gui.wrench.current", "value", data.getAxis().name()),
                Lang.get("gui.wrench.available", "values", "X, Y, Z"),
                "",
                Lang.get("gui.wrench.cycle")
        );
        return makePropertyItem(Material.STICK, Lang.get("gui.wrench.prop_axis"), lore);
    }

    private static ItemStack buildShapeItem(Stairs data) {
        String current = data.getShape().name().replace('_', ' ');
        List<String> lore = List.of(
                Lang.get("gui.wrench.current", "value", current),
                Lang.get("gui.wrench.available", "values", "STRAIGHT, INNER LEFT, INNER RIGHT, OUTER LEFT, OUTER RIGHT"),
                "",
                Lang.get("gui.wrench.cycle")
        );
        return makePropertyItem(Material.STONE_STAIRS, Lang.get("gui.wrench.prop_shape"), lore);
    }

    private static ItemStack buildHalfItem(Bisected data) {
        String current = data.getHalf().name();
        List<String> lore = List.of(
                Lang.get("gui.wrench.current", "value", current),
                Lang.get("gui.wrench.available", "values", "BOTTOM, TOP"),
                "",
                Lang.get("gui.wrench.cycle")
        );
        return makePropertyItem(Material.STONE_SLAB, Lang.get("gui.wrench.prop_half"), lore);
    }

    private static ItemStack buildSlabTypeItem(Slab data) {
        String current = data.getType().name();
        List<String> lore = List.of(
                Lang.get("gui.wrench.current", "value", current),
                Lang.get("gui.wrench.available", "values", "BOTTOM, TOP"),
                "",
                Lang.get("gui.wrench.cycle")
        );
        return makePropertyItem(Material.STONE_SLAB, Lang.get("gui.wrench.prop_slab"), lore);
    }

    private static ItemStack buildAttachmentItem(Switch data) {
        String current = data.getFace().name();
        List<String> lore = List.of(
                Lang.get("gui.wrench.current", "value", current),
                Lang.get("gui.wrench.available", "values", "FLOOR, WALL, CEILING"),
                "",
                Lang.get("gui.wrench.attach_warn"),
                "",
                Lang.get("gui.wrench.cycle")
        );
        return makePropertyItem(Material.STONE_BUTTON, Lang.get("gui.wrench.prop_attachment"), lore);
    }

    private static ItemStack buildWallDirectionItem(Switch data) {
        String current = data.getFacing().name();
        List<String> available = new ArrayList<>();
        for (BlockFace f : data.getFaces()) {
            available.add(f.name());
        }
        List<String> lore = new ArrayList<>();
        lore.add(Lang.get("gui.wrench.current", "value", current));
        lore.add(Lang.get("gui.wrench.available", "values", String.join(", ", available)));
        lore.add("");
        lore.add(Lang.get("gui.wrench.wall_dir_note"));
        lore.add(Lang.get("gui.wrench.wall_dir_warn"));
        lore.add("");
        lore.add(Lang.get("gui.wrench.cycle"));
        return makePropertyItem(Material.COMPASS, Lang.get("gui.wrench.prop_wall_dir"), lore);
    }

    private static ItemStack buildHingeItem(Door data) {
        String current = data.getHinge().name();
        List<String> lore = List.of(
                Lang.get("gui.wrench.current", "value", current),
                Lang.get("gui.wrench.available", "values", "LEFT, RIGHT"),
                "",
                Lang.get("gui.wrench.cycle")
        );
        return makePropertyItem(Material.IRON_DOOR, Lang.get("gui.wrench.prop_hinge"), lore);
    }

    private static ItemStack buildOpenItem(Openable data) {
        String current = data.isOpen() ? "OPEN" : "CLOSED";
        List<String> lore = List.of(
                Lang.get("gui.wrench.current", "value", current),
                Lang.get("gui.wrench.available", "values", "OPEN, CLOSED"),
                "",
                Lang.get("gui.wrench.toggle")
        );
        return makePropertyItem(Material.OAK_TRAPDOOR, Lang.get("gui.wrench.prop_open"), lore);
    }

    private static ItemStack buildInWallItem(Gate data) {
        String current = data.isInWall() ? "TRUE" : "FALSE";
        List<String> lore = List.of(
                Lang.get("gui.wrench.current", "value", current),
                Lang.get("gui.wrench.available", "values", "TRUE, FALSE"),
                "",
                Lang.get("gui.wrench.toggle")
        );
        return makePropertyItem(Material.COBBLESTONE_WALL, Lang.get("gui.wrench.prop_in_wall"), lore);
    }

    private static ItemStack buildRotationItem(Rotatable data) {
        int index = ROTATIONS.indexOf(data.getRotation());
        String current = data.getRotation().name() + " (" + index + "/15)";
        List<String> lore = List.of(
                Lang.get("gui.wrench.current", "value", current),
                Lang.get("gui.wrench.rotation_cycles"),
                "",
                Lang.get("gui.wrench.cycle")
        );
        return makePropertyItem(Material.CLOCK, Lang.get("gui.wrench.prop_rotation"), lore);
    }

    private static ItemStack buildWallHeightItem(Wall data, BlockFace face) {
        String langKey = switch (face) {
            case SOUTH -> "gui.wrench.prop_south_height";
            case EAST  -> "gui.wrench.prop_east_height";
            case WEST  -> "gui.wrench.prop_west_height";
            default    -> "gui.wrench.prop_north_height";
        };
        String current = data.getHeight(face).name();
        List<String> lore = List.of(
                Lang.get("gui.wrench.current", "value", current),
                Lang.get("gui.wrench.available", "values", "NONE, LOW, TALL"),
                "",
                Lang.get("gui.wrench.cycle")
        );
        return makePropertyItem(Material.COBBLESTONE_WALL, Lang.get(langKey), lore);
    }

    private static ItemStack buildWallUpItem(Wall data) {
        String current = data.isUp() ? "TRUE" : "FALSE";
        List<String> lore = List.of(
                Lang.get("gui.wrench.current", "value", current),
                Lang.get("gui.wrench.available", "values", "TRUE, FALSE"),
                "",
                Lang.get("gui.wrench.toggle")
        );
        return makePropertyItem(Material.COBBLESTONE_WALL, Lang.get("gui.wrench.prop_wall_up"), lore);
    }

    private static ItemStack buildFaceConnectionItem(MultipleFacing data, BlockFace face) {
        String langKey = switch (face) {
            case SOUTH -> "gui.wrench.prop_south_conn";
            case EAST  -> "gui.wrench.prop_east_conn";
            case WEST  -> "gui.wrench.prop_west_conn";
            case UP    -> "gui.wrench.prop_up_conn";
            case DOWN  -> "gui.wrench.prop_down_conn";
            default    -> "gui.wrench.prop_north_conn";
        };
        String current = data.hasFace(face) ? "CONNECTED" : "DISCONNECTED";
        List<String> lore = List.of(
                Lang.get("gui.wrench.current", "value", current),
                Lang.get("gui.wrench.available", "values", "CONNECTED, DISCONNECTED"),
                "",
                Lang.get("gui.wrench.toggle")
        );
        return makePropertyItem(Material.OAK_FENCE, Lang.get(langKey), lore);
    }

    private static ItemStack makePropertyItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(name);
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }
}
