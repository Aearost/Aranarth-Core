package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.commands.general.CommandMcstats;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GuiMcstats {

    public static final String TITLE_SELF_KEY = "gui.mcstats.title_self";
    public static final String TITLE_SUFFIX_KEY = "gui.mcstats.title_suffix";

    // Skill display order per section - null entries leave that slot empty
    private static final String[] GATHERING_ORDER   = {"MINING", "WOODCUTTING", "EXCAVATION", "HERBALISM", "FISHING"};
    private static final int[]    GATHERING_SLOTS    = {11, 12, 13, 14, 15};

    private static final String[] COMBAT_ROW1_ORDER  = {"SWORDS", "AXES", "ARCHERY", "SPEARS", "UNARMED"};
    private static final int[]    COMBAT_ROW1_SLOTS   = {20, 21, 22, 23, 24};

    private static final String[] COMBAT_ROW2_ORDER  = {"TRIDENTS", "MACES", null, "CROSSBOWS", "TAMING"};
    private static final int[]    COMBAT_ROW2_SLOTS   = {29, 30, 31, 32, 33};

    private static final String[] MISC_ORDER         = {"ALCHEMY", "ACROBATICS", "REPAIR"};
    private static final int[]    MISC_SLOTS          = {39, 40, 41};

    private final Player viewer;
    private final Inventory gui;

    public GuiMcstats(Player viewer, String targetName, boolean isSelf,
                      Map<PrimarySkillType, Integer> targetLevels, int targetPowerLevel,
                      Map<PrimarySkillType, Integer> targetRanks, int targetOverallRank,
                      Map<PrimarySkillType, Integer> viewerRanks, int viewerOverallRank) {
        this.viewer = viewer;
        String title = isSelf ? Lang.getFor(viewer, TITLE_SELF_KEY) : (targetName + Lang.getFor(viewer, TITLE_SUFFIX_KEY));
        this.gui = initializeGui(title, isSelf, targetLevels, targetPowerLevel,
                targetRanks, targetOverallRank, viewerRanks, viewerOverallRank);
    }

    public void openGui() {
        viewer.closeInventory();
        viewer.openInventory(gui);
    }

    private static Material getSkillMaterial(PrimarySkillType skill) {
        return switch (skill.name()) {
            case "ACROBATICS" -> Material.FEATHER;
            case "ALCHEMY" -> Material.BREWING_STAND;
            case "ARCHERY" -> Material.BOW;
            case "AXES" -> Material.IRON_AXE;
            case "CROSSBOWS" -> Material.CROSSBOW;
            case "EXCAVATION" -> Material.IRON_SHOVEL;
            case "FISHING" -> Material.FISHING_ROD;
            case "HERBALISM" -> Material.WHEAT;
            case "MACES" -> Material.MACE;
            case "MINING" -> Material.IRON_PICKAXE;
            case "REPAIR" -> Material.ANVIL;
            case "SPEARS" -> Material.ARROW;
            case "SWORDS" -> Material.IRON_SWORD;
            case "TAMING" -> Material.BONE;
            case "TRIDENTS" -> Material.TRIDENT;
            case "UNARMED" -> Material.IRON_INGOT;
            case "WOODCUTTING" -> Material.OAK_LOG;
            default -> Material.PAPER;
        };
    }

    private static String formatRank(int rank) {
        return rank > 0 ? "#" + rank : "Unranked";
    }

    private static String toDisplayName(PrimarySkillType skill) {
        String name = skill.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    private static PrimarySkillType findSkill(String name) {
        return CommandMcstats.DISPLAY_SKILLS.stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    private Inventory initializeGui(String title, boolean isSelf,
                                    Map<PrimarySkillType, Integer> targetLevels, int targetPowerLevel,
                                    Map<PrimarySkillType, Integer> targetRanks, int targetOverallRank,
                                    Map<PrimarySkillType, Integer> viewerRanks, int viewerOverallRank) {
        Inventory inv = Bukkit.createInventory(viewer, 54, title);

        // Top and bottom border rows only
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (Objects.nonNull(glassMeta)) {
            glassMeta.setDisplayName(ChatUtils.translateToColor("&f"));
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        // Power level display
        ItemStack powerItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta powerMeta = powerItem.getItemMeta();
        if (Objects.nonNull(powerMeta)) {
            powerMeta.setDisplayName(Lang.getFor(viewer, "gui.mcstats.power"));
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getFor(viewer, "gui.mcstats.total_power", "level", String.valueOf(targetPowerLevel)));
            lore.add(Lang.getFor(viewer, "gui.mcstats.rank", "rank", formatRank(targetOverallRank)));
            if (!isSelf) {
                lore.add(Lang.getFor(viewer, "gui.mcstats.your_rank", "rank", formatRank(viewerOverallRank)));
            }
            powerMeta.setLore(lore);
            powerItem.setItemMeta(powerMeta);
        }
        inv.setItem(4, powerItem);

        // Exit button
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta barrierMeta = barrier.getItemMeta();
        if (Objects.nonNull(barrierMeta)) {
            barrierMeta.setDisplayName(Lang.getFor(viewer, "gui.exit"));
            barrier.setItemMeta(barrierMeta);
        }
        inv.setItem(49, barrier);

        // Place each section
        placeSection(inv, GATHERING_ORDER,   GATHERING_SLOTS,   isSelf, targetLevels, targetRanks, viewerRanks);
        placeSection(inv, COMBAT_ROW1_ORDER, COMBAT_ROW1_SLOTS, isSelf, targetLevels, targetRanks, viewerRanks);
        placeSection(inv, COMBAT_ROW2_ORDER, COMBAT_ROW2_SLOTS, isSelf, targetLevels, targetRanks, viewerRanks);
        placeSection(inv, MISC_ORDER,        MISC_SLOTS,        isSelf, targetLevels, targetRanks, viewerRanks);

        return inv;
    }

    private void placeSection(Inventory inv, String[] skillNames, int[] slots, boolean isSelf,
                              Map<PrimarySkillType, Integer> targetLevels,
                              Map<PrimarySkillType, Integer> targetRanks,
                              Map<PrimarySkillType, Integer> viewerRanks) {
        for (int i = 0; i < skillNames.length && i < slots.length; i++) {
            if (skillNames[i] == null) {
                continue;
            }
            PrimarySkillType skill = findSkill(skillNames[i]);
            if (skill == null) {
                continue;
            }

            int level = targetLevels.getOrDefault(skill, 0);
            int targetRank = targetRanks.getOrDefault(skill, 0);

            ItemStack item = new ItemStack(getSkillMaterial(skill));
            ItemMeta meta = item.getItemMeta();
            if (Objects.nonNull(meta)) {
                meta.setDisplayName(ChatUtils.translateToColor("&e&l" + toDisplayName(skill)));

                List<String> lore = new ArrayList<>();
                lore.add(Lang.getFor(viewer, "gui.mcstats.skill_level", "level", String.valueOf(level)));
                lore.add(Lang.getFor(viewer, "gui.mcstats.skill_rank", "rank", formatRank(targetRank)));
                if (!isSelf) {
                    int viewerRank = viewerRanks.getOrDefault(skill, 0);
                    lore.add(Lang.getFor(viewer, "gui.mcstats.skill_your_rank", "rank", formatRank(viewerRank)));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i], item);
        }
    }
}
