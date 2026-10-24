package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BundleContents;
import io.papermc.paper.datacomponent.item.ChargedProjectiles;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Container;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Prevents creative players from creating items that hold too many nested items.
 */
public class CreativeItemListener implements Listener {

	private static final int MAX_NESTED_ITEMS = 2048;
	private static final int MAX_NESTING_DEPTH = 16;

	public CreativeItemListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Empties items taken through the creative inventory that hold too many nested items.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCreativeItem(InventoryCreativeEvent e) {
		ItemStack item = e.getCursor();
		if (isTooLarge(item)) {
			e.setCursor(new ItemStack(item.getType(), item.getAmount()));
			notify(e.getWhoClicked() instanceof Player player ? player : null, item);
		}
	}

	/**
	 * Prevents creative players from placing items that were created before this protection existed.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent e) {
		Player player = e.getPlayer();
		ItemStack item = e.getItemInHand();
		if (player.getGameMode() == GameMode.CREATIVE && isTooLarge(item)) {
			e.setCancelled(true);
			player.getInventory().setItem(e.getHand(), new ItemStack(item.getType(), item.getAmount()));
			notify(player, item);
		}
	}

	/**
	 * Prevents creative players from dropping items that were created before this protection existed.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onDrop(PlayerDropItemEvent e) {
		Player player = e.getPlayer();
		Item drop = e.getItemDrop();
		ItemStack item = drop.getItemStack();
		if (player.getGameMode() == GameMode.CREATIVE && isTooLarge(item)) {
			drop.setItemStack(new ItemStack(item.getType(), item.getAmount()));
			notify(player, item);
		}
	}

	/**
	 * Removes dropped items that hold other items when they are destroyed in the Creative worlds instead of spilling their contents.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onItemDestroyed(EntityDamageEvent e) {
		if (e.getEntity() instanceof Item item && item.getWorld().getName().startsWith("creative")
				&& !getContents(item.getItemStack()).isEmpty()) {
			e.setCancelled(true);
			item.remove();
		}
	}

	/**
	 * Informs the player and the console that an item was emptied.
	 */
	private void notify(Player player, ItemStack item) {
		if (player == null) {
			return;
		}
		player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "creative.item_too_large")));
		Bukkit.getLogger().warning("[AC] [Creative] Emptied an oversized " + item.getType().name() + " held by " + player.getName()
				+ " in " + player.getWorld().getName() + " at " + player.getLocation().getBlockX() + ", "
				+ player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ());
	}

	/**
	 * Provides whether the item holds more nested items than allowed.
	 */
	private boolean isTooLarge(ItemStack item) {
		return countNestedItems(item, 0) > MAX_NESTED_ITEMS;
	}

	/**
	 * Counts every item held by this item across all nesting levels, stopping early once the limit is exceeded.
	 */
	private int countNestedItems(ItemStack item, int depth) {
		List<ItemStack> contents = getContents(item);
		if (contents.isEmpty()) {
			return 0;
		}
		if (depth >= MAX_NESTING_DEPTH) {
			return MAX_NESTED_ITEMS + 1;
		}

		int count = contents.size();
		for (ItemStack inner : contents) {
			if (count > MAX_NESTED_ITEMS) {
				break;
			}
			count += countNestedItems(inner, depth + 1);
		}
		return count;
	}

	/**
	 * Provides every item directly held by this item, whether in a container, bundle, loaded crossbow, or block data.
	 */
	private List<ItemStack> getContents(ItemStack item) {
		List<ItemStack> contents = new ArrayList<>();
		if (item == null || item.getType().isAir()) {
			return contents;
		}

		ItemContainerContents container = item.getData(DataComponentTypes.CONTAINER);
		if (container != null) {
			addItems(contents, container.contents());
		}
		BundleContents bundle = item.getData(DataComponentTypes.BUNDLE_CONTENTS);
		if (bundle != null) {
			addItems(contents, bundle.contents());
		}
		ChargedProjectiles projectiles = item.getData(DataComponentTypes.CHARGED_PROJECTILES);
		if (projectiles != null) {
			addItems(contents, projectiles.projectiles());
		}
		// Covers items forged with raw block entity data instead of the container component
		if (container == null && item.getItemMeta() instanceof BlockStateMeta meta && meta.hasBlockState()
				&& meta.getBlockState() instanceof Container state) {
			addItems(contents, Arrays.asList(state.getInventory().getContents()));
		}
		return contents;
	}

	/**
	 * Adds every non-empty item to the list.
	 */
	private void addItems(List<ItemStack> contents, List<ItemStack> items) {
		for (ItemStack item : items) {
			if (item != null && !item.getType().isAir()) {
				contents.add(item);
			}
		}
	}
}
