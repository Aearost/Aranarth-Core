package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.EnchantingUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.events.skills.salvage.McMMOPlayerSalvageCheckEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.aearost.aranarthcore.objects.CustomKeys.TABLE_ENCHANTED;

/**
 * Handles the sub-skills and the Infinite Library super ability of the Enchanting child skill.
 */
public class EnchantingListener implements Listener {

    private static final Particle.DustOptions LAPIS_DUST = new Particle.DustOptions(Color.fromRGB(38, 97, 156), 1.2F);

    private final Random random = new Random();
    private final Map<UUID, LibraryRoll> libraryRolls = new HashMap<>();
    private final Map<Block, PendingGrind> pendingGrinds = new HashMap<>();

    /**
     * A grindstone result that was just taken, used to identify the EXP it drops in the same tick.
     */
    private record PendingGrind(UUID uuid, boolean isTableEnchanted, int level, int tick) {}

    private enum RollState { PENDING, PAID, FAILED }

    /**
     * The enchantments rolled by Infinite Library for the item in the enchanting table.
     */
    private static class LibraryRoll {
        private final ItemStack item;
        private final int bonus;
        private final List<Map<Enchantment, Integer>> results = new ArrayList<>();
        private RollState state = RollState.PENDING;

        private LibraryRoll(ItemStack item, int bonus) {
            this.item = item;
            this.bonus = bonus;
        }

        private boolean matchesItem(ItemStack other) {
            return other != null && item.isSimilar(other);
        }
    }

    public EnchantingListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Applies the Infinite Library roll, Arcane Ascension and Arcane Recall when an item is enchanted.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent e) {
        Player player = e.getEnchanter();
        UUID uuid = player.getUniqueId();

        LibraryRoll roll = libraryRolls.remove(uuid);
        if (roll != null && roll.matchesItem(e.getItem())) {
            // The offers shown were rolled by Infinite Library, so they must have been paid for to be used
            if (roll.state != RollState.PAID) {
                e.setCancelled(true);
                sendActionBar(player, Lang.getFor(player, "enchanting.library_no_lapis", "amount", EnchantingUtils.getInt("infinite-library.lapis-per-roll")));
                return;
            }
            Map<Enchantment, Integer> rolled = roll.results.get(e.whichButton());
            if (rolled != null) {
                e.getEnchantsToAdd().clear();
                e.getEnchantsToAdd().putAll(rolled);
            }
        }

        // Prevents table-enchanted items from being ground for bonus EXP
        ItemMeta itemMeta = e.getItem().getItemMeta();
        EnchantingUtils.markTableEnchanted(itemMeta);
        e.getItem().setItemMeta(itemMeta);

        EnchantingInventory inventory = (EnchantingInventory) e.getInventory();
        boolean canUseSkills = EnchantingUtils.canUseSkills(player);
        int level = EnchantingUtils.getLevel(player);

        if (canUseSkills && level >= EnchantingUtils.getUnlockLevel("arcane-ascension")) {
            applyArcaneAscension(player, e, level);
        }

        int levelsSpent = e.whichButton() + 1;
        int levelBefore = player.getLevel();
        int refund = 0;
        if (canUseSkills && level >= EnchantingUtils.getUnlockLevel("arcane-recall")) {
            refund = rollArcaneRecall(inventory, level, levelBefore, levelsSpent);
        }

        final int finalRefund = refund;
        final Location tableLocation = e.getEnchantBlock().getLocation();
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            // Ensures the tag is applied if the event's item was a copy of the enchanted item
            ItemStack enchanted = inventory.getItem();
            if (enchanted != null && !EnchantingUtils.isTableEnchanted(enchanted) && enchanted.hasItemMeta()) {
                ItemMeta meta = enchanted.getItemMeta();
                EnchantingUtils.markTableEnchanted(meta);
                enchanted.setItemMeta(meta);
                inventory.setItem(enchanted);
            }

            // Only refunds if the enchantment went through and the extra lapis is still in the table
            if (finalRefund > 0 && player.isOnline() && player.getLevel() < levelBefore
                    && removeLapis(inventory, levelsSpent)) {
                // The refund is split with the player's Essences like any other EXP gained
                player.giveExp(EssenceImbueListener.splitExp(player, finalRefund, true));
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "enchanting.recall_proc", "exp", finalRefund)));
                Location above = tableLocation.clone().add(0.5, 1.1, 0.5);
                tableLocation.getWorld().spawnParticle(Particle.DUST, above, 25, 0.3, 0.2, 0.3, LAPIS_DUST);
                tableLocation.getWorld().spawnParticle(Particle.ENCHANT, above, 40, 0.3, 0.4, 0.3, 0.5);
                player.playSound(tableLocation, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1F, 1.2F);
            }
        });
    }

    /**
     * Rolls Arcane Ascension, raising one eligible enchantment by a level beyond the vanilla maximum.
     */
    private void applyArcaneAscension(Player player, EnchantItemEvent e, int level) {
        double chance = EnchantingUtils.getScaled("arcane-ascension", "min-chance", "max-chance", level);
        if (random.nextDouble() >= chance) {
            return;
        }

        Map<Enchantment, Integer> enchants = e.getEnchantsToAdd();
        List<Enchantment> eligible = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (EnchantingUtils.OVER_LEVELED_ENCHANTS.contains(enchantment) && entry.getValue() <= enchantment.getMaxLevel()) {
                eligible.add(enchantment);
            }
        }
        if (eligible.isEmpty()) {
            return;
        }

        Enchantment chosen = eligible.get(random.nextInt(eligible.size()));
        int newLevel = enchants.get(chosen) + 1;
        enchants.put(chosen, newLevel);

        // Only boosts beyond the vanilla maximum are described as exceeding the limit
        String key = newLevel > chosen.getMaxLevel() ? "enchanting.ascension_proc" : "enchanting.ascension_proc_higher";
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, key, "enchant", EnchantingUtils.formatEnchantment(chosen))));
        Location above = e.getEnchantBlock().getLocation().add(0.5, 1.1, 0.5);
        above.getWorld().spawnParticle(Particle.END_ROD, above, 20, 0.2, 0.3, 0.2, 0.05);
        player.playSound(above, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1.5F);
    }

    /**
     * Rolls Arcane Recall, which requires the table to hold enough lapis to pay the enchantment's lapis cost twice.
     * @return The EXP to refund, or 0 if Arcane Recall did not proc.
     */
    private int rollArcaneRecall(EnchantingInventory inventory, int level, int playerLevel, int levelsSpent) {
        ItemStack lapis = inventory.getSecondary();
        if (lapis == null || lapis.getType() != Material.LAPIS_LAZULI || lapis.getAmount() < levelsSpent * 2) {
            return 0;
        }
        double chance = EnchantingUtils.getScaled("arcane-recall", "min-chance", "max-chance", level);
        if (random.nextDouble() >= chance) {
            return 0;
        }
        double refundPercent = EnchantingUtils.getScaled("arcane-recall", "min-refund", "max-refund", level);
        return (int) Math.floor(EnchantingUtils.getPointsOfLevels(playerLevel, levelsSpent) * refundPercent);
    }

    /**
     * Removes the input amount of lapis from the enchanting table.
     * @return Whether there was enough lapis to remove.
     */
    private boolean removeLapis(EnchantingInventory inventory, int amount) {
        ItemStack lapis = inventory.getSecondary();
        if (lapis == null || lapis.getType() != Material.LAPIS_LAZULI || lapis.getAmount() < amount) {
            return false;
        }
        lapis.setAmount(lapis.getAmount() - amount);
        inventory.setSecondary(lapis.getAmount() > 0 ? lapis : null);
        return true;
    }

    /**
     * Rolls new offers while Infinite Library is active, each roll costing lapis from the table.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareEnchant(PrepareItemEnchantEvent e) {
        Player player = e.getEnchanter();
        UUID uuid = player.getUniqueId();
        EnchantingInventory inventory = (EnchantingInventory) e.getInventory();
        ItemStack item = e.getItem();

        LibraryRoll roll = libraryRolls.get(uuid);
        if (roll != null) {
            if (roll.matchesItem(item) && roll.bonus == e.getEnchantmentBonus() && roll.state != RollState.FAILED) {
                applyRoll(e.getOffers(), roll);
                return;
            }
            libraryRolls.remove(uuid);
        }

        if (!EnchantingUtils.isInfiniteLibraryActive(player, e.getEnchantBlock().getLocation())) {
            return;
        }

        int lapisPerRoll = EnchantingUtils.getInt("infinite-library.lapis-per-roll");
        ItemStack lapis = inventory.getSecondary();
        if (lapis == null || lapis.getType() != Material.LAPIS_LAZULI || lapis.getAmount() < lapisPerRoll) {
            sendActionBar(player, Lang.getFor(player, "enchanting.library_no_lapis", "amount", lapisPerRoll));
            return;
        }

        ItemStack rolledItem = item.clone();
        rolledItem.setAmount(1);
        LibraryRoll newRoll = new LibraryRoll(rolledItem, e.getEnchantmentBonus());
        for (EnchantmentOffer offer : e.getOffers()) {
            newRoll.results.add(offer == null ? null : rollEnchantments(rolledItem, offer.getCost()));
        }
        applyRoll(e.getOffers(), newRoll);
        libraryRolls.put(uuid, newRoll);

        // The lapis is charged after the event, as changing the table's contents during it re-prepares the offers
        Location tableLocation = e.getEnchantBlock().getLocation();
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            if (libraryRolls.get(uuid) != newRoll) {
                return;
            }
            if (!newRoll.matchesItem(inventory.getItem()) || !removeLapis(inventory, lapisPerRoll)) {
                newRoll.state = RollState.FAILED;
                return;
            }
            newRoll.state = RollState.PAID;
            Location above = tableLocation.clone().add(0.5, 1.1, 0.5);
            tableLocation.getWorld().spawnParticle(Particle.ENCHANT, above, 60, 0.4, 0.5, 0.4, 1);
            tableLocation.getWorld().spawnParticle(Particle.DUST, above, 15, 0.3, 0.2, 0.3, LAPIS_DUST);
            player.playSound(tableLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1F, 1.4F);
        });
    }

    /**
     * Rolls a new set of enchantments for the item at the input level cost, following vanilla rules.
     * @return The rolled enchantments, or null if nothing could be rolled.
     */
    private Map<Enchantment, Integer> rollEnchantments(ItemStack item, int cost) {
        ItemStack result = Bukkit.getItemFactory().enchantWithLevels(item.clone(), cost, false, random);
        Map<Enchantment, Integer> enchants = new HashMap<>();
        if (result.getItemMeta() instanceof EnchantmentStorageMeta storageMeta) {
            enchants.putAll(storageMeta.getStoredEnchants());
            // Books only receive one fewer enchantment, as in vanilla
            if (item.getType() == Material.BOOK && enchants.size() > 1) {
                List<Enchantment> keys = new ArrayList<>(enchants.keySet());
                enchants.remove(keys.get(random.nextInt(keys.size())));
            }
        } else if (result.hasItemMeta()) {
            enchants.putAll(result.getItemMeta().getEnchants());
        }
        return enchants.isEmpty() ? null : enchants;
    }

    /**
     * Updates the offer hints to show one of the rolled enchantments for each offer.
     */
    private void applyRoll(EnchantmentOffer[] offers, LibraryRoll roll) {
        for (int i = 0; i < offers.length && i < roll.results.size(); i++) {
            Map<Enchantment, Integer> rolled = roll.results.get(i);
            if (offers[i] == null || rolled == null) {
                continue;
            }
            List<Enchantment> keys = new ArrayList<>(rolled.keySet());
            // The hint is chosen consistently so it does not change when the offers are re-prepared
            Enchantment hint = keys.get(Math.floorMod(roll.item.hashCode() + i, keys.size()));
            offers[i].setEnchantment(hint);
            offers[i].setEnchantmentLevel(rolled.get(hint));
        }
    }

    /**
     * Clears the Infinite Library roll once the item is removed from the table, so putting it back rolls again.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantingClick(InventoryClickEvent e) {
        if (e.getView().getType() == InventoryType.ENCHANTING && e.getWhoClicked() instanceof Player player) {
            clearRollIfItemRemoved(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantingDrag(InventoryDragEvent e) {
        if (e.getView().getType() == InventoryType.ENCHANTING && e.getWhoClicked() instanceof Player player) {
            clearRollIfItemRemoved(player);
        }
    }

    private void clearRollIfItemRemoved(Player player) {
        UUID uuid = player.getUniqueId();
        if (!libraryRolls.containsKey(uuid)) {
            return;
        }
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            if (!(player.getOpenInventory().getTopInventory() instanceof EnchantingInventory inventory)) {
                libraryRolls.remove(uuid);
                return;
            }
            ItemStack item = inventory.getItem();
            if (item == null || item.getType() == Material.AIR) {
                libraryRolls.remove(uuid);
            }
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getView().getType() == InventoryType.ENCHANTING) {
            libraryRolls.remove(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        libraryRolls.remove(uuid);
        EnchantingUtils.clearReady(uuid);
        EnchantingUtils.endInfiniteLibrary(uuid);
    }

    /**
     * Readies and activates Infinite Library on enchanting tables, and handles Purification on grindstones.
     * @param e The event.
     */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) {
            return;
        }
        Player player = e.getPlayer();
        if (!EnchantingUtils.canUseSkills(player)) {
            return;
        }

        Block block = e.getClickedBlock();
        if (block.getType() == Material.ENCHANTING_TABLE) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK && player.isSneaking()) {
                readyInfiniteLibrary(e, player);
            } else if (e.getAction() == Action.LEFT_CLICK_BLOCK && EnchantingUtils.consumeReady(player)) {
                e.setCancelled(true);
                activateInfiniteLibrary(player, block);
            }
        } else if (block.getType() == Material.GRINDSTONE && e.getAction() == Action.RIGHT_CLICK_BLOCK && player.isSneaking()) {
            purify(e, player, block);
        }
    }

    /**
     * Readies Infinite Library, to be activated by left-clicking the enchanting table.
     */
    private void readyInfiniteLibrary(PlayerInteractEvent e, Player player) {
        UUID uuid = player.getUniqueId();
        if (EnchantingUtils.isInfiniteLibraryRunning(uuid) || !EnchantingUtils.hasUnlocked(player, "infinite-library")) {
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        long remaining = aranarthPlayer.getInfiniteLibraryCooldownEnd() - System.currentTimeMillis();
        if (remaining > 0) {
            // The table still opens normally while the ability is recharging
            sendActionBar(player, Lang.getFor(player, "enchanting.too_tired", "seconds", (int) Math.ceil(remaining / 1000.0)));
            return;
        }

        e.setCancelled(true);
        if (EnchantingUtils.isReadied(player)) {
            return;
        }

        long expiry = EnchantingUtils.ready(player);
        sendActionBar(player, Lang.getFor(player, "enchanting.ready"));
        player.playSound(player, Sound.ITEM_ARMOR_EQUIP_GOLD, 1F, 1F);
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            if (EnchantingUtils.lowerIfUnused(uuid, expiry) && player.isOnline()) {
                sendActionBar(player, Lang.getFor(player, "enchanting.lower"));
            }
        }, EnchantingUtils.getInt("infinite-library.ready-seconds") * 20L);
    }

    /**
     * Activates Infinite Library and opens the enchanting table.
     */
    private void activateInfiniteLibrary(Player player, Block table) {
        UUID uuid = player.getUniqueId();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        long now = System.currentTimeMillis();
        if (aranarthPlayer.getInfiniteLibraryCooldownEnd() > now) {
            return;
        }

        long durationMillis = EnchantingUtils.getInfiniteLibraryDurationMillis(EnchantingUtils.getLevel(player));
        long cooldownMillis = EnchantingUtils.getInt("infinite-library.cooldown-seconds") * 1000L;
        long cooldownEnd = now + cooldownMillis;
        aranarthPlayer.setInfiniteLibraryCooldownEnd(cooldownEnd);
        EnchantingUtils.startInfiniteLibrary(player, table.getLocation(), durationMillis);

        sendActionBar(player, Lang.getFor(player, "enchanting.library_on"));
        player.playSound(table.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1F, 1.2F);
        Location above = table.getLocation().add(0.5, 1.1, 0.5);
        table.getWorld().spawnParticle(Particle.ENCHANT, above, 120, 0.5, 0.6, 0.5, 1.5);
        player.openEnchanting(table.getLocation(), false);
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "enchanting.library_reroll_hint",
                "amount", EnchantingUtils.getInt("infinite-library.lapis-per-roll"))));

        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            EnchantingUtils.endInfiniteLibrary(uuid);
            if (player.isOnline()) {
                sendActionBar(player, Lang.getFor(player, "enchanting.library_off"));
            }
        }, Math.max(1, durationMillis / 50));

        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            AranarthPlayer current = AranarthUtils.getPlayer(uuid);
            // Skipped if the cooldown was reset or restarted in the meantime
            if (player.isOnline() && current != null && current.getInfiniteLibraryCooldownEnd() == cooldownEnd) {
                sendActionBar(player, Lang.getFor(player, "enchanting.library_refresh"));
            }
        }, cooldownMillis / 50 + 1);
    }

    /**
     * Lifts the curses of the held item in exchange for levels, with a chance of failure.
     */
    private void purify(PlayerInteractEvent e, Player player, Block grindstone) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        Map<Enchantment, Integer> enchants = meta instanceof EnchantmentStorageMeta storageMeta
                ? storageMeta.getStoredEnchants() : meta.getEnchants();
        List<Enchantment> curses = new ArrayList<>();
        for (Enchantment enchantment : enchants.keySet()) {
            if (enchantment.isCursed()) {
                curses.add(enchantment);
            }
        }
        if (curses.isEmpty()) {
            return;
        }

        e.setCancelled(true);
        int level = EnchantingUtils.getLevel(player);
        int unlockLevel = EnchantingUtils.getUnlockLevel("purification");
        if (level < unlockLevel) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "enchanting.purification_locked", "level", unlockLevel)));
            return;
        }

        int cost = EnchantingUtils.getPurificationLevelsPerCurse(level) * curses.size();
        if (player.getLevel() < cost) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "enchanting.purification_not_enough_levels", "levels", cost)));
            return;
        }
        player.setLevel(player.getLevel() - cost);

        Location above = grindstone.getLocation().add(0.5, 1.1, 0.5);
        double chance = EnchantingUtils.getScaled("purification", "min-chance", "max-chance", level);
        if (random.nextDouble() >= chance) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "enchanting.purification_fail", "levels", cost)));
            grindstone.getWorld().spawnParticle(Particle.SMOKE, above, 20, 0.2, 0.2, 0.2, 0.02);
            player.playSound(grindstone.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1F, 1F);
            return;
        }

        for (Enchantment curse : curses) {
            if (meta instanceof EnchantmentStorageMeta storageMeta) {
                storageMeta.removeStoredEnchant(curse);
            } else {
                meta.removeEnchant(curse);
            }
        }
        item.setItemMeta(meta);
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "enchanting.purification_success", "levels", cost)));
        grindstone.getWorld().spawnParticle(Particle.END_ROD, above, 25, 0.2, 0.3, 0.2, 0.05);
        player.playSound(grindstone.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1F, 1F);
        player.playSound(grindstone.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.6F, 1.5F);
    }

    /**
     * Records the grindstone result being taken, so the EXP it drops can be boosted.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGrindstoneTake(InventoryClickEvent e) {
        if (e.getView().getType() != InventoryType.GRINDSTONE || e.getRawSlot() != 2) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player player) || !EnchantingUtils.canUseSkills(player)) {
            return;
        }
        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }
        GrindstoneInventory grindstone = (GrindstoneInventory) e.getView().getTopInventory();
        Location location = grindstone.getLocation();
        if (location == null) {
            return;
        }
        boolean isTableEnchanted = EnchantingUtils.isTableEnchanted(grindstone.getUpperItem())
                || EnchantingUtils.isTableEnchanted(grindstone.getLowerItem());
        Block block = location.getBlock();
        PendingGrind pending = new PendingGrind(player.getUniqueId(), isTableEnchanted, EnchantingUtils.getLevel(player), Bukkit.getCurrentTick());
        pendingGrinds.put(block, pending);
        // The EXP is dropped in the same tick, so the pending grind is no longer needed afterwards
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> pendingGrinds.remove(block, pending));
    }

    /**
     * Clears the table-enchanted tag from grindstone results that no longer have any enchantments other than curses.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareGrindstone(PrepareGrindstoneEvent e) {
        ItemStack result = e.getResult();
        if (!EnchantingUtils.isTableEnchanted(result)) {
            return;
        }
        ItemMeta meta = result.getItemMeta();
        Map<Enchantment, Integer> enchants = meta instanceof EnchantmentStorageMeta storageMeta
                ? storageMeta.getStoredEnchants() : meta.getEnchants();
        for (Enchantment enchantment : enchants.keySet()) {
            if (!enchantment.isCursed()) {
                return;
            }
        }
        ItemStack untagged = result.clone();
        ItemMeta untaggedMeta = untagged.getItemMeta();
        untaggedMeta.getPersistentDataContainer().remove(TABLE_ENCHANTED);
        untagged.setItemMeta(untaggedMeta);
        e.setResult(untagged);
    }

    /**
     * Awards Repair XP for grindstone EXP, and boosts it with Distilled Wisdom unless the item was table-enchanted.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onGrindstoneExp(BlockExpEvent e) {
        // Block breaks are also BlockExpEvents, so only the grindstone's own EXP drop is handled
        // The block is not checked, as portable grindstones drop their EXP at the player's position
        if (e.getClass() != BlockExpEvent.class) {
            return;
        }
        PendingGrind pending = pendingGrinds.remove(e.getBlock());
        if (pending == null || pending.tick() != Bukkit.getCurrentTick()) {
            return;
        }
        Player player = Bukkit.getPlayer(pending.uuid());
        int baseExp = e.getExpToDrop();
        if (player == null || baseExp <= 0) {
            return;
        }

        addRepairXp(player, baseExp * EnchantingUtils.getInt("repair-xp.per-grindstone-exp"));

        if (pending.isTableEnchanted() || pending.level() < EnchantingUtils.getUnlockLevel("distilled-wisdom")) {
            return;
        }
        double multiplier = EnchantingUtils.getScaled("distilled-wisdom", "min-multiplier", "max-multiplier", pending.level());
        e.setExpToDrop(EnchantingUtils.randomRound(baseExp * multiplier));
    }

    /**
     * Carries the table-enchanted tag over to anvil results, so it cannot be removed by combining items.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        ItemStack result = e.getResult();
        if (result == null || result.getType() == Material.AIR || !result.hasItemMeta() || EnchantingUtils.isTableEnchanted(result)) {
            return;
        }
        if (!EnchantingUtils.isTableEnchanted(e.getInventory().getItem(0)) && !EnchantingUtils.isTableEnchanted(e.getInventory().getItem(1))) {
            return;
        }
        ItemStack tagged = result.clone();
        ItemMeta meta = tagged.getItemMeta();
        // Only enchanted results need the tag, such as not when crushing an Aranarthium Ingot into an Essence
        boolean hasEnchants = meta instanceof EnchantmentStorageMeta storageMeta ? storageMeta.hasStoredEnchants() : meta.hasEnchants();
        if (!hasEnchants) {
            return;
        }
        EnchantingUtils.markTableEnchanted(meta);
        tagged.setItemMeta(meta);
        e.setResult(tagged);
    }

    /**
     * Awards Repair XP based on the levels spent on an anvil, excluding renaming alone.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilTake(InventoryClickEvent e) {
        if (e.getView().getType() != InventoryType.ANVIL || e.getRawSlot() != 2 || !(e.getView() instanceof AnvilView anvilView)) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player player) || !EnchantingUtils.canUseSkills(player)) {
            return;
        }
        ItemStack result = e.getCurrentItem();
        ItemStack sacrifice = anvilView.getTopInventory().getItem(1);
        int cost = anvilView.getRepairCost();
        if (result == null || result.getType() == Material.AIR || sacrifice == null || sacrifice.getType() == Material.AIR || cost <= 0) {
            return;
        }

        int levelBefore = player.getLevel();
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            // Only awards if the levels were actually spent, as the result may not have been taken
            if (player.isOnline() && player.getLevel() <= levelBefore - cost) {
                addRepairXp(player, cost * EnchantingUtils.getInt("repair-xp.per-anvil-level"));
            }
        });
    }

    /**
     * Carries the table-enchanted tag over to books extracted by mcMMO Arcane Salvaging.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSalvage(McMMOPlayerSalvageCheckEvent e) {
        ItemStack book = e.getEnchantedBook();
        if (book == null || !book.hasItemMeta() || !EnchantingUtils.isTableEnchanted(e.getSalvageItem())) {
            return;
        }
        ItemMeta meta = book.getItemMeta();
        EnchantingUtils.markTableEnchanted(meta);
        book.setItemMeta(meta);
    }

    /**
     * Resets the Infinite Library cooldown alongside the mcMMO cooldowns when /mcrefresh is used.
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        handleMcrefresh(e.getPlayer(), e.getMessage().substring(1));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent e) {
        handleMcrefresh(e.getSender(), e.getCommand().startsWith("/") ? e.getCommand().substring(1) : e.getCommand());
    }

    private void handleMcrefresh(CommandSender sender, String commandLine) {
        String[] parts = commandLine.trim().split("\\s+");
        String label = parts[0].toLowerCase();
        if (!label.equals("mcrefresh") && !label.equals("mcmmo:mcrefresh")) {
            return;
        }

        Player target;
        if (parts.length >= 2) {
            if (!sender.hasPermission("mcmmo.commands.mcrefresh.others")) {
                return;
            }
            target = Bukkit.getPlayerExact(parts[1]);
        } else {
            if (!sender.hasPermission("mcmmo.commands.mcrefresh") || !(sender instanceof Player player)) {
                return;
            }
            target = player;
        }
        if (target == null) {
            return;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
        if (aranarthPlayer != null) {
            aranarthPlayer.setInfiniteLibraryCooldownEnd(0);
        }
    }

    private void addRepairXp(Player player, int xp) {
        if (xp <= 0) {
            return;
        }
        try {
            ExperienceAPI.addXP(player, "Repair", xp, "UNKNOWN");
        } catch (Exception ignored) {
            // The player's mcMMO profile is not loaded
        }
    }

    private void sendActionBar(Player player, String message) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor(message)));
    }
}
