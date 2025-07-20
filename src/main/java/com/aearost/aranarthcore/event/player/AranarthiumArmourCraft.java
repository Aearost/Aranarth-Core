package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiEnhancedAranarthium;
import com.aearost.aranarthcore.items.aranarthium.armour.*;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARANARTHIUM_INGOT;
import static com.aearost.aranarthcore.items.CustomItemKeys.ARMOR_TYPE;

/**
 * Enhances the piece of netherite armour to one of the Aranarthium armours.
 */
public class AranarthiumArmourCraft {
	public void execute(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player player) {
			Inventory inventory = e.getClickedInventory();
			if (e.getClickedInventory().getType() == InventoryType.ANVIL) {
				int slot = e.getSlot();

				// Placing on empty slot
				if (e.getAction() == InventoryAction.PLACE_ALL || e.getAction() == InventoryAction.PLACE_ONE || e.getAction() == InventoryAction.PLACE_ALL) {
					ItemStack armor = null;
					ItemStack ingot = null;
					boolean isArmorFirst = false;

					if (slot == 0) {
						if (hasNetheriteArmour(e.getCursor()) && hasEnhancedAranarthium(inventory.getItem(1))) {
							armor = e.getCursor().clone();
							ingot = inventory.getItem(1).clone();
							isArmorFirst = true;
						} else if (hasEnhancedAranarthium(e.getCursor()) && hasNetheriteArmour(inventory.getItem(1))) {
							ingot = e.getCursor().clone();
							armor = inventory.getItem(1).clone();
						}
					} else if (slot == 1) {
						if (hasNetheriteArmour(e.getCursor()) && hasEnhancedAranarthium(inventory.getItem(0))) {
							armor = e.getCursor().clone();
							ingot = inventory.getItem(0).clone();
						} else if (hasEnhancedAranarthium(e.getCursor()) && hasNetheriteArmour(inventory.getItem(0))) {
							ingot = e.getCursor().clone();
							armor = inventory.getItem(0).clone();
							isArmorFirst = true;
						}
					}
					// Yield the enhanced armor
					if (armor != null & ingot != null) {
						inventory.clear();
						e.getCursor().setAmount(0);
						new GuiEnhancedAranarthium(player, armor, ingot, determineArmourResult(armor, ingot), isArmorFirst).openGui();
					}
				}
				// Picking up
				else if (e.getAction() == InventoryAction.PICKUP_ALL || e.getAction() == InventoryAction.PICKUP_HALF
						|| e.getAction() == InventoryAction.PICKUP_ONE || e.getAction() == InventoryAction.PICKUP_SOME) {
					if (slot == 0 || slot == 1) {
						inventory.setItem(2, null);
					} else if (slot == 2) {
						boolean armorInFirst = hasNetheriteArmour(inventory.getItem(0)) && hasEnhancedAranarthium(inventory.getItem(1)) && inventory.getItem(2) != null;
						boolean ingotInFirst = hasEnhancedAranarthium(inventory.getItem(0)) && hasNetheriteArmour(inventory.getItem(1)) && inventory.getItem(2) != null;
						if (armorInFirst || ingotInFirst) {
							inventory.getItem(0).setAmount(inventory.getItem(0).getAmount() - 1);
							inventory.getItem(1).setAmount(inventory.getItem(1).getAmount() - 1);

							ItemMeta meta = inventory.getItem(2).getItemMeta();
							player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1F);
							String type = meta.getPersistentDataContainer().get(ARMOR_TYPE, PersistentDataType.STRING);
							if (type.equals("aquatic") || type.equals("ardent") || type.equals("elven")) {
								player.sendMessage(ChatUtils.chatMessage("&7You have forged an " + meta.getDisplayName()));
							} else {
								player.sendMessage(ChatUtils.chatMessage("&7You have forged a " + meta.getDisplayName()));
							}
						}
					}
				}
				// Switching  two items
				else if (e.getAction() == InventoryAction.SWAP_WITH_CURSOR || e.getAction() == InventoryAction.HOTBAR_SWAP) {
					// TO-DO
				}
			}
		}
	}

	/**
	 * Determines if the input slot is a normal netherite armour piece.
	 * @param item The item in the anvil to be verified.
	 * @return Confirmation of whether the input slot is a normal netherite armour piece.
	 */
	private boolean hasNetheriteArmour(ItemStack item) {
		if (item != null) {
			Material type = item.getType();
			if (type == Material.NETHERITE_HELMET || type == Material.NETHERITE_CHESTPLATE
					|| type == Material.NETHERITE_LEGGINGS || type == Material.NETHERITE_BOOTS) {
				if (item.hasItemMeta()) {
					// Prevents enhanced armour from being used
					if (!item.getItemMeta().getPersistentDataContainer().has(ARMOR_TYPE)) {
						return true;
					} else {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines if the input slot is an enhanced Aranarthium ingot.
	 * @param item The item in the anvil to be verified.
	 * @return Confirmation of whether the input slot is an enhanced Aranarthium ingot.
	 */
	private boolean hasEnhancedAranarthium(ItemStack item) {
		if (item != null) {
			Material type = item.getType();
			if (type == Material.ECHO_SHARD) {
				if (item.hasItemMeta()) {
					// Ensures an enhanced aranarthium is used and not a standard one
					if (item.getItemMeta().getPersistentDataContainer().has(ARANARTHIUM_INGOT)
						&& !item.getItemMeta().getPersistentDataContainer().get(ARANARTHIUM_INGOT, PersistentDataType.STRING).equals("aranarthium")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Determines which armour piece should be returned from the input armor and ingot.
	 * @param armor The armor piece.
	 * @param ingot The enhanced Aranarthium ingot.
	 * @return The enhanced Aranarthium armor piece.
	 */
	private ItemStack determineArmourResult(ItemStack armor, ItemStack ingot) {
		String ingotName = ingot.getItemMeta().getPersistentDataContainer().get(ARANARTHIUM_INGOT, PersistentDataType.STRING);
		Material type = armor.getType();
		ItemStack enhancedAranarthiumArmor = null;
        switch (ingotName) {
            case "aquatic" -> {
                if (type == Material.NETHERITE_HELMET) {
                    enhancedAranarthiumArmor = new AquaticAranarthiumHelmet().getItem();
                } else if (type == Material.NETHERITE_CHESTPLATE) {
					enhancedAranarthiumArmor = new AquaticAranarthiumChestplate().getItem();
                } else if (type == Material.NETHERITE_LEGGINGS) {
					enhancedAranarthiumArmor = new AquaticAranarthiumLeggings().getItem();
                } else if (type == Material.NETHERITE_BOOTS) {
					enhancedAranarthiumArmor = new AquaticAranarthiumBoots().getItem();
                }
            }
            case "ardent" -> {
				if (type == Material.NETHERITE_HELMET) {
					enhancedAranarthiumArmor = new ArdentAranarthiumHelmet().getItem();
				} else if (type == Material.NETHERITE_CHESTPLATE) {
					enhancedAranarthiumArmor = new ArdentAranarthiumChestplate().getItem();
				} else if (type == Material.NETHERITE_LEGGINGS) {
					enhancedAranarthiumArmor = new ArdentAranarthiumLeggings().getItem();
				} else if (type == Material.NETHERITE_BOOTS) {
					enhancedAranarthiumArmor = new ArdentAranarthiumBoots().getItem();
				}
            }
            case "dwarven" -> {
				if (type == Material.NETHERITE_HELMET) {
					enhancedAranarthiumArmor = new DwarvenAranarthiumHelmet().getItem();
				} else if (type == Material.NETHERITE_CHESTPLATE) {
					enhancedAranarthiumArmor = new DwarvenAranarthiumChestplate().getItem();
				} else if (type == Material.NETHERITE_LEGGINGS) {
					enhancedAranarthiumArmor = new DwarvenAranarthiumLeggings().getItem();
				} else if (type == Material.NETHERITE_BOOTS) {
					enhancedAranarthiumArmor = new DwarvenAranarthiumBoots().getItem();
				}
            }
            case "elven" -> {
				if (type == Material.NETHERITE_HELMET) {
					enhancedAranarthiumArmor = new ElvenAranarthiumHelmet().getItem();
				} else if (type == Material.NETHERITE_CHESTPLATE) {
					enhancedAranarthiumArmor = new ElvenAranarthiumChestplate().getItem();
				} else if (type == Material.NETHERITE_LEGGINGS) {
					enhancedAranarthiumArmor = new ElvenAranarthiumLeggings().getItem();
				} else if (type == Material.NETHERITE_BOOTS) {
					enhancedAranarthiumArmor = new ElvenAranarthiumBoots().getItem();
				}
            }
            case "scorched" -> {
				if (type == Material.NETHERITE_HELMET) {
					enhancedAranarthiumArmor = new ScorchedAranarthiumHelmet().getItem();
				} else if (type == Material.NETHERITE_CHESTPLATE) {
					enhancedAranarthiumArmor = new ScorchedAranarthiumChestplate().getItem();
				} else if (type == Material.NETHERITE_LEGGINGS) {
					enhancedAranarthiumArmor = new ScorchedAranarthiumLeggings().getItem();
				} else if (type == Material.NETHERITE_BOOTS) {
					enhancedAranarthiumArmor = new ScorchedAranarthiumBoots().getItem();
				}
            }
            case "soulbound" -> {
				if (type == Material.NETHERITE_HELMET) {
					enhancedAranarthiumArmor = new SoulboundAranarthiumHelmet().getItem();
				} else if (type == Material.NETHERITE_CHESTPLATE) {
					enhancedAranarthiumArmor = new SoulboundAranarthiumChestplate().getItem();
				} else if (type == Material.NETHERITE_LEGGINGS) {
					enhancedAranarthiumArmor = new SoulboundAranarthiumLeggings().getItem();
				} else if (type == Material.NETHERITE_BOOTS) {
					enhancedAranarthiumArmor = new SoulboundAranarthiumBoots().getItem();
				}
            }
        }
		Map<Enchantment, Integer> enchantments = armor.getEnchantments();
		for (Enchantment enchantment : enchantments.keySet()) {
			enhancedAranarthiumArmor.addEnchantment(enchantment, enchantments.get(enchantment));
		}

		return enhancedAranarthiumArmor;
	}
}
