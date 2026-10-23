package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.essence.EssenceBlank;
import com.aearost.aranarthcore.recipes.RecipeEssences;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.EnchantingUtils;
import com.aearost.aranarthcore.utils.EssenceImbueUtils;
import com.aearost.aranarthcore.utils.Lang;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Random;

import static com.aearost.aranarthcore.objects.CustomKeys.ARANARTHIUM_INGOT;
import static com.aearost.aranarthcore.objects.CustomKeys.CLUSTER;
import static com.aearost.aranarthcore.objects.CustomKeys.ESSENCE_TYPE;

/**
 * Handles crushing Aranarthium Ingots into blank Essences, imbuing them with earned XP,
 * and crafting essences from imbued Essences.
 */
public class EssenceImbueListener implements Listener {

    private static final int ANVIL_RESULT_SLOT = 2;
    private static final int OFF_HAND_SLOT = 40;

    private static final Random RANDOM = new Random();

    public EssenceImbueListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Splits earned XP between the player and the first Essence in their inventory that is not fully imbued.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerExpChange(PlayerExpChangeEvent e) {
        int amount = e.getAmount();
        if (amount <= 0) {
            return;
        }
        // Bottles o' Enchanting can be filled with the player's own EXP, so they never receive the Distilled Wisdom bonus
        boolean isFromBottle = e.getSource() instanceof ExperienceOrb orb && orb.getSpawnReason() == ExperienceOrb.SpawnReason.EXP_BOTTLE;
        e.setAmount(splitExp(e.getPlayer(), amount, !isFromBottle));
    }

    /**
     * Splits the input XP between the player and the Essences in their inventory that are not fully imbued.
     * @param player The player.
     * @param amount The XP earned.
     * @param applyDistilledWisdom Whether the Distilled Wisdom bonus can apply.
     * @return The XP the player keeps.
     */
    public static int splitExp(Player player, int amount, boolean applyDistilledWisdom) {
        if (amount <= 0 || !EssenceImbueUtils.hasRequiredRank(player)) {
            return amount;
        }
        if (findImbuableSlot(player.getInventory(), EssenceImbueUtils.getMaxImbuedXp(), -1) == -1) {
            return amount;
        }

        int playerShare = amount - amount / 2;
        // Odd amounts give the extra point to the player, and have a 50% chance to also give it to the Essence
        int essenceShare = amount / 2 + (amount % 2 == 1 && RANDOM.nextBoolean() ? 1 : 0);

        int distributed = imbue(player, essenceShare);
        if (applyDistilledWisdom && distributed == essenceShare) {
            double multiplier = EnchantingUtils.getDistilledWisdomEssenceMultiplier(player);
            int bonus = EnchantingUtils.randomRound(essenceShare * (multiplier - 1));
            if (bonus > 0) {
                imbue(player, bonus);
            }
        }

        // Any XP that could not be imbued because every Essence is full is returned to the player
        return Math.min(amount, playerShare + (essenceShare - distributed));
    }

    /**
     * Imbues the input XP into the Essences in the player's inventory that are not yet fully imbued.
     * @param player The player.
     * @param amount The XP to imbue.
     * @return The XP that was imbued, which is less than the amount if every Essence became full.
     */
    public static int imbue(Player player, int amount) {
        int maxXp = EssenceImbueUtils.getMaxImbuedXp();
        PlayerInventory inventory = player.getInventory();
        int distributed = 0;
        int slot = -1;
        while (distributed < amount) {
            slot = findImbuableSlot(inventory, maxXp, slot);
            if (slot == -1) {
                break;
            }
            ItemStack essence = inventory.getItem(slot);
            int previousXp = EssenceImbueUtils.getImbuedXp(essence);
            int added = Math.min(amount - distributed, maxXp - previousXp);
            int newXp = previousXp + added;
            EssenceImbueUtils.setImbuedXp(essence, newXp);
            inventory.setItem(slot, essence);
            distributed += added;
            notifyThresholds(player, previousXp, newXp, maxXp);
        }
        return distributed;
    }

    /**
     * Provides the first slot after the input slot holding an Essence that is not yet fully imbued.
     * @param inventory The player's inventory.
     * @param maxXp The maximum XP an Essence can hold.
     * @param afterSlot The slot to search after, or -1 to search from the start.
     * @return The slot, or -1 if there is none.
     */
    private static int findImbuableSlot(PlayerInventory inventory, int maxXp, int afterSlot) {
        int[] slots = new int[37];
        for (int i = 0; i < 36; i++) {
            slots[i] = i;
        }
        slots[36] = OFF_HAND_SLOT;

        boolean passed = afterSlot == -1;
        for (int slot : slots) {
            if (!passed) {
                if (slot == afterSlot) {
                    passed = true;
                }
                continue;
            }
            ItemStack item = inventory.getItem(slot);
            if (EssenceImbueUtils.isBlankEssence(item) && EssenceImbueUtils.getImbuedXp(item) < maxXp) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Notifies the player when their Essence becomes imbued enough to craft a new tier of essence.
     */
    private static void notifyThresholds(Player player, int previousXp, int newXp, int maxXp) {
        if (newXp >= maxXp && previousXp < maxXp) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "essence.imbue_complete")));
            return;
        }
        for (EssenceImbueUtils.Tier tier : EssenceImbueUtils.Tier.values()) {
            int required = tier.getRequiredXp();
            if (previousXp < required && newXp >= required) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "essence.imbue_threshold", "tier", Lang.getFor(player, tier.getLangKey()))));
            }
        }
    }

    /**
     * Displays a blank Essence as the result when crushing an Aranarthium Ingot with a Mace.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        AnvilInventory anvil = e.getInventory();
        ItemStack mace = getCrushingMace(anvil);
        if (mace == null) {
            return;
        }

        if (getRemainingDurability(mace) < getMaceCost(mace)) {
            e.setResult(null);
            if (e.getView().getPlayer() instanceof Player player) {
                sendActionBar(player, Lang.getFor(player, "essence.crush_mace_durability"));
            }
            return;
        }

        e.setResult(new EssenceBlank().getItem());
        e.getView().setRepairCost(0);
    }

    /**
     * Handles taking the blank Essence from the anvil, as vanilla does not allow taking a result that costs 0 levels.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilResultClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory() instanceof AnvilInventory anvil)) {
            return;
        }
        if (e.getRawSlot() != ANVIL_RESULT_SLOT) {
            return;
        }
        ItemStack mace = getCrushingMace(anvil);
        if (mace == null) {
            return;
        }

        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        int cost = getMaceCost(mace);
        if (getRemainingDurability(mace) < cost) {
            return;
        }

        ItemStack essence = new EssenceBlank().getItem();
        if (e.isShiftClick()) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(essence);
            if (!leftover.isEmpty()) {
                return;
            }
        } else {
            ItemStack cursor = player.getItemOnCursor();
            if (cursor.getType() != Material.AIR) {
                return;
            }
            player.setItemOnCursor(essence);
        }

        // Consume one ingot
        int ingotSlot = isAranarthiumIngot(anvil.getItem(0)) ? 0 : 1;
        int maceSlot = ingotSlot == 0 ? 1 : 0;
        ItemStack ingot = anvil.getItem(ingotSlot);
        ingot.setAmount(ingot.getAmount() - 1);
        anvil.setItem(ingotSlot, ingot.getAmount() > 0 ? ingot : null);

        // Damage the mace by a fixed portion of its max durability, regardless of Unbreaking
        if (!isResilient(mace)) {
            ItemMeta meta = mace.getItemMeta();
            if (meta instanceof Damageable damageable) {
                int newDamage = damageable.getDamage() + cost;
                if (newDamage >= mace.getType().getMaxDurability()) {
                    anvil.setItem(maceSlot, null);
                } else {
                    damageable.setDamage(newDamage);
                    mace.setItemMeta(meta);
                    anvil.setItem(maceSlot, mace);
                }
            }
        }

        anvil.setItem(ANVIL_RESULT_SLOT, null);
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), player::updateInventory);
    }

    /**
     * Provides the Mace if the anvil contains an Aranarthium Ingot and a Mace in either order.
     */
    private ItemStack getCrushingMace(AnvilInventory anvil) {
        ItemStack first = anvil.getItem(0);
        ItemStack second = anvil.getItem(1);
        if (isAranarthiumIngot(first) && isMace(second)) {
            return second;
        } else if (isMace(first) && isAranarthiumIngot(second)) {
            return first;
        }
        return null;
    }

    private boolean isAranarthiumIngot(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD || !item.hasItemMeta()) {
            return false;
        }
        String type = item.getItemMeta().getPersistentDataContainer().get(ARANARTHIUM_INGOT, PersistentDataType.STRING);
        return "aranarthium".equals(type);
    }

    private boolean isMace(ItemStack item) {
        return item != null && item.getType() == Material.MACE;
    }

    /**
     * Determines if the item has the Resilience essence, which grants infinite durability.
     */
    private boolean isResilient(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }
        String type = item.getItemMeta().getPersistentDataContainer().get(ESSENCE_TYPE, PersistentDataType.STRING);
        return "essence_resilience".equals(type);
    }

    private int getMaceCost(ItemStack mace) {
        return (int) Math.ceil(mace.getType().getMaxDurability() * EssenceImbueUtils.MACE_DURABILITY_COST);
    }

    private int getRemainingDurability(ItemStack mace) {
        int damage = mace.getItemMeta() instanceof Damageable damageable ? damageable.getDamage() : 0;
        return mace.getType().getMaxDurability() - damage;
    }

    /**
     * Validates the crafting of essences from imbued Essences, and prevents blank Essences
     * from being used in any other recipe.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        String errorKey = getCraftError(e.getInventory(), e.getRecipe(), e.getView().getPlayer());
        if (errorKey == null) {
            return;
        }
        e.getInventory().setResult(null);
        if (!errorKey.isEmpty() && e.getView().getPlayer() instanceof Player player) {
            sendActionBar(player, Lang.getFor(player, errorKey, "percent", getRequiredPercent(e.getRecipe())));
        }
    }

    /**
     * Re-validates the crafting of essences in case the matrix changed after the result was displayed.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCraft(CraftItemEvent e) {
        if (getCraftError(e.getInventory(), e.getRecipe(), e.getWhoClicked()) != null) {
            e.setCancelled(true);
        }
    }

    /**
     * Determines whether the crafting matrix is invalid for an essence recipe or uses a blank Essence elsewhere.
     * @return null if the craft is allowed, an empty String if it is silently blocked,
     * or the lang key of the message explaining why it is blocked.
     */
    private String getCraftError(CraftingInventory inventory, Recipe recipe, HumanEntity crafter) {
        boolean isEssenceRecipe = isEssenceRecipe(recipe);
        ItemStack[] matrix = inventory.getMatrix();
        ItemStack blankEssence = null;
        for (ItemStack ingredient : matrix) {
            if (EssenceImbueUtils.isBlankEssence(ingredient)) {
                blankEssence = ingredient;
                break;
            }
        }

        if (!isEssenceRecipe) {
            return blankEssence != null ? "" : null;
        }

        // Every ingredient other than the Essence must be a cluster
        if (blankEssence == null || matrix.length < 9 || !EssenceImbueUtils.isBlankEssence(matrix[4])) {
            return "";
        }
        for (int i = 0; i < matrix.length; i++) {
            if (i == 4) {
                continue;
            }
            ItemStack ingredient = matrix[i];
            if (ingredient == null || !ingredient.hasItemMeta()
                    || !ingredient.getItemMeta().getPersistentDataContainer().has(CLUSTER, PersistentDataType.STRING)) {
                return "";
            }
        }

        if (!(crafter instanceof Player player) || !EssenceImbueUtils.hasRequiredRank(player)) {
            return "essence.craft_rank_required";
        }

        EssenceImbueUtils.Tier tier = getTier(recipe);
        if (tier == null || EssenceImbueUtils.getImbuedXp(blankEssence) < tier.getRequiredXp()) {
            return "essence.craft_not_enough_xp";
        }
        return null;
    }

    private boolean isEssenceRecipe(Recipe recipe) {
        return recipe instanceof Keyed keyed
                && keyed.getKey().getNamespace().equals(AranarthCore.getInstance().getName().toLowerCase())
                && keyed.getKey().getKey().startsWith(RecipeEssences.KEY_PREFIX);
    }

    private EssenceImbueUtils.Tier getTier(Recipe recipe) {
        ItemStack result = recipe.getResult();
        if (!result.hasItemMeta()) {
            return null;
        }
        return EssenceImbueUtils.getTier(result.getItemMeta().getPersistentDataContainer().get(ESSENCE_TYPE, PersistentDataType.STRING));
    }

    private String getRequiredPercent(Recipe recipe) {
        EssenceImbueUtils.Tier tier = recipe == null ? null : getTier(recipe);
        return tier == null ? "100" : String.valueOf(tier.getPercent());
    }

    private void sendActionBar(Player player, String message) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor(message)));
    }
}
