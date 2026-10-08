package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.CustomKeys;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles over-leveled trades on the Wandering Enchanter.
 * Each player may only purchase a single over-leveled trade from a given Wandering Enchanter.
 */
public class EnchanterLockoutListener implements Listener {

    private static final int RESULT_SLOT = 2;

    private final AranarthCore plugin;

    public EnchanterLockoutListener(AranarthCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory() instanceof MerchantInventory merchantInv)) {
            return;
        }
        if (e.getRawSlot() != RESULT_SLOT) {
            return;
        }
        if (e.getCurrentItem() == null) {
            return;
        }
        if (!isTakeAction(e.getAction()) && e.getAction() != InventoryAction.NOTHING) {
            return;
        }
        if (!(merchantInv.getMerchant() instanceof WanderingTrader wt)) {
            return;
        }

        Set<Integer> overLeveledIndices = getOverLeveledIndices(wt);
        if (overLeveledIndices == null) {
            return;
        }

        int selectedIndex = merchantInv.getSelectedRecipeIndex();
        if (!overLeveledIndices.contains(selectedIndex)) {
            return;
        }

        // Cancel vanilla trade
        e.setCancelled(true);

        // Block the purchase if this player already bought an over-leveled trade from this enchanter
        Player player = (Player) e.getWhoClicked();
        PersistentDataContainer pdc = wt.getPersistentDataContainer();
        if (hasPurchased(pdc, player)) {
            return;
        }

        List<MerchantRecipe> recipes = wt.getRecipes();
        if (selectedIndex >= recipes.size()) {
            return;
        }
        MerchantRecipe recipe = recipes.get(selectedIndex);
        if (recipe.getIngredients().isEmpty()) {
            return;
        }

        // Block the purchase if another player already bought this specific trade
        if (recipe.getUses() >= recipe.getMaxUses()) {
            return;
        }

        // Consume the required ingredient
        ItemStack required = recipe.getIngredients().get(0);
        ItemStack firstIndex = merchantInv.getItem(0);
        if (firstIndex != null && firstIndex.getType() == required.getType() && firstIndex.getAmount() >= required.getAmount()) {
            int leftOver = firstIndex.getAmount() - required.getAmount();
            merchantInv.setItem(0, leftOver > 0 ? new ItemStack(firstIndex.getType(), leftOver) : null);
        } else {
            ItemStack toRemove = new ItemStack(required.getType(), required.getAmount());
            if (!player.getInventory().containsAtLeast(toRemove, required.getAmount())) {
                return;
            }
            player.getInventory().removeItem(toRemove);
        }

        // Give the result item directly, bypassing the merchant transaction
        ItemStack result = recipe.getResult().clone();
        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            player.getInventory().addItem(result).values()
                    .forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        } else if (player.getItemOnCursor().getType() == org.bukkit.Material.AIR) {
            player.setItemOnCursor(result);
        } else {
            player.getInventory().addItem(result).values()
                    .forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        }

        // Mark this player as having bought an over-leveled enchantment from this enchanter
        addPurchaser(pdc, player);

        // Exhaust only the purchased trade so other players can still buy the remaining over-leveled trades
        recipe.setUses(recipe.getMaxUses());
        wt.setRecipes(recipes);

        // Push the updated trade list to the client so the crossed-out trades render
        sendMerchantOffersUpdate(player, overLeveledIndices);
    }

    /**
     * Shows all over-leveled trades as crossed out to a player who has already purchased one.
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getInventory() instanceof MerchantInventory merchantInv)) {
            return;
        }
        if (!(merchantInv.getMerchant() instanceof WanderingTrader wt)) {
            return;
        }
        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }

        Set<Integer> overLeveledIndices = getOverLeveledIndices(wt);
        if (overLeveledIndices == null) {
            return;
        }
        if (!hasPurchased(wt.getPersistentDataContainer(), player)) {
            return;
        }

        // Delayed so this is sent after the vanilla trade list
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.getOpenInventory().getTopInventory() instanceof MerchantInventory openInv
                    && openInv.getMerchant() == wt) {
                sendMerchantOffersUpdate(player, overLeveledIndices);
            }
        });
    }

    /**
     * Blocks navigation to any over-leveled trade after the player has purchased one.
     */
    @EventHandler
    public void onTradeSelect(TradeSelectEvent e) {
        if (!(e.getInventory() instanceof MerchantInventory merchantInv)) {
            return;
        }
        if (!(merchantInv.getMerchant() instanceof WanderingTrader wt)) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        Set<Integer> overLeveledIndices = getOverLeveledIndices(wt);
        if (overLeveledIndices == null) {
            return;
        }
        if (!hasPurchased(wt.getPersistentDataContainer(), player)) {
            return;
        }
        if (!overLeveledIndices.contains(e.getIndex())) {
            return;
        }

        e.setCancelled(true);
    }

    /**
     * Sends an update to the player so their client redraws the trade list immediately.
     */
    private void sendMerchantOffersUpdate(Player player, Set<Integer> lockedIndices) {
        try {
            Method getHandle = player.getClass().getMethod("getHandle");
            Object serverPlayer = getHandle.invoke(player);

            Field containerMenuField = serverPlayer.getClass().getField("containerMenu");
            Object containerMenu = containerMenuField.get(serverPlayer);

            if (!containerMenu.getClass().getName().endsWith("MerchantMenu")) {
                return;
            }

            Field containerIdField = null;
            Class<?> c = containerMenu.getClass();
            while (c != null) {
                try {
                    containerIdField = c.getDeclaredField("containerId");
                    containerIdField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {
                    c = c.getSuperclass();
                }
            }
            if (containerIdField == null) return;
            int containerId = (int) containerIdField.get(containerMenu);

            Method getOffers = containerMenu.getClass().getMethod("getOffers");
            Object offers = getOffers.invoke(containerMenu);

            // Copy the offers so the out of stock state only applies to this player's view
            Object playerOffers = offers.getClass().getMethod("copy").invoke(offers);
            List<?> playerOffersList = (List<?>) playerOffers;
            for (int idx : lockedIndices) {
                if (idx >= playerOffersList.size()) continue;
                Object offer = playerOffersList.get(idx);
                offer.getClass().getMethod("setToOutOfStock").invoke(offer);
            }

            Class<?> packetClass = Class.forName(
                    "net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket");
            Class<?> merchantOffersClass = Class.forName(
                    "net.minecraft.world.item.trading.MerchantOffers");
            Constructor<?> ctor = packetClass.getConstructor(
                    int.class, merchantOffersClass, int.class, int.class, boolean.class, boolean.class);
            Object packet = ctor.newInstance(containerId, playerOffers, 1, 0, false, false);

            Field connectionField = serverPlayer.getClass().getField("connection");
            Object connection = connectionField.get(serverPlayer);
            if (connection == null) return;

            for (Method m : connection.getClass().getMethods()) {
                if (m.getName().equals("send") && m.getParameterCount() == 1) {
                    m.invoke(connection, packet);
                    return;
                }
            }
        } catch (Exception ex) {
            Bukkit.getLogger().warning("[AC] EnchanterLockout - sendMerchantOffersUpdate failed: "
                    + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    /**
     * Provides the over-leveled trade indices of the trader, or null if it is not a Wandering Enchanter.
     */
    private Set<Integer> getOverLeveledIndices(WanderingTrader wt) {
        PersistentDataContainer pdc = wt.getPersistentDataContainer();
        if (!"ENCHANTER".equals(pdc.get(CustomKeys.WANDERING_TRADER_TYPE, PersistentDataType.STRING))) {
            return null;
        }
        if (!pdc.has(CustomKeys.ENCHANTER_LOCKOUT, PersistentDataType.STRING)) {
            return null;
        }
        return parseOverLeveledIndices(pdc);
    }

    private boolean hasPurchased(PersistentDataContainer pdc, Player player) {
        String purchasers = pdc.get(CustomKeys.ENCHANTER_PURCHASERS, PersistentDataType.STRING);
        if (purchasers == null) {
            return false;
        }
        return Arrays.asList(purchasers.split(",")).contains(player.getUniqueId().toString());
    }

    private void addPurchaser(PersistentDataContainer pdc, Player player) {
        String purchasers = pdc.get(CustomKeys.ENCHANTER_PURCHASERS, PersistentDataType.STRING);
        String uuid = player.getUniqueId().toString();
        pdc.set(CustomKeys.ENCHANTER_PURCHASERS, PersistentDataType.STRING,
                purchasers == null || purchasers.isEmpty() ? uuid : purchasers + "," + uuid);
    }

    private Set<Integer> parseOverLeveledIndices(PersistentDataContainer pdc) {
        String lockoutData = pdc.get(CustomKeys.ENCHANTER_LOCKOUT, PersistentDataType.STRING);
        return Arrays.stream(lockoutData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    private boolean isTakeAction(InventoryAction action) {
        return switch (action) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME,
                 MOVE_TO_OTHER_INVENTORY, HOTBAR_SWAP -> true;
            default -> false;
        };
    }
}
