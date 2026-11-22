package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.produce.Produce;
import com.aearost.aranarthcore.items.produce.ProduceType;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Handles produce in villager and wandering trader trades.
 */
public class ProduceTradeUtils {

    private static final Random RANDOM = new Random();
    private static final int RESULT_SLOT = 2;

    /** How many times a Farmer's produce trade can be used before it restocks, like vanilla crop trades. */
    private static final int MAX_USES = 16;
    private static final float PRICE_MULTIPLIER = 0.05F;
    private static final int NOVICE_XP = 2;
    private static final int APPRENTICE_XP = 10;
    private static final int TRADES_PER_LEVEL = 2;
    private static final int VANILLA_TRADES_PER_LEVEL = 2;

    private static final String SWEET_BERRIES = "sweet_berries";

    /**
     * How many of each produce a Farmer buys for 1 emerald, keyed by produce id (or SWEET_BERRIES).
     */
    private static final Map<String, Integer> AMOUNT_PER_EMERALD = new LinkedHashMap<>();

    static {
        for (ProduceType type : ProduceType.values()) {
            if (type.isInVoteCrate()) {
                // Common tree fruit, one per spot
                put(type, 12);
            }
        }
        // Tree fruit that drops several per spot
        put(ProduceType.CHERRY, 24);
        put(ProduceType.LYCHEE, 24);
        put(ProduceType.BANANA, 24);
        // Several per spot, but their trees only grow where their condition is met
        put(ProduceType.DATE, 16);
        put(ProduceType.OLIVE, 16);
        put(ProduceType.BLUEBERRY, 32);
        put(ProduceType.RASPBERRY, 32);
        put(ProduceType.BLACKBERRY, 32);
        put(ProduceType.STRAWBERRY, 32);
        AMOUNT_PER_EMERALD.put(SWEET_BERRIES, 32);
        put(ProduceType.GREEN_GRAPES, 32);
        put(ProduceType.RED_GRAPES, 32);
        put(ProduceType.KIWI, 32);
        put(ProduceType.CANTALOUPE_SLICE, 32);
        put(ProduceType.TOMATO, 24);
        // Peppers change colour the longer they are left, so each colour takes a different time to grow
        put(ProduceType.GREEN_BELL_PEPPER, 24);
        put(ProduceType.YELLOW_BELL_PEPPER, 12);
        put(ProduceType.ORANGE_BELL_PEPPER, 12);
        put(ProduceType.RED_BELL_PEPPER, 18);
        put(ProduceType.CORN, 18);
        put(ProduceType.LETTUCE, 12);
        put(ProduceType.SWEET_POTATO, 12);
        put(ProduceType.CABBAGE, 8);
        put(ProduceType.PINEAPPLE, 3);
    }

    private static void put(ProduceType type, int amount) {
        AMOUNT_PER_EMERALD.put(type.getId(), amount);
    }

    /**
     * Gives a Farmer villager its produce trades.
     * @param villager The villager.
     */
    public static void ensureFarmerTrades(Villager villager) {
        if (villager.getProfession() != Villager.Profession.FARMER || villager.getRecipeCount() == 0) {
            return;
        }
        int target = villager.getVillagerLevel() >= 2 ? TRADES_PER_LEVEL * 2 : TRADES_PER_LEVEL;

        List<MerchantRecipe> vanilla = new ArrayList<>();
        List<MerchantRecipe> produce = new ArrayList<>();
        Set<String> offered = new HashSet<>();
        for (MerchantRecipe recipe : villager.getRecipes()) {
            String key = getFarmerTradeKey(recipe);
            if (key == null) {
                vanilla.add(recipe);
            } else {
                produce.add(recipe);
                offered.add(key);
            }
        }
        if (produce.size() >= target) {
            return;
        }

        List<String> pool = new ArrayList<>(AMOUNT_PER_EMERALD.keySet());
        pool.removeAll(offered);
        while (produce.size() < target && !pool.isEmpty()) {
            String key = pool.remove(RANDOM.nextInt(pool.size()));
            produce.add(createFarmerTrade(key, produce.size() < TRADES_PER_LEVEL ? NOVICE_XP : APPRENTICE_XP));
        }

        // Novice trades, then Apprentice trades, then every higher level
        List<MerchantRecipe> ordered = new ArrayList<>();
        ordered.addAll(vanilla.subList(0, Math.min(VANILLA_TRADES_PER_LEVEL, vanilla.size())));
        ordered.addAll(produce.subList(0, Math.min(TRADES_PER_LEVEL, produce.size())));
        ordered.addAll(vanilla.subList(Math.min(VANILLA_TRADES_PER_LEVEL, vanilla.size()),
                Math.min(VANILLA_TRADES_PER_LEVEL * 2, vanilla.size())));
        ordered.addAll(produce.subList(Math.min(TRADES_PER_LEVEL, produce.size()), produce.size()));
        ordered.addAll(vanilla.subList(Math.min(VANILLA_TRADES_PER_LEVEL * 2, vanilla.size()), vanilla.size()));
        villager.setRecipes(ordered);
    }

    /**
     * Provides what a Farmer's produce trade buys.
     * @param recipe The trade.
     * @return The produce id or SWEET_BERRIES, or null if the trade is not a produce trade.
     */
    private static String getFarmerTradeKey(MerchantRecipe recipe) {
        if (recipe.getResult().getType() != Material.EMERALD || recipe.getIngredients().isEmpty()) {
            return null;
        }
        ItemStack ingredient = recipe.getIngredients().getFirst();
        ProduceType type = Produce.getType(ingredient);
        if (type != null) {
            return type.getId();
        }
        return ingredient.getType() == Material.SWEET_BERRIES ? SWEET_BERRIES : null;
    }

    private static MerchantRecipe createFarmerTrade(String key, int villagerXp) {
        MerchantRecipe recipe = new MerchantRecipe(new ItemStack(Material.EMERALD), 0, MAX_USES, true, villagerXp,
                PRICE_MULTIPLIER);
        int amount = AMOUNT_PER_EMERALD.get(key);
        ProduceType type = ProduceType.fromId(key);
        recipe.addIngredient(type != null ? createProduce(type, amount) : createSweetBerries(amount));
        return recipe;
    }

    /**
     * Rebuilds the produce in every trade with this month's seasonal lore, as trades only accept produce whose lore
     * matches exactly.
     * @param merchant The villager or wandering trader.
     */
    public static void refreshProduceTrades(Merchant merchant) {
        // Every trade of a wandering trader is made by the plugin, so its vanilla seeds and crops must match exactly too
        boolean allSeeds = merchant instanceof WanderingTrader;
        List<MerchantRecipe> recipes = merchant.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe recipe = recipes.get(i);
            boolean isChanged = false;
            ItemStack result = refresh(recipe.getResult(), allSeeds);
            if (result != null) {
                isChanged = true;
            } else {
                result = recipe.getResult();
            }
            List<ItemStack> ingredients = new ArrayList<>();
            for (ItemStack ingredient : recipe.getIngredients()) {
                ItemStack refreshed = refresh(ingredient, allSeeds);
                if (refreshed != null) {
                    isChanged = true;
                }
                ingredients.add(refreshed != null ? refreshed : ingredient);
            }
            if (!isChanged) {
                continue;
            }
            MerchantRecipe updated = new MerchantRecipe(result, recipe.getUses(), recipe.getMaxUses(),
                    recipe.hasExperienceReward(), recipe.getVillagerExperience(), recipe.getPriceMultiplier(),
                    recipe.getDemand(), recipe.getSpecialPrice(), recipe.shouldIgnoreDiscounts());
            updated.setIngredients(ingredients);
            merchant.setRecipe(i, updated);
        }
    }

    /**
     * @param allSeeds Whether every vanilla seed and crop is refreshed too, rather than only sweet berries.
     * @return The item with this month's seasonal lore, or null if it has none or is already current.
     */
    private static ItemStack refresh(ItemStack item, boolean allSeeds) {
        ProduceType type = Produce.getType(item);
        ItemStack current;
        if (type != null) {
            current = createProduce(type, item.getAmount());
        } else if (allSeeds ? CropUtils.isCropSeed(item.getType()) : item.getType() == Material.SWEET_BERRIES) {
            // Villagers only have our trades of sweet berries, as vanilla villagers never trade them
            current = item.clone();
            CropUtils.updateSeedLore(current, null);
        } else {
            return null;
        }
        return current.isSimilar(item) ? null : current;
    }

    /**
     * Creates vanilla sweet berries with this month's seasonal lore, for trades.
     * @param amount The amount.
     * @return The sweet berries.
     */
    public static ItemStack createSweetBerries(int amount) {
        ItemStack item = new ItemStack(Material.SWEET_BERRIES, amount);
        CropUtils.updateSeedLore(item, null);
        return item;
    }

    /**
     * Creates produce with this month's seasonal lore.
     * @param type The produce.
     * @param amount The amount.
     * @return The produce.
     */
    public static ItemStack createProduce(ProduceType type, int amount) {
        ItemStack item = new Produce(type).getItem();
        item.setAmount(amount);
        CropUtils.updateSeedLore(item, null);
        return item;
    }

    /**
     * Prevents the result of a trade from being taken when it is paid with produce the trade does not ask for.
     * @param e The click in a villager or wandering trader inventory.
     * @param merchant The open merchant inventory.
     */
    public static void onMerchantClick(InventoryClickEvent e, MerchantInventory merchant) {
        if (e.getRawSlot() == RESULT_SLOT && !isTradeAllowed(merchant)) {
            e.setCancelled(true);
            if (e.getWhoClicked() instanceof Player player) {
                player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                        ChatUtils.translateToColor(Lang.getFor(player, "crafting.cannot_use_ingredient"))));
            }
            return;
        }
        refreshPaymentLater(merchant);
    }

    /**
     * Brings produce placed in the payment slots up to this month's seasonal lore once the click is done.
     * @param merchant The open merchant inventory.
     */
    public static void refreshPaymentLater(MerchantInventory merchant) {
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            for (int slot = 0; slot <= 1; slot++) {
                ItemStack item = merchant.getItem(slot);
                if (!CropUtils.hasSeasonalLore(item)) {
                    continue;
                }
                ItemStack updated = item.clone();
                CropUtils.updateSeedLore(updated, null);
                if (!updated.isSimilar(item)) {
                    merchant.setItem(slot, updated);
                }
            }
        });
    }

    /**
     * Determines if the produce given to the merchant is what the trade asks for.
     * @param merchant The open merchant inventory.
     * @return Confirmation of whether the trade may be made.
     */
    private static boolean isTradeAllowed(MerchantInventory merchant) {
        Set<ProduceType> wanted = EnumSet.noneOf(ProduceType.class);
        MerchantRecipe recipe = merchant.getSelectedRecipe();
        if (recipe != null) {
            for (ItemStack ingredient : recipe.getIngredients()) {
                ProduceType type = Produce.getType(ingredient);
                if (type != null) {
                    wanted.add(type);
                }
            }
        }
        for (int slot = 0; slot <= 1; slot++) {
            ProduceType given = Produce.getType(merchant.getItem(slot));
            if (given != null && !wanted.contains(given)) {
                return false;
            }
        }
        return true;
    }
}
