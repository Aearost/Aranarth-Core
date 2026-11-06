package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CustomItemUtils;
import com.aearost.aranarthcore.utils.CustomItemUtils.Station;
import com.aearost.aranarthcore.utils.Lang;
import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LoomInventory;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.StonecutterInventory;

import static com.aearost.aranarthcore.objects.CustomKeys.ARANARTHIUM_INGOT;

/**
 * Prevents custom Aranarth items from being used in any station they are not meant to be used in.
 */
public class CustomItemStationListener implements Listener {

    public CustomItemStationListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Clears the anvil result when a custom item is used incorrectly.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        AnvilInventory anvil = e.getInventory();
        if (!isAnvilAllowed(anvil.getItem(0), anvil.getItem(1))) {
            clearResult(e, e.getView().getPlayer());
        }
    }

    /**
     * Clears the grindstone result when a custom item is used incorrectly.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareGrindstone(PrepareGrindstoneEvent e) {
        GrindstoneInventory grindstone = e.getInventory();
        if (!isGrindstoneAllowed(grindstone.getItem(0), grindstone.getItem(1))) {
            clearResult(e, e.getView().getPlayer());
        }
    }

    /**
     * Clears the smithing table result when a custom item is used in it.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareSmithing(PrepareSmithingEvent e) {
        SmithingInventory smithing = e.getInventory();
        if (isSmithingDisallowed(smithing)) {
            clearResult(e, e.getView().getPlayer());
        }
    }

    /**
     * Clears the cartography table and loom result when a custom item is used in it.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareResult(PrepareResultEvent e) {
        Inventory inventory = e.getInventory();
        if (inventory instanceof CartographyInventory cartography) {
            if (!CustomItemUtils.isAllowed(cartography.getItem(0), Station.CARTOGRAPHY)
                    || !CustomItemUtils.isAllowed(cartography.getItem(1), Station.CARTOGRAPHY)) {
                clearResult(e, e.getView().getPlayer());
            }
        } else if (inventory instanceof LoomInventory loom) {
            if (isLoomDisallowed(loom)) {
                clearResult(e, e.getView().getPlayer());
            }
        }
    }

    /**
     * Safety net preventing the result of any station from being taken when a custom item is used incorrectly.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onResultClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        boolean isDisallowed = false;
        if (top instanceof AnvilInventory anvil && e.getRawSlot() == 2) {
            isDisallowed = !isAnvilAllowed(anvil.getItem(0), anvil.getItem(1));
        } else if (top instanceof GrindstoneInventory grindstone && e.getRawSlot() == 2) {
            isDisallowed = !isGrindstoneAllowed(grindstone.getItem(0), grindstone.getItem(1));
        } else if (top instanceof SmithingInventory smithing && e.getRawSlot() == 3) {
            isDisallowed = isSmithingDisallowed(smithing);
        } else if (top instanceof CartographyInventory cartography && e.getRawSlot() == 2) {
            isDisallowed = !CustomItemUtils.isAllowed(cartography.getItem(0), Station.CARTOGRAPHY)
                    || !CustomItemUtils.isAllowed(cartography.getItem(1), Station.CARTOGRAPHY);
        } else if (top instanceof LoomInventory loom && e.getRawSlot() == 3) {
            isDisallowed = isLoomDisallowed(loom);
        } else if (top instanceof StonecutterInventory stonecutter && e.getRawSlot() == 1) {
            isDisallowed = !CustomItemUtils.isAllowed(stonecutter.getInputItem(), Station.STONECUTTER);
        }

        if (isDisallowed) {
            e.setCancelled(true);
            sendBlockedMessage(e.getWhoClicked());
        }
    }

    /**
     * Prevents custom items from being smelted or cooked.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCook(BlockCookEvent e) {
        if (!CustomItemUtils.isAllowed(e.getSource(), Station.FURNACE)) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents custom items from being used as furnace fuel.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFurnaceBurn(FurnaceBurnEvent e) {
        if (!CustomItemUtils.isAllowed(e.getFuel(), Station.FURNACE)) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents custom items from being used as brewing ingredients.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBrew(BrewEvent e) {
        // Only the potion and ingredient slots, as fuel is validated in onBrewingFuel
        for (int slot = 0; slot <= 3; slot++) {
            if (!CustomItemUtils.isAllowed(e.getContents().getItem(slot), Station.BREWING)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevents custom items from being used as brewing stand fuel.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBrewingFuel(BrewingStandFuelEvent e) {
        if (!CustomItemUtils.isAllowed(e.getFuel(), Station.BREWING)) {
            e.setCancelled(true);
        }
    }

    /**
     * Determines if the anvil inputs are a valid use of any custom items.
     */
    private boolean isAnvilAllowed(ItemStack left, ItemStack right) {
        if (!CustomItemUtils.isAllowed(left, Station.ANVIL) || !CustomItemUtils.isAllowed(right, Station.ANVIL)) {
            return false;
        }
        if (CustomItemUtils.isCustomItem(right)) {
            return CustomItemUtils.isSameCustomItem(left, right) || ARANARTHIUM_INGOT.equals(CustomItemUtils.getIdentityKey(right));
        }
        return true;
    }

    /**
     * Determines if the grindstone inputs are a valid use of any custom items.
     */
    private boolean isGrindstoneAllowed(ItemStack first, ItemStack second) {
        if (!CustomItemUtils.isAllowed(first, Station.GRINDSTONE) || !CustomItemUtils.isAllowed(second, Station.GRINDSTONE)) {
            return false;
        }
        if (isEmpty(first) || isEmpty(second)) {
            return true;
        }
        if (CustomItemUtils.isCustomItem(first) || CustomItemUtils.isCustomItem(second)) {
            return CustomItemUtils.isSameCustomItem(first, second);
        }
        return true;
    }

    private boolean isSmithingDisallowed(SmithingInventory smithing) {
        return !CustomItemUtils.isAllowed(smithing.getInputTemplate(), Station.SMITHING)
                || !CustomItemUtils.isAllowed(smithing.getInputEquipment(), Station.SMITHING)
                || !CustomItemUtils.isAllowed(smithing.getInputMineral(), Station.SMITHING);
    }

    private boolean isLoomDisallowed(LoomInventory loom) {
        for (int slot = 0; slot < 3; slot++) {
            if (!CustomItemUtils.isAllowed(loom.getItem(slot), Station.LOOM)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    /**
     * Clears the result of the station, notifying the player only if there was a result to be blocked.
     */
    private void clearResult(PrepareResultEvent e, HumanEntity viewer) {
        boolean hadResult = !isEmpty(e.getResult());
        e.setResult(null);
        if (hadResult) {
            sendBlockedMessage(viewer);
        }
    }

    private void sendBlockedMessage(HumanEntity viewer) {
        if (viewer instanceof Player player) {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                    ChatUtils.translateToColor(Lang.getFor(player, "crafting.cannot_use_ingredient"))));
        }
    }
}
