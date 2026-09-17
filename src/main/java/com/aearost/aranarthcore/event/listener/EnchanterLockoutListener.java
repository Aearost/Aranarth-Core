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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles over-leveled trades on the Wandering Enchanter.
 */
public class EnchanterLockoutListener implements Listener {

    private static final int RESULT_SLOT = 2;

    public EnchanterLockoutListener(AranarthCore plugin) {
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

        PersistentDataContainer pdc = wt.getPersistentDataContainer();
        if (!pdc.has(CustomKeys.WANDERING_TRADER_TYPE, PersistentDataType.STRING)) {
            return;
        }
        if (!"ENCHANTER".equals(pdc.get(CustomKeys.WANDERING_TRADER_TYPE, PersistentDataType.STRING))) {
            return;
        }
        if (!pdc.has(CustomKeys.ENCHANTER_LOCKOUT, PersistentDataType.STRING)) {
            return;
        }

        Set<Integer> overLeveledIndices = parseOverLeveledIndices(pdc);
        int selectedIndex = merchantInv.getSelectedRecipeIndex();

        if (!overLeveledIndices.contains(selectedIndex)) {
            return;
        }

        // Block any click on an over-leveled result slot if one was already purchased
        if (pdc.has(CustomKeys.ENCHANTER_PURCHASED, PersistentDataType.BYTE)) {
            e.setCancelled(true);
            return;
        }

        // Cancel vanilla trade
        e.setCancelled(true);

        MerchantRecipe recipe = merchantInv.getSelectedRecipe();
        if (recipe == null || recipe.getIngredients().isEmpty()) {
            return;
        }

        // Consume the required ingredient
        Player player = (Player) e.getWhoClicked();
        ItemStack required = recipe.getIngredients().get(0);
        ItemStack firstIndex = merchantInv.getItem(0);
        if (firstIndex != null && firstIndex.getType() == required.getType() && firstIndex.getAmount() >= required.getAmount()) {
            int leftOver = firstIndex.getAmount() - required.getAmount();
            merchantInv.setItem(0, leftOver > 0 ? new ItemStack(firstIndex.getType(), leftOver) : null);
        } else {
            Map<Integer, ItemStack> notConsumed = player.getInventory()
                    .removeItem(new ItemStack(required.getType(), required.getAmount()));
            if (!notConsumed.isEmpty()) {
                return;
            }
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

        // Mark this trader as having sold an over-leveled enchantment
        pdc.set(CustomKeys.ENCHANTER_PURCHASED, PersistentDataType.BYTE, (byte) 1);

        // Exhaust all over-leveled trades so the server side reflects the lockout
        List<MerchantRecipe> recipes = wt.getRecipes();
        for (int idx : overLeveledIndices) {
            if (idx >= recipes.size()) continue;
            recipes.get(idx).setUses(recipes.get(idx).getMaxUses());
        }
        wt.setRecipes(recipes);

        // Push the updated trade list to the client so the crossed-out trades render
        sendMerchantOffersUpdate(player);
    }

    /**
     * Blocks navigation to any over-leveled trade after one has been purchased.
     */
    @EventHandler
    public void onTradeSelect(TradeSelectEvent e) {
        if (!(e.getInventory() instanceof MerchantInventory merchantInv)) {
            return;
        }
        if (!(merchantInv.getMerchant() instanceof WanderingTrader wt)) {
            return;
        }

        PersistentDataContainer pdc = wt.getPersistentDataContainer();
        if (!pdc.has(CustomKeys.WANDERING_TRADER_TYPE, PersistentDataType.STRING)) {
            return;
        }
        if (!"ENCHANTER".equals(pdc.get(CustomKeys.WANDERING_TRADER_TYPE, PersistentDataType.STRING))) {
            return;
        }
        if (!pdc.has(CustomKeys.ENCHANTER_LOCKOUT, PersistentDataType.STRING)) {
            return;
        }
        if (!pdc.has(CustomKeys.ENCHANTER_PURCHASED, PersistentDataType.BYTE)) {
            return;
        }

        Set<Integer> overLeveledIndices = parseOverLeveledIndices(pdc);
        if (!overLeveledIndices.contains(e.getIndex())) {
            return;
        }

        e.setCancelled(true);
    }

    /**
     * Sends an update to the player so their client redraws the trade list immediately.
     */
    private void sendMerchantOffersUpdate(Player player) {
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

            Class<?> packetClass = Class.forName(
                    "net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket");
            Class<?> merchantOffersClass = Class.forName(
                    "net.minecraft.world.item.trading.MerchantOffers");
            Constructor<?> ctor = packetClass.getConstructor(
                    int.class, merchantOffersClass, int.class, int.class, boolean.class, boolean.class);
            Object packet = ctor.newInstance(containerId, offers, 1, 0, false, false);

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
