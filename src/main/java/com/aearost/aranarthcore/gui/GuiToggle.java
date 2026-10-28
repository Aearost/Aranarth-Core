package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.enums.FireType;
import com.aearost.aranarthcore.event.listener.misc.InvisibleArmorManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Perk;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.GateUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiToggle {

    private final Player player;
    private final Inventory initializedGui;

    public GuiToggle(Player player) {
        this.player = player;
        this.initializedGui = initializeGui(player);
    }

    public void openGui() {
        player.closeInventory();
        if (initializedGui != null) {
            player.openInventory(initializedGui);
        }
    }

    private Inventory initializeGui(Player player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        Inventory gui = GuiHolder.create(player, 54, Lang.getFor(player, "gui.toggle.title"));

        ItemStack blank = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta blankMeta = blank.getItemMeta();
        blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
        blank.setItemMeta(blankMeta);

        // Top filler row (0-8)
        for (int i = 0; i <= 8; i++) {
            gui.setItem(i, blank);
        }
        // Row 1 edge fillers (9, 17)
        gui.setItem(9, blank);
        gui.setItem(17, blank);
        // Row 2 edge fillers (18, 26)
        gui.setItem(18, blank);
        gui.setItem(26, blank);
        // Row 3 edge fillers (27, 35)
        gui.setItem(27, blank);
        gui.setItem(35, blank);
        // Row 4 edge fillers (36, 44)
        gui.setItem(36, blank);
        gui.setItem(44, blank);
        // Bottom filler row (45-53)
        for (int i = 45; i <= 53; i++) {
            gui.setItem(i, blank);
        }

        ItemStack exit = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName(Lang.getFor(player, "gui.exit"));
        exit.setItemMeta(exitMeta);
        gui.setItem(49, exit);

        // Row 1: slots 10-16
        // Blacklist
        if (player.hasPermission("aranarth.blacklist")) {
            boolean active = aranarthPlayer.getBlacklistingMethod() != -1;
            gui.setItem(10, buildToggleItem(player, Material.LAVA_BUCKET, Lang.getFor(player, "gui.toggle.blacklist"), active));
        } else {
            gui.setItem(10, buildLockedItem(player, Material.LAVA_BUCKET, "&f&lBlacklist"));
        }

        // Fire Type
        if (hasAnyFirePerk(aranarthPlayer)) {
            gui.setItem(11, buildFireTypeItem(player, aranarthPlayer.getFireType()));
        } else {
            gui.setItem(11, buildLockedItem(player, Material.CAMPFIRE, Lang.getFor(player, "gui.toggle.fire_type")));
        }

        // Bulk Sell Shulker
        if (player.hasPermission("aranarth.shulker")) {
            gui.setItem(12, buildToggleItem(player, Material.PURPLE_SHULKER_BOX, Lang.getFor(player, "gui.toggle.bulk_sell"), aranarthPlayer.isBulkSellShulkerEnabled()));
        } else {
            gui.setItem(12, buildLockedItem(player, Material.PURPLE_SHULKER_BOX, "&f&lBulk Sell Shulker"));
        }

        // Dominion Claim Messages
        gui.setItem(13, buildToggleItem(player, Material.WHITE_BANNER, Lang.getFor(player, "gui.toggle.claim_messages"), !aranarthPlayer.isTogglingChangeClaim()));

        // Chat
        if (player.hasPermission("aranarth.toggle.chat")) {
            gui.setItem(14, buildToggleItem(player, Material.WRITTEN_BOOK, Lang.getFor(player, "gui.toggle.chat"), !aranarthPlayer.isTogglingChat()));
        } else {
            gui.setItem(14, buildLockedItem(player, Material.WRITTEN_BOOK, "&f&lChat"));
        }

        // Chest Lock
        gui.setItem(15, buildToggleItem(player, Material.TRIAL_KEY, Lang.getFor(player, "gui.toggle.chest_lock"), aranarthPlayer.isAutoLockingChests()));

        // Compressor
        if (player.hasPermission("aranarth.compressor")) {
            gui.setItem(16, buildToggleItem(player, Material.PISTON, Lang.getFor(player, "gui.toggle.compressor"), aranarthPlayer.isCompressingItems()));
        } else {
            gui.setItem(16, buildLockedItem(player, Material.PISTON, "&f&lCompressor"));
        }

        // Row 2: slots 19-25
        // Day Message
        gui.setItem(19, buildToggleItem(player, Material.CLOCK, Lang.getFor(player, "gui.toggle.day_message"), !aranarthPlayer.isDayMessageDisabled()));

        // Gate Creation
        if (player.hasPermission("aranarth.gate")) {
            gui.setItem(20, buildToggleItem(player, Material.IRON_BARS, Lang.getFor(player, "gui.toggle.gate"), GateUtils.isInGatePlacementMode(player.getUniqueId())));
        } else {
            gui.setItem(20, buildLockedItem(player, Material.IRON_BARS, "&f&lGate Creation"));
        }

        // Gradient Chat
        boolean hasGradientAccess = aranarthPlayer.getPerks().containsKey(Perk.CHAT) || aranarthPlayer.getSaintRank() >= 2;
        if (hasGradientAccess) {
            gui.setItem(21, buildToggleItem(player, Material.ORANGE_GLAZED_TERRACOTTA, Lang.getFor(player, "gui.toggle.gradient_chat"), aranarthPlayer.isGradientChatEnabled()));
        } else {
            gui.setItem(21, buildLockedItem(player, Material.ORANGE_GLAZED_TERRACOTTA, "&f&lGradient Chat"));
        }

        // Inventory Assist
        if (player.hasPermission("aranarth.inventory")) {
            gui.setItem(22, buildToggleItem(player, Material.CHEST, Lang.getFor(player, "gui.toggle.inventory_assist"), !aranarthPlayer.isTogglingInventoryAssist()));
        } else {
            gui.setItem(22, buildLockedItem(player, Material.CHEST, "&f&lInventory Assist"));
        }

        // Private Messages
        if (player.hasPermission("aranarth.toggle.msg")) {
            gui.setItem(23, buildToggleItem(player, Material.PAPER, Lang.getFor(player, "gui.toggle.private_messages"), !aranarthPlayer.isTogglingMessages()));
        } else {
            gui.setItem(23, buildLockedItem(player, Material.PAPER, "&f&lPrivate Messages"));
        }

        // Pet Hurt
        gui.setItem(24, buildToggleItem(player, Material.NAME_TAG, Lang.getFor(player, "gui.toggle.pet_hurt"), aranarthPlayer.isHurtingOwnPets()));

        // Shulker Assist
        if (player.hasPermission("aranarth.shulker")) {
            gui.setItem(25, buildToggleItem(player, Material.SHULKER_BOX, Lang.getFor(player, "gui.toggle.shulker_assist"), aranarthPlayer.isAddingToShulker()));
        } else {
            gui.setItem(25, buildLockedItem(player, Material.SHULKER_BOX, "&f&lShulker Assist"));
        }

        // Row 3: slots 28-34
        // Spawn Boost
        gui.setItem(28, buildToggleItem(player, Material.FEATHER, Lang.getFor(player, "gui.toggle.spawn_boost"), aranarthPlayer.isUsingSpawnBoost()));

        // Teleport Requests
        if (player.hasPermission("aranarth.toggle.tp")) {
            gui.setItem(29, buildToggleItem(player, Material.ENDER_PEARL, Lang.getFor(player, "gui.toggle.tp_requests"), !aranarthPlayer.isTogglingTp()));
        } else {
            gui.setItem(29, buildLockedItem(player, Material.ENDER_PEARL, "&f&lTeleport Requests"));
        }

        // Weather Messages
        gui.setItem(30, buildToggleItem(player, Material.WIND_CHARGE, Lang.getFor(player, "gui.toggle.weather_messages"), !aranarthPlayer.isWeatherMessageDisabled()));

        // Dominion Msg Compact
        gui.setItem(31, buildToggleItem(player, Material.COMPASS, Lang.getFor(player, "gui.toggle.dominion_compact"), aranarthPlayer.isDominionMsgCompact()));

        // Interactive Chat
        boolean hasInteractivePerm = aranarthPlayer.getSaintRank() >= 2 || aranarthPlayer.getCouncilRank() > 0;
        if (hasInteractivePerm) {
            gui.setItem(32, buildToggleItem(player, Material.RECOVERY_COMPASS, Lang.getFor(player, "gui.toggle.interactive_chat"), aranarthPlayer.isInteractiveChatEnabled()));
        } else {
            gui.setItem(32, buildLockedItem(player, Material.RECOVERY_COMPASS, "&f&lInteractive Chat"));
        }

        // Emoji
        gui.setItem(33, buildToggleItem(player, Material.HEART_OF_THE_SEA, Lang.getFor(player, "gui.toggle.emoji"), aranarthPlayer.isEmojiEnabled()));

        // Size Scale
        gui.setItem(34, buildToggleItem(player, Material.POPPED_CHORUS_FRUIT, Lang.getFor(player, "gui.toggle.size_scale"), aranarthPlayer.isSizeScaleEnabled()));

        // Invisible Armor
        if (player.hasPermission("aranarth.invisiblearmor")) {
            gui.setItem(37, buildToggleItem(player, Material.IRON_CHESTPLATE, Lang.getFor(player, "gui.toggle.invisible_armor"), InvisibleArmorManager.isArmorHidden(player.getUniqueId())));
        } else {
            gui.setItem(37, buildLockedItem(player, Material.IRON_CHESTPLATE, "&f&lInvisible Armor"));
        }

        // Server Tips
        gui.setItem(38, buildToggleItem(player, Material.KNOWLEDGE_BOOK, Lang.getFor(player, "gui.toggle.server_tips"), !aranarthPlayer.isServerTipsDisabled()));

        // Sit
        if (aranarthPlayer.getSaintRank() >= 1 || aranarthPlayer.getCouncilRank() >= 1) {
            gui.setItem(39, buildToggleItem(player, Material.WHITE_CUSHION, Lang.getFor(player, "gui.toggle.sit"), !aranarthPlayer.isSitDisabled()));
        } else {
            gui.setItem(39, buildLockedItem(player, Material.WHITE_CUSHION, Lang.getFor(player, "gui.toggle.sit")));
        }

        // Jobs Earnings Message
        gui.setItem(40, buildToggleItem(player, Material.GOLD_INGOT, Lang.getFor(player, "gui.toggle.jobs_message"), !aranarthPlayer.isJobsMessageDisabled()));

        return gui;
    }

    public static boolean hasAnyFirePerk(AranarthPlayer aranarthPlayer) {
        return (aranarthPlayer.getPerks().getOrDefault(Perk.BLUEFIRE, 0) == 1)
                || (aranarthPlayer.getPerks().getOrDefault(Perk.WHITEFIRE, 0) == 1)
                || (aranarthPlayer.getPerks().getOrDefault(Perk.PRISMATICFIRE, 0) == 1)
;
    }

    public static ItemStack buildFireTypeItem(Player player, FireType type) {
        ItemStack item = new ItemStack(Material.CAMPFIRE);
        ItemMeta meta = item.getItemMeta();
        String displayName = switch (type) {
            case DEFAULT -> Lang.getFor(player, "gui.toggle.fire_regular");
            case BLUE -> Lang.getFor(player, "gui.toggle.fire_blue");
            case WHITE -> Lang.getFor(player, "gui.toggle.fire_white");
            case PRISMATIC -> ChatUtils.translateToGradient(
                    "#EB5A5A,#EB8C5A,#EBEB5A,#5ACA5A,#5A6EEB,#9B5ACA", "Prismatic Fire", true);
        };
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildToggleItem(Player player, Material material, String name, boolean active) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String status = active ? Lang.getFor(player, "gui.active") : Lang.getFor(player, "gui.inactive");
        meta.setDisplayName(name + " " + Lang.getFor(player, "gui.separator") + " " + status);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildLockedItem(Player player, Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name + " " + Lang.getFor(player, "gui.separator") + " " + Lang.getFor(player, "gui.locked"));
        item.setItemMeta(meta);
        return item;
    }

}
