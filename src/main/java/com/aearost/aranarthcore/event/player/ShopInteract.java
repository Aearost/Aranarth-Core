package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CropUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Handles the interacting with a player shop.
 */
public class ShopInteract {

    public void execute(PlayerInteractEvent e) {
        // Left or right-clicking the sign, including placing and breaking
        if (e.getClickedBlock().getType().name().endsWith("_SIGN")) {
            Player player = e.getPlayer();
            AranarthPlayer clickUser = AranarthUtils.getPlayer(player.getUniqueId());
            Location signLocation = e.getClickedBlock().getLocation();
            Location locationBelow = new Location(signLocation.getWorld(),
                    signLocation.getBlockX(), signLocation.getBlockY() - 1, signLocation.getBlockZ());
            Shop shop = ShopUtils.getShopFromLocation(signLocation);

            // Player shop
            if (isContainer(locationBelow.getBlock()) && shop != null && AranarthUtils.getPlayer(shop.getUuid()) != null) {
                if (shop != null) {
                    e.setCancelled(true);

                    AranarthPlayer shopUser = AranarthUtils.getPlayer(shop.getUuid());

                    if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        // If editing your own shop
                        if (shop.getUuid().equals(player.getUniqueId())) {
                            e.setCancelled(false);
                            return;
                        }

                        // Enables bulk mode for the purchase
                        if (clickUser.getBulkTransactionNum() == 1 && player.isSneaking()) {
                            shop = ShopUtils.getBulkShop(shop, player, true);
                        }
                        // The user is just toggling the bulk purchase mode
                        else if (clickUser.getBulkTransactionNum() == 0 && player.isSneaking()) {
                            return;
                        }

                        handleBuyLogic(player, clickUser, shopUser, shop, locationBelow);
                    } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                        // If editing your own shop
                        if (shop.getUuid().equals(player.getUniqueId())) {
                            e.setCancelled(false);
                            return;
                        }

                        // Just completed a bulk transaction - skip while sneaking to avoid re-triggering
                        if (clickUser.getBulkTransactionNum() == -1 && player.isSneaking()) {
                            return;
                        }

                        // Enables bulk mode for the sale
                        if (clickUser.getBulkTransactionNum() == 1 && player.isSneaking()) {
                            shop = ShopUtils.getBulkShop(shop, player, false);
                        }
                        // The user is just toggling the bulk sale mode
                        else if (clickUser.getBulkTransactionNum() == 0 && player.isSneaking()) {
                            return;
                        }

                        handleSellLogic(e, player, clickUser, shopUser, shop, locationBelow);
                    }
                } else {
                    if (player.isSneaking()) {
                        if (e.getClickedBlock().getState() instanceof Sign sign) {
                            player.openSign(sign);
                        }
                    }
                }
            }
            // Server shop
            // If the clicked block is a sign but the block below is not a chest
            else {
                if (shop != null) {
                    if (shop.getUuid() == null) {
                        e.setCancelled(true);
                        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

                        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            // If editing a server shop
                            if (aranarthPlayer.getCouncilRank() == 3 && player.isSneaking() && player.getGameMode() == GameMode.CREATIVE) {
                                if (e.getClickedBlock().getState() instanceof Sign sign) {
                                    player.openSign(sign);
                                }
                                return;
                            }

                            // Enables bulk mode for the purchase
                            if (clickUser.getBulkTransactionNum() == 1 && player.isSneaking()) {
                                shop = ShopUtils.getBulkShop(shop, player, true);
                            }
                            // The user is just toggling the bulk purchase mode
                            else if (clickUser.getBulkTransactionNum() == 0 && player.isSneaking()) {
                                return;
                            }

                            handleBuyLogic(player, clickUser, null, shop, null);
                        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                            // Just completed a bulk transaction - skip while sneaking to avoid re-triggering
                            if (clickUser.getBulkTransactionNum() == -1 && player.isSneaking()) {
                                return;
                            }

                            // Enables bulk mode for the sale
                            if (clickUser.getBulkTransactionNum() == 1 && player.isSneaking()) {
                                shop = ShopUtils.getBulkShop(shop, player, false);
                            }
                            // The user is just toggling the bulk sale mode
                            else if (clickUser.getBulkTransactionNum() == 0 && player.isSneaking()) {
                                return;
                            }
                            if (player.getGameMode() != GameMode.CREATIVE) {
                                handleSellLogic(e, player, clickUser, null, shop, null);
                            } else {
                                e.setCancelled(false);
                                ShopUtils.removeShop(shop);
                                player.sendMessage(ChatUtils.chatMessage("&7You have destroyed this shop"));
                                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 0.1F);
                            }
                        }
                    }
                } else {
                    if (player.isSneaking()) {
                        if (e.getClickedBlock().getState() instanceof Sign sign) {
                            player.openSign(sign);
                        }
                    }
                }
            }
        }
        // Left or right-clicking the chest, including opening and breaking
        else if (isContainer(e.getClickedBlock())) {
            // Gets both locations if it is a double chest
            BlockState state = e.getClickedBlock().getState();
            Container container = (Container) state;
            Location[] locations = new Location[2];
            if (container.getInventory().getHolder() instanceof DoubleChest doubleChest) {
                Chest leftChest = (Chest) doubleChest.getLeftSide();
                Chest rightChest = (Chest) doubleChest.getRightSide();
                locations[0] = leftChest.getLocation();
                locations[1] = rightChest.getLocation();
            } else {
                locations[0] = e.getClickedBlock().getLocation();
            }

            Shop location1Shop = ShopUtils.getShopFromLocation(locations[0].getBlock().getRelative(BlockFace.UP).getLocation());
            Shop location2Shop = null;
            if (ShopUtils.getShopFromLocation(locations[1]) != null) {
                location2Shop = ShopUtils.getShopFromLocation(locations[1].getBlock().getRelative(BlockFace.UP).getLocation());
            }

            if (location1Shop != null || (locations[1] != null && location2Shop != null)) {
                if (!location1Shop.getUuid().equals(e.getPlayer().getUniqueId()) || (location2Shop != null && !location2Shop.getUuid().equals(e.getPlayer().getUniqueId()))) {

                    AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
                    if (!aranarthPlayer.isInAdminMode()) {
                        // Prevents other players from destroying or opening the chest
                        e.setCancelled(true);
                        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                            e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot destroy someone else's shop!"));
                        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot open someone else's player shop chest!"));
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles all logic involving buying from a shop.
     *
     * @param player        The player buying from the shop.
     * @param clickUser     The user that clicked the shop sign.
     * @param shopUser      The user that owns the shop.
     * @param shop          The shop.
     * @param locationBelow The location below the shop sign.
     */
    private void handleBuyLogic(Player player, AranarthPlayer clickUser, AranarthPlayer shopUser, Shop shop, Location locationBelow) {
        // Buy and edit logic
        if (shop.getBuyPrice() > 0) {
            if (clickUser.getBalance() >= shop.getBuyPrice()) {
                boolean isPlayerShop = locationBelow != null;
                Inventory chestInventory = null;
                if (isPlayerShop) {
                    BlockState state = locationBelow.getBlock().getState();
                    Container container = (Container) state;
                    chestInventory = container.getInventory();
                    if (chestInventory.getHolder() instanceof DoubleChest doubleChest) {
                        chestInventory = doubleChest.getInventory(); // Get the full 54 slot inventory
                    }
                }

                if (isPlayerShop) {
                    // Verifies that there is enough quantity in the chest's inventory
                    // Cycles through the chest's inventory starting from end to beginning
                    HashMap<Boolean, ItemStack[]> result = checkIfContentsHasShopItems(chestInventory.getContents(), shop, true);
                    if (result.containsKey(true)) {
                        chestInventory.clear();
                        chestInventory.setContents(result.get(true));
                    } else {
                        player.sendMessage(ChatUtils.chatMessage("&cThere is not enough inventory in this shop!"));
                        return;
                    }
                }

                // Verifies there is enough space in the player's inventory to add the items
                int spaceForShopItemInPlayerInventory = 0;
                for (ItemStack inventoryItem : player.getInventory().getStorageContents()) {
                    if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
                        spaceForShopItemInPlayerInventory += 64;
                        continue;
                    }

                    // Each month changes the lore of crop seeds, must consider that
                    boolean isSameCropSeed = inventoryItem.getType() == shop.getItem().getType() && CropUtils.isCropSeed(inventoryItem.getType());
                    if (inventoryItem.isSimilar(shop.getItem()) || isSameCropSeed) {
                        spaceForShopItemInPlayerInventory += inventoryItem.getMaxStackSize() - inventoryItem.getAmount();
                    }
                }
                // Also count non-full stacks inside shulker boxes if player has aranarth.shulker permission
                if (player.hasPermission("aranarth.shulker") && !isShulkerBox(shop.getItem())) {
                    for (ItemStack inventoryItem : player.getInventory().getStorageContents()) {
                        if (!isShulkerBox(inventoryItem)) {
                            continue;
                        }
                        BlockStateMeta bsm = (BlockStateMeta) inventoryItem.getItemMeta();
                        ShulkerBox shulker = (ShulkerBox) bsm.getBlockState();
                        for (ItemStack shulkerItem : shulker.getInventory().getContents()) {
                            if (shulkerItem == null) {
                                continue;
                            }
                            boolean isSameCropSeed = shulkerItem.getType() == shop.getItem().getType() && CropUtils.isCropSeed(shulkerItem.getType());
                            if (shulkerItem.isSimilar(shop.getItem()) || isSameCropSeed) {
                                spaceForShopItemInPlayerInventory += shulkerItem.getMaxStackSize() - shulkerItem.getAmount();
                            }
                        }
                    }
                }
                if (spaceForShopItemInPlayerInventory < shop.getQuantity()) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough space for this!"));
                    clickUser.setBulkTransactionNum(-1);
                    AranarthUtils.setPlayer(player.getUniqueId(), clickUser);
                    return;
                }

                NumberFormat formatter = NumberFormat.getCurrencyInstance();

                // Logic to update balances and chest inventory
                clickUser.setBalance(clickUser.getBalance() - shop.getBuyPrice());
                if (shopUser != null) {
                    shopUser.setBalance(shopUser.getBalance() + shop.getBuyPrice());
                }

                // Logic to add items to player's inventory
                int quantityToDistribute = shop.getQuantity();

                // If player has aranarth.shulker, fill non-full stacks in shulker boxes first
                if (player.hasPermission("aranarth.shulker") && !isShulkerBox(shop.getItem())) {
                    for (int slotIndex = 0; slotIndex < 36 && quantityToDistribute > 0; slotIndex++) {
                        ItemStack invItem = player.getInventory().getItem(slotIndex);
                        if (!isShulkerBox(invItem)) {
                            continue;
                        }
                        BlockStateMeta bsm = (BlockStateMeta) invItem.getItemMeta();
                        ShulkerBox shulker = (ShulkerBox) bsm.getBlockState();
                        Inventory shulkerInv = shulker.getInventory();
                        boolean modified = false;

                        for (int shulkerSlot = 0; shulkerSlot < shulkerInv.getSize() && quantityToDistribute > 0; shulkerSlot++) {
                            ItemStack shulkerItem = shulkerInv.getItem(shulkerSlot);
                            if (shulkerItem == null) {
                                continue;
                            }
                            boolean isSameCropSeed = shulkerItem.getType() == shop.getItem().getType() && CropUtils.isCropSeed(shop.getItem().getType());
                            if (shulkerItem.isSimilar(shop.getItem()) || isSameCropSeed) {
                                int spaceInStack = shulkerItem.getMaxStackSize() - shulkerItem.getAmount();
                                if (spaceInStack > 0) {
                                    int addAmount = Math.min(spaceInStack, quantityToDistribute);
                                    shulkerItem.setAmount(shulkerItem.getAmount() + addAmount);
                                    quantityToDistribute -= addAmount;
                                    shulkerInv.setItem(shulkerSlot, shulkerItem);
                                    modified = true;
                                }
                            }
                        }

                        if (modified) {
                            bsm.setBlockState(shulker);
                            invItem.setItemMeta(bsm);
                            player.getInventory().setItem(slotIndex, invItem);
                        }
                    }
                }

                HashMap<Integer, ItemStack> remainder = new HashMap<>();
                if (quantityToDistribute > 0) {
                    ItemStack itemToAdd = shop.getItem().clone();
                    itemToAdd.setAmount(quantityToDistribute);
                    remainder = player.getInventory().addItem(itemToAdd);
                    for (Integer index : remainder.keySet()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), remainder.get(index));
                    }
                }
                String itemname = ChatUtils.getFormattedItemName(shop.getItem().getType().name());
                if (shop.getItem().hasItemMeta()) {
                    if (shop.getItem().getItemMeta().hasDisplayName()) {
                        itemname = shop.getItem().getItemMeta().getDisplayName();
                    }
                }

                player.sendMessage(ChatUtils.chatMessage(
                        "&7You have purchased &e" + shop.getQuantity() + " " + itemname
                                + ChatUtils.translateToColor(" &7for &6" + formatter.format(shop.getBuyPrice()))));
                clickUser.setBulkTransactionNum(-1);
                AranarthUtils.setPlayer(player.getUniqueId(), clickUser);

                if (!remainder.isEmpty()) {
                    player.sendMessage(ChatUtils.chatMessage("&e" + remainder.size() + " " + itemname
                            + ChatUtils.translateToColor(" &7was dropped on the ground!")));
                }

                // If the shop owner is online
                if (isPlayerShop) {
                    if (Bukkit.getPlayer(shop.getUuid()) != null) {
                        Player shopPlayer = Bukkit.getPlayer(shop.getUuid());
                        shopPlayer.sendMessage(
                                ChatUtils.chatMessage("&e" + player.getName() + " &7has purchased &e" + shop.getQuantity() + " "
                                        + itemname + ChatUtils.translateToColor(" &7for &6" + formatter.format(shop.getBuyPrice()))));
                    }
                }
            } else {
                if (clickUser.getBulkTransactionNum() <= 0 && player.isSneaking()) {
                    return;
                }
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough money to buy this!"));
            }
        }
    }

    /**
     * Handles all logic involving selling to a shop.
     *
     * @param e             The event.
     * @param player        The player selling the shop.
     * @param clickUser     The user that clicked the shop sign.
     * @param shopUser      The user that owns the shop.
     * @param shop          The shop.
     * @param locationBelow The location below the shop sign.
     */
    private void handleSellLogic(PlayerInteractEvent e, Player player, AranarthPlayer clickUser, AranarthPlayer shopUser, Shop shop, Location locationBelow) {
        if (shop.getSellPrice() > 0) {
            boolean isPlayerShop = locationBelow != null;

            // For bulk sales with aranarth.shulker perm: compute "leave one per shulker slot" adjusted inventory
            boolean useLeaveOneLogic = clickUser.getBulkTransactionNum() == 1
                    && player.hasPermission("aranarth.shulker")
                    && !isShulkerBox(shop.getItem())
                    && !clickUser.isBulkSellShulkerEnabled();
            HashMap<Integer, ItemStack[]> leaveOneResult = null;
            if (useLeaveOneLogic) {
                leaveOneResult = computeLeaveOneShulkerContents(player.getInventory().getContents(), shop);
                int actualQuantity = leaveOneResult.keySet().iterator().next();
                if (actualQuantity > 0) {
                    // Align to a multiple of the original shop's unit quantity
                    Shop originalShop = ShopUtils.getShopFromLocation(shop.getLocation());
                    int originalUnit = (originalShop != null) ? originalShop.getQuantity() : 1;
                    int alignedQuantity = (actualQuantity / originalUnit) * originalUnit;
                    if (alignedQuantity <= 0) {
                        leaveOneResult = null;
                    } else {
                        ItemStack[] modifiedContents = leaveOneResult.values().iterator().next();
                        if (alignedQuantity < actualQuantity) {
                            // Put the leftover back into the first shulker slot that contains the item
                            putBackIntoFirstShulkerSlot(modifiedContents, shop, actualQuantity - alignedQuantity);
                        }
                        leaveOneResult = new HashMap<>();
                        leaveOneResult.put(alignedQuantity, modifiedContents);
                        double pricePerUnit = shop.getSellPrice() / shop.getQuantity();
                        shop.setQuantity(alignedQuantity);
                        shop.setSellPrice(pricePerUnit * alignedQuantity);
                    }
                } else {
                    leaveOneResult = null;
                }
            }

            if (isPlayerShop) {
                if (shopUser.getBalance() < shop.getSellPrice()) {
                    player.sendMessage(ChatUtils.chatMessage("&cThis player does not have enough money to sell this!"));
                    return;
                }
            }

            Inventory chestInventory = null;
            Container copyOfChest = null;

            if (isPlayerShop) {
                BlockState state = locationBelow.getBlock().getState();
                Container container = (Container) state;
                copyOfChest = (Container) container.copy();
                chestInventory = container.getInventory();
                if (chestInventory.getHolder() instanceof DoubleChest doubleChest) {
                    chestInventory = doubleChest.getInventory(); // Get the full 54 slot inventory
                }

                // Verifies there is enough space in the chest's inventory to add the items
                int spaceForShopItemInChestInventory = 0;
                for (ItemStack chestItem : copyOfChest.getInventory().getStorageContents()) {
                    if (chestItem == null || chestItem.getType() == Material.AIR) {
                        spaceForShopItemInChestInventory += 64;
                        continue;
                    }

                    // Each month changes the lore of crop seeds, must consider that
                    boolean isSameCropSeed = chestItem.getType() == shop.getItem().getType() && CropUtils.isCropSeed(chestItem.getType());
                    if (chestItem.isSimilar(shop.getItem()) || isSameCropSeed) {
                        spaceForShopItemInChestInventory += chestItem.getMaxStackSize() - chestItem.getAmount();
                    }
                }
                if (spaceForShopItemInChestInventory < shop.getQuantity()) {
                    player.sendMessage(ChatUtils.chatMessage("&cThere is no space remaining in the chest!"));
                    clickUser.setBulkTransactionNum(-1);
                    AranarthUtils.setPlayer(player.getUniqueId(), clickUser);
                    return;
                }
            }

            // Verifies the player has the items
            Inventory playerInventory = player.getInventory();
            HashMap<Boolean, ItemStack[]> result;
            if (leaveOneResult != null) {
                result = new HashMap<>();
                result.put(true, leaveOneResult.values().iterator().next());
            } else if (useLeaveOneLogic) {
                // Leave-one logic was active but nothing could be sold (only 1 item per shulker slot).
                // Do NOT fall back to checkIfContentsHasShopItems — that would consume the kept items.
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough of this item!"));
                clickUser.setBulkTransactionNum(-1);
                AranarthUtils.setPlayer(player.getUniqueId(), clickUser);
                return;
            } else {
                // When the player is in leave-one mode (aranarth.shulker perm, bulkSellShulker off,
                // not selling a shulker), don't let a regular sell drain protected shulker slots.
                boolean leaveOneMode = player.hasPermission("aranarth.shulker")
                        && !isShulkerBox(shop.getItem())
                        && !clickUser.isBulkSellShulkerEnabled();
                result = checkIfContentsHasShopItems(playerInventory.getContents(), shop, !leaveOneMode);
            }

            if (result.containsKey(true)) {
                NumberFormat formatter = NumberFormat.getCurrencyInstance();

                // Logic to update balances and chest inventory
                clickUser.setBalance(clickUser.getBalance() + shop.getSellPrice());
                if (shopUser != null) {
                    shopUser.setBalance(shopUser.getBalance() - shop.getSellPrice());
                    ItemStack shopItem = shop.getItem().clone();
                    shopItem.setAmount(shop.getQuantity());
                    chestInventory.addItem(shopItem);
                }

                // Logic to remove items from the player's inventory
                playerInventory.clear();
                playerInventory.setContents(result.get(true));

                String itemname = ChatUtils.getFormattedItemName(shop.getItem().getType().name());
                if (shop.getItem().hasItemMeta()) {
                    if (shop.getItem().getItemMeta().hasDisplayName()) {
                        itemname = shop.getItem().getItemMeta().getDisplayName();
                    }
                }

                player.sendMessage(ChatUtils.chatMessage(
                        "&7You have sold &e" + shop.getQuantity() + " " + itemname
                                + ChatUtils.translateToColor(" &7for &6" + formatter.format(shop.getSellPrice()))));
                clickUser.setBulkTransactionNum(-1);
                AranarthUtils.setPlayer(player.getUniqueId(), clickUser);

                // If the shop owner is online
                if (isPlayerShop) {
                    if (Bukkit.getPlayer(shop.getUuid()) != null) {
                        Player shopPlayer = Bukkit.getPlayer(shop.getUuid());
                        shopPlayer.sendMessage(
                                ChatUtils.chatMessage("&e" + player.getName() + " &7has sold you &e" + shop.getQuantity() + " "
                                        + itemname + ChatUtils.translateToColor(" &7for &6" + formatter.format(shop.getSellPrice()))));
                    }
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough of this item!"));
            }
        } else {
            if (clickUser.getBulkTransactionNum() == 1 && player.isSneaking()) {
                return;
            }
            if (shop.getUuid() != null) {
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot destroy someone else's shop!"));
            } else {
                if (AranarthUtils.getPlayer(player.getUniqueId()).getCouncilRank() < 3) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou cannot destroy a server shop!"));
                }
            }
        }
    }

    /**
     * Adds the given amount back to the first shulker slot (scanning inventory from slot 0)
     * that contains the shop item, after a leave-one alignment leftover.
     *
     * @param contents The modified inventory contents (mutated in place).
     * @param shop     The shop whose item is being matched.
     * @param amount   The number of items to add back.
     */
    private void putBackIntoFirstShulkerSlot(ItemStack[] contents, Shop shop, int amount) {
        for (ItemStack content : contents) {
            if (!isShulkerBox(content)) {
                continue;
            }
            if (!(content.getItemMeta() instanceof BlockStateMeta bsm)) {
                continue;
            }
            if (!(bsm.getBlockState() instanceof ShulkerBox shulker)) {
                continue;
            }
            Inventory shulkerInv = shulker.getInventory();
            ItemStack[] shulkerContents = shulkerInv.getContents();
            for (ItemStack shulkerContent : shulkerContents) {
                if (shulkerContent == null) {
                    continue;
                }
                boolean isSameCropSeed = shulkerContent.getType() == shop.getItem().getType() && CropUtils.isCropSeed(shop.getItem().getType());
                if (shulkerContent.isSimilar(shop.getItem()) || isSameCropSeed) {
                    shulkerContent.setAmount(shulkerContent.getAmount() + amount);
                    shulkerInv.setContents(shulkerContents);
                    bsm.setBlockState(shulker);
                    content.setItemMeta(bsm);
                    return;
                }
            }
        }
    }

    /**
     * Computes the modified inventory for a "leave one per shulker slot" bulk sale.
     * Takes all matching items from regular inventory slots, and takes (amount - 1) from each
     * shulker slot containing the item (for maxStackSize > 1 items), or the full 1 for
     * maxStackSize == 1 items. The total is capped at the shop's original bulk quantity.
     *
     * @param inventory The player's storage contents.
     * @param shop      The shop being sold to (quantity used as the cap).
     * @return A map of {actualAmountCollected -> modifiedInventoryContents}.
     */
    private HashMap<Integer, ItemStack[]> computeLeaveOneShulkerContents(ItemStack[] inventory, Shop shop) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack is : inventory) {
            items.add(is == null ? null : is.clone());
        }
        ItemStack[] contents = items.toArray(new ItemStack[0]);

        int bulkQuantity = shop.getQuantity();

        // Step 1: Calculate how much shulker slots can contribute (amount - 1 per slot for maxStackSize > 1)
        int shulkerContribution = 0;
        for (ItemStack invItem : contents) {
            if (!isShulkerBox(invItem)) {
                continue;
            }
            if (!(invItem.getItemMeta() instanceof BlockStateMeta bsm)) {
                continue;
            }
            if (!(bsm.getBlockState() instanceof ShulkerBox shulker)) {
                continue;
            }
            for (ItemStack shulkerItem : shulker.getInventory().getContents()) {
                if (shulkerItem == null) {
                    continue;
                }
                boolean isSameCropSeed = shulkerItem.getType() == shop.getItem().getType() && CropUtils.isCropSeed(shop.getItem().getType());
                if (shulkerItem.isSimilar(shop.getItem()) || isSameCropSeed) {
                    shulkerContribution += shulkerItem.getMaxStackSize() > 1
                            ? shulkerItem.getAmount() - 1
                            : 1;
                }
            }
        }

        // Step 2: Take matching items from regular (non-shulker) inventory slots.
        // When shulker items contribute, clear all regular items first so the inventory
        // is fully emptied and any leftover after alignment is returned to the shulker.
        // When no shulker items contribute, only take what is needed (cap at bulkQuantity).
        int collectedFromRegular = 0;
        for (int i = contents.length - 1; i >= 0; i--) {
            if (contents[i] == null || isShulkerBox(contents[i])) {
                continue;
            }
            boolean isSameCropSeed = contents[i].getType() == shop.getItem().getType() && CropUtils.isCropSeed(shop.getItem().getType());
            if (contents[i].isSimilar(shop.getItem()) || isSameCropSeed) {
                int take = shulkerContribution > 0
                        ? contents[i].getAmount()
                        : Math.min(bulkQuantity - collectedFromRegular, contents[i].getAmount());
                if (take <= 0) continue;
                collectedFromRegular += take;
                int remaining = contents[i].getAmount() - take;
                if (remaining <= 0) {
                    contents[i] = null;
                } else {
                    contents[i].setAmount(remaining);
                }
                if (shulkerContribution == 0 && collectedFromRegular >= bulkQuantity) break;
            }
        }

        int effectiveShulkerContribution = Math.min(shulkerContribution, Math.max(0, bulkQuantity - collectedFromRegular));

        // Step 3: Take (amount - 1) per shulker slot, capped at effectiveShulkerContribution
        int collectedFromShulker = 0;
        for (int i = contents.length - 1; i >= 0 && collectedFromShulker < effectiveShulkerContribution; i--) {
            if (contents[i] == null || !isShulkerBox(contents[i])) {
                continue;
            }
            if (!(contents[i].getItemMeta() instanceof BlockStateMeta bsm)) {
                continue;
            }
            if (!(bsm.getBlockState() instanceof ShulkerBox shulker)) {
                continue;
            }

            Inventory shulkerInv = shulker.getInventory();
            ItemStack[] shulkerContents = shulkerInv.getContents().clone();
            boolean modified = false;

            for (int j = shulkerContents.length - 1; j >= 0 && collectedFromShulker < effectiveShulkerContribution; j--) {
                if (shulkerContents[j] == null) {
                    continue;
                }
                boolean isSameCropSeed = shulkerContents[j].getType() == shop.getItem().getType() && CropUtils.isCropSeed(shop.getItem().getType());
                if (shulkerContents[j].isSimilar(shop.getItem()) || isSameCropSeed) {
                    int maxTakeFromSlot = shulkerContents[j].getMaxStackSize() > 1
                            ? shulkerContents[j].getAmount() - 1
                            : 1;
                    int take = Math.min(maxTakeFromSlot, effectiveShulkerContribution - collectedFromShulker);
                    if (take > 0) {
                        collectedFromShulker += take;
                        int remaining = shulkerContents[j].getAmount() - take;
                        if (remaining <= 0) {
                            shulkerContents[j] = null;
                        } else {
                            shulkerContents[j].setAmount(remaining);
                        }
                        modified = true;
                    }
                }
            }

            if (modified) {
                shulkerInv.setContents(shulkerContents);
                bsm.setBlockState(shulker);
                contents[i].setItemMeta(bsm);
            }
        }

        HashMap<Integer, ItemStack[]> result = new HashMap<>();
        result.put(collectedFromRegular + collectedFromShulker, contents);
        return result;
    }

    /**
     * Verifies if the contents contains the full amount needed from the shop.
     * Also scans inside shulker boxes in the inventory for the required items.
     *
     * @param inventory  The contents to be verified.
     * @param playerShop The player shop being interacted with.
     * @return Confirmation if the contents contain the full amount from the shop.
     */
    private HashMap<Boolean, ItemStack[]> checkIfContentsHasShopItems(ItemStack[] inventory, Shop playerShop, boolean scanShulkers) {
        // Avoids reference errors
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack is : inventory) {
            if (is == null) {
                items.add(null);
            } else {
                items.add(is.clone());
            }
        }
        ItemStack[] contents = items.toArray(new ItemStack[0]);

        int needed = playerShop.getQuantity();
        int collected = 0;
        boolean shopItemIsShulker = isShulkerBox(playerShop.getItem());

        // Phase 1: Scan regular (non-shulker) inventory slots from end to beginning
        for (int i = contents.length - 1; i >= 0 && collected < needed; i--) {
            if (contents[i] == null || isShulkerBox(contents[i])) {
                continue;
            }
            boolean isSameCropSeed = contents[i].getType() == playerShop.getItem().getType() && CropUtils.isCropSeed(playerShop.getItem().getType());
            if (contents[i].isSimilar(playerShop.getItem()) || isSameCropSeed) {
                int take = Math.min(needed - collected, contents[i].getAmount());
                collected += take;
                int newAmount = contents[i].getAmount() - take;
                if (newAmount <= 0) {
                    contents[i] = null;
                } else {
                    contents[i].setAmount(newAmount);
                }
            }
        }

        // Phase 2: Scan inside shulker boxes if still not enough (only when shop item is not itself a shulker
        // and the caller permits shulker scanning)
        if (!shopItemIsShulker && scanShulkers) {
            for (int i = contents.length - 1; i >= 0 && collected < needed; i--) {
                if (contents[i] == null || !isShulkerBox(contents[i])) {
                    continue;
                }
                if (!(contents[i].getItemMeta() instanceof BlockStateMeta bsm)) {
                    continue;
                }
                if (!(bsm.getBlockState() instanceof ShulkerBox shulker)) {
                    continue;
                }

                Inventory shulkerInv = shulker.getInventory();
                ItemStack[] shulkerContents = shulkerInv.getContents().clone();
                boolean modified = false;

                for (int j = shulkerContents.length - 1; j >= 0 && collected < needed; j--) {
                    if (shulkerContents[j] == null) {
                        continue;
                    }
                    boolean isSameCropSeed = shulkerContents[j].getType() == playerShop.getItem().getType() && CropUtils.isCropSeed(playerShop.getItem().getType());
                    if (shulkerContents[j].isSimilar(playerShop.getItem()) || isSameCropSeed) {
                        int take = Math.min(needed - collected, shulkerContents[j].getAmount());
                        collected += take;
                        int newAmount = shulkerContents[j].getAmount() - take;
                        if (newAmount <= 0) {
                            shulkerContents[j] = null;
                        } else {
                            shulkerContents[j].setAmount(newAmount);
                        }
                        modified = true;
                    }
                }

                if (modified) {
                    shulkerInv.setContents(shulkerContents);
                    bsm.setBlockState(shulker);
                    contents[i].setItemMeta(bsm);
                }
            }
        }

        HashMap<Boolean, ItemStack[]> results = new HashMap<>();
        results.put(collected >= needed, contents);
        return results;
    }

    /**
     * Determines if the item is a shulker box (with block state meta).
     *
     * @param item The item to check.
     * @return True if the item is a shulker box.
     */
    private boolean isShulkerBox(ItemStack item) {
        if (item == null) {
            return false;
        }
        return item.getItemMeta() instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof ShulkerBox;
    }


    /**
     * Determines if the block is a valid shop container (chest, trapped chest, barrel, copper chest, or shulker box).
     *
     * @param block The block to check.
     * @return Confirmation of whether the block is a valid shop container.
     */
    private boolean isContainer(Block block) {
        return AranarthUtils.isContainerBlock(block);
    }


}
