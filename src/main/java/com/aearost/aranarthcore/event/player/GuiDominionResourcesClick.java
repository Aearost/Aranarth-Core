package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiDominionPermissions;
import com.aearost.aranarthcore.gui.GuiDominionResourceFilters;
import com.aearost.aranarthcore.gui.GuiDominionResources;
import com.aearost.aranarthcore.gui.GuiDominionResourcesPreview;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionResourceCategory;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handles preventing the items from being added to and from the Dominion Resources inventory.
 */
public class GuiDominionResourcesClick {

    // Tracks the pending 10-second claim timeout task for each dominion (by dominion ID)
    public static final Map<UUID, BukkitTask> claimTimerTasks = new ConcurrentHashMap<>();

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CHEST) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        int slot = e.getSlot();
        int invSize = e.getClickedInventory().getSize();

        // Top row - only the filter button (slot 4) opens the filter GUI
        if (slot < 9) {
            if (slot == 4) {
                Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
                if (dominion != null && dominion.getLeader().equals(player.getUniqueId())) {
                    new GuiDominionResourceFilters(player).openGui();
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
            }
            return;
        }

        // Bottom row - back button, pagination nav, or filler
        if (slot >= invSize - 9) {
            if (slot == 40) {
                // Back button - return to dominion hub
                new GuiDominionPermissions(player).openGui();
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                return;
            }
            ItemStack navItem = e.getClickedInventory().getItem(slot);
            if (navItem != null && navItem.hasItemMeta()) {
                String navName = ChatUtils.stripColorFormatting(navItem.getItemMeta().getDisplayName());
                int currentPage = GuiDominionResources.playerPage.getOrDefault(player.getUniqueId(), 0);
                if (navName.equals(ChatUtils.stripColorFormatting(Lang.getFor(player, "gui.dominionresources.prev")))) {
                    new GuiDominionResources(player, currentPage - 1).openGui();
                } else if (navName.equals(ChatUtils.stripColorFormatting(Lang.getFor(player, "gui.dominionresources.next")))) {
                    new GuiDominionResources(player, currentPage + 1).openGui();
                }
            }
            return;
        }

        // Content area - biome items
        ItemStack clickedItem = e.getClickedInventory().getItem(slot);
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }
        String biomeNameUnformatted = ChatUtils.stripColorFormatting(clickedItem.getItemMeta().getDisplayName());
        String biomeNameFormatted = "minecraft:" + biomeNameUnformatted.replaceAll(" ", "_").toLowerCase();
        Biome biome = Registry.BIOME.get(NamespacedKey.fromString(biomeNameFormatted));
        if (biome == null) {
            return;
        }

        if (e.getClick() == ClickType.RIGHT) {
            new GuiDominionResourcesPreview(player, biome).openGui();
            return;
        }

        if (e.getClick() != ClickType.LEFT) {
            return;
        }

        // Left-click - begin claim flow
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        dominion.setBiomeResourcesBeingClaimed(biome);
        DominionUtils.updateDominion(dominion);

        // Warn about disabled categories first, before the prompt
        Set<DominionResourceCategory> disabled = dominion.getDisabledResourceCategories();
        if (!disabled.isEmpty()) {
            String categoryNames = disabled.stream()
                    .map(cat -> ChatUtils.translateToColor("&c" + Lang.get("gui.dominionresourcefilters.category_" + cat.name().toLowerCase())))
                    .collect(Collectors.joining("&7, "));
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.resources_categories_disabled", "categories", categoryNames)));
        }

        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.resources_enter_claims", "biome", biomeNameUnformatted)));
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.resources_available", "chunks", dominion.getClaimableResources() + "/" + DominionUtils.getMaxClaimableResourcesAmount(dominion), "biome", biomeNameUnformatted)));

        // Show average claim yield across all pending slots
        List<Double> yields = dominion.getClaimFoodYields();
        int claimable = dominion.getClaimableResources();
        double avgYield;
        if (claimable == 0) {
            avgYield = DominionUtils.foodPercentageToYield(DominionUtils.getBaseFoodPercentage(dominion));
        } else {
            double sum = yields.stream().mapToDouble(Double::doubleValue).sum();
            int missing = Math.max(0, claimable - yields.size());
            avgYield = (sum + missing) / claimable;
        }
        int avgYieldPct = (int) Math.round(avgYield * 100);
        String yieldColor = avgYieldPct >= 80 ? "&a" : avgYieldPct >= 50 ? "&e" : "&c";
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.resources_avg_yield", "color", yieldColor, "yield", avgYieldPct)));
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.resources_timer_hint")));
        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1F);
        player.closeInventory();

        // Start 10-second auto-cancel timer
        UUID dominionId = dominion.getId();
        BukkitTask existing = claimTimerTasks.remove(dominionId);
        if (existing != null) existing.cancel();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            claimTimerTasks.remove(dominionId);
            if (dominion.getBiomeResourcesBeingClaimed() != null) {
                dominion.setBiomeResourcesBeingClaimed(null);
                DominionUtils.updateDominion(dominion);
                Player leader = Bukkit.getPlayer(dominion.getLeader());
                if (leader != null && leader.isOnline()) {
                    leader.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.resources_timer_expired")));
                }
            }
        }, 200L); // 10 seconds
        claimTimerTasks.put(dominionId, task);
    }
}
