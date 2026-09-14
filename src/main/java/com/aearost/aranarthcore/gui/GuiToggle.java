package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.enums.FireType;
import com.aearost.aranarthcore.event.listener.misc.InvisibleArmorManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Perk;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.GateUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
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
        Inventory gui = Bukkit.getServer().createInventory(player, 54, Lang.getFor(player, "gui.toggle.title"));

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
            gui.setItem(10, buildToggleItem(Material.LAVA_BUCKET, Lang.getFor(player, "gui.toggle.blacklist"), active));
        } else {
            gui.setItem(10, buildLockedItem(Material.LAVA_BUCKET, "&f&lBlacklist"));
        }

        // Fire Type
        if (hasAnyFirePerk(aranarthPlayer)) {
            gui.setItem(11, buildFireTypeItem(aranarthPlayer.getFireType()));
        } else {
            gui.setItem(11, buildLockedItem(Material.CAMPFIRE, Lang.getFor(player, "gui.toggle.fire_type")));
        }

        // Bulk Sell Shulker
        if (player.hasPermission("aranarth.shulker")) {
            gui.setItem(12, buildToggleItem(Material.PURPLE_SHULKER_BOX, Lang.getFor(player, "gui.toggle.bulk_sell"), aranarthPlayer.isBulkSellShulkerEnabled()));
        } else {
            gui.setItem(12, buildLockedItem(Material.PURPLE_SHULKER_BOX, "&f&lBulk Sell Shulker"));
        }

        // Dominion Claim Messages
        gui.setItem(13, buildToggleItem(Material.WHITE_BANNER, Lang.getFor(player, "gui.toggle.claim_messages"), !aranarthPlayer.isTogglingChangeClaim()));

        // Chat
        if (player.hasPermission("aranarth.toggle.chat")) {
            gui.setItem(14, buildToggleItem(Material.WRITTEN_BOOK, Lang.getFor(player, "gui.toggle.chat"), !aranarthPlayer.isTogglingChat()));
        } else {
            gui.setItem(14, buildLockedItem(Material.WRITTEN_BOOK, "&f&lChat"));
        }

        // Chest Lock
        gui.setItem(15, buildToggleItem(Material.TRIAL_KEY, Lang.getFor(player, "gui.toggle.chest_lock"), aranarthPlayer.isAutoLockingChests()));

        // Compressor
        if (player.hasPermission("aranarth.compressor")) {
            gui.setItem(16, buildToggleItem(Material.PISTON, Lang.getFor(player, "gui.toggle.compressor"), aranarthPlayer.isCompressingItems()));
        } else {
            gui.setItem(16, buildLockedItem(Material.PISTON, "&f&lCompressor"));
        }

        // Row 2: slots 19-25
        // Day Message
        gui.setItem(19, buildToggleItem(Material.CLOCK, Lang.getFor(player, "gui.toggle.day_message"), !aranarthPlayer.isDayMessageDisabled()));

        // Gate Creation
        if (player.hasPermission("aranarth.gate")) {
            gui.setItem(20, buildToggleItem(Material.IRON_BARS, Lang.getFor(player, "gui.toggle.gate"), GateUtils.isInGatePlacementMode(player.getUniqueId())));
        } else {
            gui.setItem(20, buildLockedItem(Material.IRON_BARS, "&f&lGate Creation"));
        }

        // Gradient Chat
        boolean hasGradientAccess = aranarthPlayer.getPerks().containsKey(Perk.CHAT) || aranarthPlayer.getSaintRank() >= 2;
        if (hasGradientAccess) {
            gui.setItem(21, buildToggleItem(Material.ORANGE_GLAZED_TERRACOTTA, Lang.getFor(player, "gui.toggle.gradient_chat"), aranarthPlayer.isGradientChatEnabled()));
        } else {
            gui.setItem(21, buildLockedItem(Material.ORANGE_GLAZED_TERRACOTTA, "&f&lGradient Chat"));
        }

        // Inventory Assist
        if (player.hasPermission("aranarth.inventory")) {
            gui.setItem(22, buildToggleItem(Material.CHEST, Lang.getFor(player, "gui.toggle.inventory_assist"), !aranarthPlayer.isTogglingInventoryAssist()));
        } else {
            gui.setItem(22, buildLockedItem(Material.CHEST, "&f&lInventory Assist"));
        }

        // Private Messages
        if (player.hasPermission("aranarth.toggle.msg")) {
            gui.setItem(23, buildToggleItem(Material.PAPER, Lang.getFor(player, "gui.toggle.private_messages"), !aranarthPlayer.isTogglingMessages()));
        } else {
            gui.setItem(23, buildLockedItem(Material.PAPER, "&f&lPrivate Messages"));
        }

        // Pet Hurt
        gui.setItem(24, buildToggleItem(Material.NAME_TAG, Lang.getFor(player, "gui.toggle.pet_hurt"), aranarthPlayer.isHurtingOwnPets()));

        // Shulker Assist
        if (player.hasPermission("aranarth.shulker")) {
            gui.setItem(25, buildToggleItem(Material.SHULKER_BOX, Lang.getFor(player, "gui.toggle.shulker_assist"), aranarthPlayer.isAddingToShulker()));
        } else {
            gui.setItem(25, buildLockedItem(Material.SHULKER_BOX, "&f&lShulker Assist"));
        }

        // Row 3: slots 28-34
        // Spawn Boost
        gui.setItem(28, buildToggleItem(Material.FEATHER, Lang.getFor(player, "gui.toggle.spawn_boost"), aranarthPlayer.isUsingSpawnBoost()));

        // Teleport Requests
        if (player.hasPermission("aranarth.toggle.tp")) {
            gui.setItem(29, buildToggleItem(Material.ENDER_PEARL, Lang.getFor(player, "gui.toggle.tp_requests"), !aranarthPlayer.isTogglingTp()));
        } else {
            gui.setItem(29, buildLockedItem(Material.ENDER_PEARL, "&f&lTeleport Requests"));
        }

        // Weather Messages
        gui.setItem(30, buildToggleItem(Material.WIND_CHARGE, Lang.getFor(player, "gui.toggle.weather_messages"), !aranarthPlayer.isWeatherMessageDisabled()));

        // Dominion Msg Compact
        gui.setItem(31, buildToggleItem(Material.COMPASS, Lang.getFor(player, "gui.toggle.dominion_compact"), aranarthPlayer.isDominionMsgCompact()));

        // Interactive Chat
        boolean hasInteractivePerm = aranarthPlayer.getSaintRank() >= 2 || aranarthPlayer.getCouncilRank() > 0;
        if (hasInteractivePerm) {
            gui.setItem(32, buildToggleItem(Material.RECOVERY_COMPASS, Lang.getFor(player, "gui.toggle.interactive_chat"), aranarthPlayer.isInteractiveChatEnabled()));
        } else {
            gui.setItem(32, buildLockedItem(Material.RECOVERY_COMPASS, "&f&lInteractive Chat"));
        }

        // Emoji
        gui.setItem(33, buildToggleItem(Material.HEART_OF_THE_SEA, Lang.getFor(player, "gui.toggle.emoji"), aranarthPlayer.isEmojiEnabled()));

        // Size Scale
        gui.setItem(34, buildToggleItem(Material.POPPED_CHORUS_FRUIT, Lang.getFor(player, "gui.toggle.size_scale"), aranarthPlayer.isSizeScaleEnabled()));

        // Invisible Armor
        if (player.hasPermission("aranarth.invisiblearmor")) {
            gui.setItem(37, buildToggleItem(Material.IRON_CHESTPLATE, Lang.getFor(player, "gui.toggle.invisible_armor"), InvisibleArmorManager.isArmorHidden(player.getUniqueId())));
        } else {
            gui.setItem(37, buildLockedItem(Material.IRON_CHESTPLATE, "&f&lInvisible Armor"));
        }

        // Server Tips
        gui.setItem(38, buildToggleItem(Material.KNOWLEDGE_BOOK, Lang.getFor(player, "gui.toggle.server_tips"), !aranarthPlayer.isServerTipsDisabled()));

        return gui;
    }

    public static boolean hasAnyFirePerk(AranarthPlayer aranarthPlayer) {
        return (aranarthPlayer.getPerks().getOrDefault(Perk.BLUEFIRE, 0) == 1)
                || (aranarthPlayer.getPerks().getOrDefault(Perk.WHITEFIRE, 0) == 1)
                || (aranarthPlayer.getPerks().getOrDefault(Perk.PRISMATICFIRE, 0) == 1)
;
    }

    public static ItemStack buildFireTypeItem(FireType type) {
        ItemStack item = new ItemStack(Material.CAMPFIRE);
        ItemMeta meta = item.getItemMeta();
        String displayName = switch (type) {
            case DEFAULT -> Lang.get("gui.toggle.fire_regular");
            case BLUE -> Lang.get("gui.toggle.fire_blue");
            case WHITE -> Lang.get("gui.toggle.fire_white");
            case PRISMATIC -> ChatUtils.translateToGradient(
                    "#EB5A5A,#EB8C5A,#EBEB5A,#5ACA5A,#5A6EEB,#9B5ACA", "Prismatic Fire", true);
        };
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildToggleItem(Material material, String name, boolean active) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String status = active ? Lang.get("gui.active") : Lang.get("gui.inactive");
        meta.setDisplayName(name + " " + Lang.get("gui.separator") + " " + status);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildLockedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name + " " + Lang.get("gui.separator") + " " + Lang.get("gui.locked"));
        item.setItemMeta(meta);
        return item;
    }

}
