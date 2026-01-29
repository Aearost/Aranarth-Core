package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GuiQuiver {

	private final Player player;
	private final Inventory initializedGui;
	private final ItemStack blank;

	public GuiQuiver(Player player) {
		this.player = player;
		blank = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		// Blank
		ItemMeta blankMeta = blank.getItemMeta();
		if (Objects.nonNull(blankMeta)) {
			blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
			blank.setItemMeta(blankMeta);
		}

		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		if (initializedGui != null) {
			player.openInventory(initializedGui);
		}
	}
	
	private Inventory initializeGui(Player player) {
		Inventory gui = null;
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		List<ItemStack> arrows = new ArrayList<>();
		if (aranarthPlayer.getArrows() != null) {
			for (ItemStack is : aranarthPlayer.getArrows()) {
				if (is != null && is.getType() != Material.BLACK_STAINED_GLASS_PANE) {
					ItemStack clone = is.clone();
					arrows.add(clone);
				}
			}
		}

		List<ItemStack> initializedArrows = new ArrayList<>();
		if (player.isSneaking()) {
			int guiSize = 0;

			if (arrows.isEmpty()) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have any arrows in your Quiver!"));
				return null;
			}

			// Identifies all unique arrows presently in the Quiver
			for (ItemStack arrow : arrows) {
				arrow.setAmount(1);
				if (!initializedArrows.contains(arrow)) {
					initializedArrows.add(arrow);
					guiSize++;
				}
			}

			// Size is based on which method is used
			// If the amount is a multiple of 9, use a full row
			if (guiSize % 9 != 0) {
				guiSize = ((int) (double) (guiSize / 9) + 1) * 9;
			}

			String guiName = "Arrow Selection";
			gui = Bukkit.getServer().createInventory(player, guiSize, guiName);

			for (ItemStack arrow : initializedArrows) {
				gui.addItem(arrow);
			}
		} else {
			gui = getQuiverInventoryFromRank(player);

			if (!arrows.isEmpty()) {
				// Skips over stained glass panes
				for (ItemStack arrow : arrows) {
					for (int i = 0; i < gui.getContents().length; i++) {
						if (gui.getContents()[i] != null) {
							if (gui.getContents()[i].getType() == Material.BLACK_STAINED_GLASS_PANE) {
								continue;
							}
						}

						if (gui.getContents()[i] == null) {
							if (arrow.getType() == Material.BLACK_STAINED_GLASS_PANE) {
								continue;
							}
							gui.setItem(i, arrow);
							break;
						}
					}
				}
			}
		}
		
		return gui;
	}

	/**
	 * Determines the Quiver inventory that the player will get based on their ranks.
	 * @param player The player.
	 * @return The inventory of the Quiver.
	 */
	private Inventory getQuiverInventoryFromRank(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		int rank = aranarthPlayer.getRank();
		int saintRank = aranarthPlayer.getSaintRank();
        return switch (rank) {
            case 1 -> getEsquireQuiver(saintRank);
            case 2 -> getKnightQuiver(saintRank);
            case 3 -> getBaronQuiver(saintRank);
            case 4 -> getCountQuiver(saintRank);
            case 5 -> getDukeQuiver(saintRank);
            case 6 -> getPrinceQuiver(saintRank);
            case 7 -> getKingQuiver(saintRank);
            case 8 -> getEmperorQuiver(saintRank);
            default -> getPeasantQuiver(saintRank);
        };
	}

	/**
	 * Provides the base quiver inventory for Peasants.
	 * @param saintRank The player's Saint rank.
	 * @return The structured inventory without the input arrows.
	 */
	private Inventory getPeasantQuiver(int saintRank) {
		Inventory gui  = null;
		if (saintRank == 0) {
			gui = Bukkit.getServer().createInventory(player, 9, "Quiver");
			gui.setItem(0, blank);
			gui.setItem(1, blank);
			gui.setItem(2, blank);
			gui.setItem(6, blank);
			gui.setItem(7, blank);
			gui.setItem(8, blank);
		} else {
			gui = Bukkit.getServer().createInventory(player, 18, "Quiver");
			gui.setItem(0, blank);
			gui.setItem(1, blank);
			gui.setItem(2, blank);
			gui.setItem(6, blank);
			gui.setItem(7, blank);
			gui.setItem(8, blank);

			if (saintRank == 1) {
				gui.setItem(9, blank);
				gui.setItem(10, blank);
				gui.setItem(11, blank);
				gui.setItem(15, blank);
				gui.setItem(16, blank);
				gui.setItem(17, blank);
			} else if (saintRank == 2) {
				gui.setItem(9, blank);
				gui.setItem(13, blank);
				gui.setItem(17, blank);
			}
		}
		return gui;
	}

	/**
	 * Provides the base quiver inventory for Esquires.
	 * @param saintRank The player's Saint rank.
	 * @return The structured inventory without the input arrows.
	 */
	private Inventory getEsquireQuiver(int saintRank) {
		Inventory gui  = null;
		if (saintRank == 0) {
			gui = Bukkit.getServer().createInventory(player, 9, "Quiver");
			gui.setItem(0, blank);
			gui.setItem(1, blank);
			gui.setItem(7, blank);
			gui.setItem(8, blank);
		} else {
			gui = Bukkit.getServer().createInventory(player, 18, "Quiver");
			gui.setItem(0, blank);
			gui.setItem(1, blank);
			gui.setItem(7, blank);
			gui.setItem(8, blank);

			if (saintRank == 1) {
				gui.setItem(9, blank);
				gui.setItem(10, blank);
				gui.setItem(11, blank);
				gui.setItem(15, blank);
				gui.setItem(16, blank);
				gui.setItem(17, blank);
			} else if (saintRank == 2) {
				gui.setItem(9, blank);
				gui.setItem(13, blank);
				gui.setItem(17, blank);
			}
		}
		return gui;
	}

	/**
	 * Provides the base quiver inventory for Knights.
	 * @param saintRank The player's Saint rank.
	 * @return The structured inventory without the input arrows.
	 */
	private Inventory getKnightQuiver(int saintRank) {
		Inventory gui  = null;
		if (saintRank == 0) {
			gui = Bukkit.getServer().createInventory(player, 9, "Quiver");
		} else {
			gui = Bukkit.getServer().createInventory(player, 18, "Quiver");
			if (saintRank == 1) {
				gui.setItem(9, blank);
				gui.setItem(10, blank);
				gui.setItem(11, blank);
				gui.setItem(15, blank);
				gui.setItem(16, blank);
				gui.setItem(17, blank);
			} else if (saintRank == 2) {
				gui.setItem(9, blank);
				gui.setItem(13, blank);
				gui.setItem(17, blank);
			}
		}
		return gui;
	}

	/**
	 * Provides the base quiver inventory for Barons.
	 * @param saintRank The player's Saint rank.
	 * @return The structured inventory without the input arrows.
	 */
	private Inventory getBaronQuiver(int saintRank) {
		Inventory gui  = null;
		if (saintRank == 0) {
			gui = Bukkit.getServer().createInventory(player, 18, "Quiver");
			gui.setItem(0, blank);
			gui.setItem(1, blank);
			gui.setItem(7, blank);
			gui.setItem(8, blank);
			gui.setItem(9, blank);
			gui.setItem(17, blank);
		} else {
			gui = Bukkit.getServer().createInventory(player, 27, "Quiver");
			gui.setItem(0, blank);
			gui.setItem(1, blank);
			gui.setItem(7, blank);
			gui.setItem(8, blank);
			gui.setItem(9, blank);
			gui.setItem(17, blank);

			if (saintRank == 1) {
				gui.setItem(18, blank);
				gui.setItem(19, blank);
				gui.setItem(20, blank);
				gui.setItem(24, blank);
				gui.setItem(25, blank);
				gui.setItem(26, blank);
			} else if (saintRank == 2) {
				gui.setItem(18, blank);
				gui.setItem(22, blank);
				gui.setItem(26, blank);
			}
		}
		return gui;
	}

	/**
	 * Provides the base quiver inventory for Counts.
	 * @param saintRank The player's Saint rank.
	 * @return The structured inventory without the input arrows.
	 */
	private Inventory getCountQuiver(int saintRank) {
		Inventory gui  = null;
		if (saintRank == 0) {
			gui = Bukkit.getServer().createInventory(player, 18, "Quiver");
		} else {
			gui = Bukkit.getServer().createInventory(player, 27, "Quiver");
			if (saintRank == 1) {
				gui.setItem(18, blank);
				gui.setItem(19, blank);
				gui.setItem(20, blank);
				gui.setItem(24, blank);
				gui.setItem(25, blank);
				gui.setItem(26, blank);
			} else if (saintRank == 2) {
				gui.setItem(18, blank);
				gui.setItem(22, blank);
				gui.setItem(26, blank);
			}
		}
		return gui;
	}

	/**
	 * Provides the base quiver inventory for Dukes.
	 * @param saintRank The player's Saint rank.
	 * @return The structured inventory without the input arrows.
	 */
	private Inventory getDukeQuiver(int saintRank) {
		Inventory gui  = null;
		if (saintRank == 0) {
			gui = Bukkit.getServer().createInventory(player, 27, "Quiver");
			gui.setItem(0, blank);
			gui.setItem(8, blank);
		} else {
			gui = Bukkit.getServer().createInventory(player, 36, "Quiver");
			gui.setItem(0, blank);
			gui.setItem(8, blank);

			if (saintRank == 1) {
				gui.setItem(27, blank);
				gui.setItem(28, blank);
				gui.setItem(29, blank);
				gui.setItem(33, blank);
				gui.setItem(34, blank);
				gui.setItem(35, blank);
			} else if (saintRank == 2) {
				gui.setItem(27, blank);
				gui.setItem(31, blank);
				gui.setItem(35, blank);
			}
		}
		return gui;
	}

	/**
	 * Provides the base quiver inventory for Princes.
	 * @param saintRank The player's Saint rank.
	 * @return The structured inventory without the input arrows.
	 */
	private Inventory getPrinceQuiver(int saintRank) {
		Inventory gui  = null;
		if (saintRank == 0) {
			gui = Bukkit.getServer().createInventory(player, 36, "Quiver");
			gui.setItem(0, blank);
			gui.setItem(1, blank);
			gui.setItem(2, blank);
			gui.setItem(6, blank);
			gui.setItem(7, blank);
			gui.setItem(8, blank);
		} else {
			gui = Bukkit.getServer().createInventory(player, 45, "Quiver");
			gui.setItem(0, blank);
			gui.setItem(1, blank);
			gui.setItem(2, blank);
			gui.setItem(6, blank);
			gui.setItem(7, blank);
			gui.setItem(8, blank);

			if (saintRank == 1) {
				gui.setItem(36, blank);
				gui.setItem(37, blank);
				gui.setItem(38, blank);
				gui.setItem(42, blank);
				gui.setItem(43, blank);
				gui.setItem(44, blank);
			} else if (saintRank == 2) {
				gui.setItem(36, blank);
				gui.setItem(40, blank);
				gui.setItem(44, blank);
			}
		}
		return gui;
	}

	/**
	 * Provides the base quiver inventory for Kings.
	 * @param saintRank The player's Saint rank.
	 * @return The structured inventory without the input arrows.
	 */
	private Inventory getKingQuiver(int saintRank) {
		Inventory gui  = null;
		if (saintRank == 0) {
			gui = Bukkit.getServer().createInventory(player, 36, "Quiver");
		} else {
			gui = Bukkit.getServer().createInventory(player, 45, "Quiver");
			if (saintRank == 1) {
				gui.setItem(36, blank);
				gui.setItem(37, blank);
				gui.setItem(38, blank);
				gui.setItem(42, blank);
				gui.setItem(43, blank);
				gui.setItem(44, blank);
			} else if (saintRank == 2) {
				gui.setItem(36, blank);
				gui.setItem(40, blank);
				gui.setItem(44, blank);
			}
		}
		return gui;
	}

	/**
	 * Provides the base quiver inventory for Emperors.
	 * @param saintRank The player's Saint rank.
	 * @return The structured inventory without the input arrows.
	 */
	private Inventory getEmperorQuiver(int saintRank) {
		Inventory gui  = null;
		if (saintRank == 0) {
			gui = Bukkit.getServer().createInventory(player, 45, "Quiver");
		} else {
			gui = Bukkit.getServer().createInventory(player, 54, "Quiver");
			if (saintRank == 1) {
				gui.setItem(45, blank);
				gui.setItem(46, blank);
				gui.setItem(47, blank);
				gui.setItem(51, blank);
				gui.setItem(52, blank);
				gui.setItem(53, blank);
			} else if (saintRank == 2) {
				gui.setItem(45, blank);
				gui.setItem(49, blank);
				gui.setItem(53, blank);
			}
		}
		return gui;
	}

}
