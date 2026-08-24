package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_LEVEL;
import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_TYPE;
import static com.aearost.aranarthcore.objects.CustomKeys.PRESERVATION_USES;

/**
 * If harvested with a pickaxe bearing the Incantation of Preservation, the Budding Amethyst block will drop.
 */
public class BuddingAmethystBreak {
	public void execute(BlockBreakEvent e) {
		ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
		if (hasPreservation(heldItem) && isHoldingPickaxe(heldItem)) {
			Location location = e.getBlock().getLocation();
			e.setDropItems(false);
			location.getWorld().dropItemNaturally(location, new ItemStack(Material.BUDDING_AMETHYST, 1));

			// Decrement uses and strip the incantation when exhausted
			Player player = e.getPlayer();
			ItemMeta meta = heldItem.getItemMeta();
			if (meta == null || !meta.getPersistentDataContainer().has(PRESERVATION_USES)) return;

			int uses = meta.getPersistentDataContainer().get(PRESERVATION_USES, PersistentDataType.INTEGER);
			uses--;

			if (uses <= 0) {
				meta.getPersistentDataContainer().remove(INCANTATION_TYPE);
				meta.getPersistentDataContainer().remove(INCANTATION_LEVEL);
				meta.getPersistentDataContainer().remove(PRESERVATION_USES);
				meta.removeEnchant(Enchantment.SILK_TOUCH);
				List<String> lore = meta.getLore();
				if (lore != null) {
					lore.removeIf(line -> ChatUtils.stripColorFormatting(line).equals("Preservation"));
					meta.setLore(lore.isEmpty() ? null : lore);
				}
				player.sendMessage(ChatUtils.chatMessage("&5Your Incantation of Preservation has expired."));
			} else {
				meta.getPersistentDataContainer().set(PRESERVATION_USES, PersistentDataType.INTEGER, uses);
			}

			heldItem.setItemMeta(meta);
		}
	}

	private boolean hasPreservation(ItemStack item) {
		if (!item.hasItemMeta()) return false;
		var pdc = item.getItemMeta().getPersistentDataContainer();
		return pdc.has(INCANTATION_TYPE) &&
				"incantation_preservation".equals(pdc.get(INCANTATION_TYPE, PersistentDataType.STRING));
	}

	private boolean isHoldingPickaxe(ItemStack heldItem) {
		Material item = heldItem.getType();
        return item == Material.WOODEN_PICKAXE || item == Material.STONE_PICKAXE || item == Material.IRON_PICKAXE
                || item == Material.GOLDEN_PICKAXE || item == Material.DIAMOND_PICKAXE
                || item == Material.NETHERITE_PICKAXE;
	}
}
