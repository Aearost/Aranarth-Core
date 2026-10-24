package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
        this.gui = GuiHolder.create(player, 27, Lang.getFor(player, TITLE_KEY));
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
        if (!GuiHolder.isGui(player.getOpenInventory())
                || !ChatUtils.stripColorFormatting(player.getOpenInventory().getTitle()).equals(Lang.getFor(player, TITLE_KEY))) {
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
        List<ItemStack> props = buildPropertyItems(player, data);

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
                Lang.getFor(player, "gui.wrench.click_info")
        ));
        item.setItemMeta(m);
        return item;
    }

    public static List<ItemStack> buildPropertyItems(Player player, BlockData data) {
        List<ItemStack> items = new ArrayList<>();

        // Facing
        if (!(data instanceof Switch) && data instanceof Directional directional) {
            boolean needsFloatCheck = data instanceof TrapDoor;
            items.add(buildFacingItem(player, directional, needsFloatCheck));
        }

        // Axis
        if (data instanceof Orientable orientable) {
            items.add(buildAxisItem(player, orientable));
        }

        // Stair shape
        if (data instanceof Stairs stairs) {
            items.add(buildShapeItem(player, stairs));
        }

        // Half
        if ((data instanceof Stairs || data instanceof TrapDoor) && data instanceof Bisected bisected) {
            items.add(buildHalfItem(player, bisected));
        }

        // Type
        if (data instanceof Slab slab) {
            items.add(buildSlabTypeItem(player, slab));
        }

        // Lever
        if (data instanceof Switch switchData) {
            items.add(buildAttachmentItem(player, switchData));
            items.add(buildWallDirectionItem(player, switchData));
        }

        // Hinge
        if (data instanceof Door door) {
            items.add(buildHingeItem(player, door));
        }

        // Open
        if (data instanceof Openable openable) {
            items.add(buildOpenItem(player, openable));
        }

        // Fence gate wall
        if (data instanceof Gate gate) {
            items.add(buildInWallItem(player, gate));
        }

        // Rotation
        if (data instanceof Rotatable rotatable) {
            items.add(buildRotationItem(player, rotatable));
        }

        // Wall heights and post
        if (data instanceof Wall wall) {
            items.add(buildWallHeightItem(player, wall, BlockFace.NORTH));
            items.add(buildWallHeightItem(player, wall, BlockFace.EAST));
            items.add(buildWallHeightItem(player, wall, BlockFace.SOUTH));
            items.add(buildWallHeightItem(player, wall, BlockFace.WEST));
            items.add(buildWallUpItem(player, wall));
        }

        // Fence connections
        if (data instanceof MultipleFacing multiFacing) {
            for (BlockFace face : CARDINAL_FACES) {
                if (multiFacing.getAllowedFaces().contains(face)) {
                    items.add(buildFaceConnectionItem(player, multiFacing, face));
                }
            }
        }

        return items;
    }


    private static ItemStack buildFacingItem(Player player, Directional data, boolean needsFloatCheck) {
        String current = data.getFacing().name();
        List<String> available = new ArrayList<>();
        for (BlockFace f : data.getFaces()) {
            available.add(f.name());
        }
        List<String> lore = new ArrayList<>();
        lore.add(Lang.getFor(player, "gui.wrench.current", "value", current));
        lore.add(Lang.getFor(player, "gui.wrench.available", "values", String.join(", ", available)));
        lore.add("");
        if (needsFloatCheck) {
            lore.add(Lang.getFor(player, "gui.wrench.float_warn1"));
            lore.add(Lang.getFor(player, "gui.wrench.float_warn2"));
            lore.add("");
        }
        lore.add(Lang.getFor(player, "gui.wrench.cycle"));
        return makePropertyItem(Material.COMPASS, Lang.getFor(player, "gui.wrench.prop_facing"), lore);
    }

    private static ItemStack buildAxisItem(Player player, Orientable data) {
        List<String> lore = List.of(
                Lang.getFor(player, "gui.wrench.current", "value", data.getAxis().name()),
                Lang.getFor(player, "gui.wrench.available", "values", "X, Y, Z"),
                "",
                Lang.getFor(player, "gui.wrench.cycle")
        );
        return makePropertyItem(Material.STICK, Lang.getFor(player, "gui.wrench.prop_axis"), lore);
    }

    private static ItemStack buildShapeItem(Player player, Stairs data) {
        String current = data.getShape().name().replace('_', ' ');
        List<String> lore = List.of(
                Lang.getFor(player, "gui.wrench.current", "value", current),
                Lang.getFor(player, "gui.wrench.available", "values", "STRAIGHT, INNER LEFT, INNER RIGHT, OUTER LEFT, OUTER RIGHT"),
                "",
                Lang.getFor(player, "gui.wrench.cycle")
        );
        return makePropertyItem(Material.STONE_STAIRS, Lang.getFor(player, "gui.wrench.prop_shape"), lore);
    }

    private static ItemStack buildHalfItem(Player player, Bisected data) {
        String current = data.getHalf().name();
        List<String> lore = List.of(
                Lang.getFor(player, "gui.wrench.current", "value", current),
                Lang.getFor(player, "gui.wrench.available", "values", "BOTTOM, TOP"),
                "",
                Lang.getFor(player, "gui.wrench.cycle")
        );
        return makePropertyItem(Material.STONE_SLAB, Lang.getFor(player, "gui.wrench.prop_half"), lore);
    }

    private static ItemStack buildSlabTypeItem(Player player, Slab data) {
        String current = data.getType().name();
        List<String> lore = List.of(
                Lang.getFor(player, "gui.wrench.current", "value", current),
                Lang.getFor(player, "gui.wrench.available", "values", "BOTTOM, TOP"),
                "",
                Lang.getFor(player, "gui.wrench.cycle")
        );
        return makePropertyItem(Material.STONE_SLAB, Lang.getFor(player, "gui.wrench.prop_slab"), lore);
    }

    private static ItemStack buildAttachmentItem(Player player, Switch data) {
        String current = data.getFace().name();
        List<String> lore = List.of(
                Lang.getFor(player, "gui.wrench.current", "value", current),
                Lang.getFor(player, "gui.wrench.available", "values", "FLOOR, WALL, CEILING"),
                "",
                Lang.getFor(player, "gui.wrench.attach_warn"),
                "",
                Lang.getFor(player, "gui.wrench.cycle")
        );
        return makePropertyItem(Material.STONE_BUTTON, Lang.getFor(player, "gui.wrench.prop_attachment"), lore);
    }

    private static ItemStack buildWallDirectionItem(Player player, Switch data) {
        String current = data.getFacing().name();
        List<String> available = new ArrayList<>();
        for (BlockFace f : data.getFaces()) {
            available.add(f.name());
        }
        List<String> lore = new ArrayList<>();
        lore.add(Lang.getFor(player, "gui.wrench.current", "value", current));
        lore.add(Lang.getFor(player, "gui.wrench.available", "values", String.join(", ", available)));
        lore.add("");
        lore.add(Lang.getFor(player, "gui.wrench.wall_dir_note"));
        lore.add(Lang.getFor(player, "gui.wrench.wall_dir_warn"));
        lore.add("");
        lore.add(Lang.getFor(player, "gui.wrench.cycle"));
        return makePropertyItem(Material.COMPASS, Lang.getFor(player, "gui.wrench.prop_wall_dir"), lore);
    }

    private static ItemStack buildHingeItem(Player player, Door data) {
        String current = data.getHinge().name();
        List<String> lore = List.of(
                Lang.getFor(player, "gui.wrench.current", "value", current),
                Lang.getFor(player, "gui.wrench.available", "values", "LEFT, RIGHT"),
                "",
                Lang.getFor(player, "gui.wrench.cycle")
        );
        return makePropertyItem(Material.IRON_DOOR, Lang.getFor(player, "gui.wrench.prop_hinge"), lore);
    }

    private static ItemStack buildOpenItem(Player player, Openable data) {
        String current = data.isOpen() ? "OPEN" : "CLOSED";
        List<String> lore = List.of(
                Lang.getFor(player, "gui.wrench.current", "value", current),
                Lang.getFor(player, "gui.wrench.available", "values", "OPEN, CLOSED"),
                "",
                Lang.getFor(player, "gui.wrench.toggle")
        );
        return makePropertyItem(Material.OAK_TRAPDOOR, Lang.getFor(player, "gui.wrench.prop_open"), lore);
    }

    private static ItemStack buildInWallItem(Player player, Gate data) {
        String current = data.isInWall() ? "TRUE" : "FALSE";
        List<String> lore = List.of(
                Lang.getFor(player, "gui.wrench.current", "value", current),
                Lang.getFor(player, "gui.wrench.available", "values", "TRUE, FALSE"),
                "",
                Lang.getFor(player, "gui.wrench.toggle")
        );
        return makePropertyItem(Material.COBBLESTONE_WALL, Lang.getFor(player, "gui.wrench.prop_in_wall"), lore);
    }

    private static ItemStack buildRotationItem(Player player, Rotatable data) {
        int index = ROTATIONS.indexOf(data.getRotation());
        String current = data.getRotation().name() + " (" + index + "/15)";
        List<String> lore = List.of(
                Lang.getFor(player, "gui.wrench.current", "value", current),
                Lang.getFor(player, "gui.wrench.rotation_cycles"),
                "",
                Lang.getFor(player, "gui.wrench.cycle")
        );
        return makePropertyItem(Material.CLOCK, Lang.getFor(player, "gui.wrench.prop_rotation"), lore);
    }

    private static ItemStack buildWallHeightItem(Player player, Wall data, BlockFace face) {
        String langKey = switch (face) {
            case SOUTH -> "gui.wrench.prop_south_height";
            case EAST  -> "gui.wrench.prop_east_height";
            case WEST  -> "gui.wrench.prop_west_height";
            default    -> "gui.wrench.prop_north_height";
        };
        String current = data.getHeight(face).name();
        List<String> lore = List.of(
                Lang.getFor(player, "gui.wrench.current", "value", current),
                Lang.getFor(player, "gui.wrench.available", "values", "NONE, LOW, TALL"),
                "",
                Lang.getFor(player, "gui.wrench.cycle")
        );
        return makePropertyItem(Material.COBBLESTONE_WALL, Lang.getFor(player, langKey), lore);
    }

    private static ItemStack buildWallUpItem(Player player, Wall data) {
        String current = data.isUp() ? "TRUE" : "FALSE";
        List<String> lore = List.of(
                Lang.getFor(player, "gui.wrench.current", "value", current),
                Lang.getFor(player, "gui.wrench.available", "values", "TRUE, FALSE"),
                "",
                Lang.getFor(player, "gui.wrench.toggle")
        );
        return makePropertyItem(Material.COBBLESTONE_WALL, Lang.getFor(player, "gui.wrench.prop_wall_up"), lore);
    }

    private static ItemStack buildFaceConnectionItem(Player player, MultipleFacing data, BlockFace face) {
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
                Lang.getFor(player, "gui.wrench.current", "value", current),
                Lang.getFor(player, "gui.wrench.available", "values", "CONNECTED, DISCONNECTED"),
                "",
                Lang.getFor(player, "gui.wrench.toggle")
        );
        return makePropertyItem(Material.OAK_FENCE, Lang.getFor(player, langKey), lore);
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
