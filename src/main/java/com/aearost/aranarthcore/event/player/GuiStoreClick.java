package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiStore;
import com.aearost.aranarthcore.objects.StorePage;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles the page changing logic of the server store.
 */
public class GuiStoreClick {
	public void execute(InventoryClickEvent e) {
		e.setCancelled(true);
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Aranarth Store - Saint Ranks")) {
			saintPageLogic(e);
		} else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Aranarth Store - Miscellaneous Perks")) {
			perksPageLogic(e);
		} else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Aranarth Store - Server Boosts")) {
			boostsPageLogic(e);
		} else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Aranarth Store - Crate Keys")) {
			cratesPageLogic(e);
		} else  {
			mainPageLogic(e);
		}
	}

	private void mainPageLogic(InventoryClickEvent e) {
		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		int slot = e.getSlot();
		Player player = (Player) e.getWhoClicked();

		// To a new page
		if (slot == 10) {
			player.playSound(e.getWhoClicked().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
			GuiStore gui = new GuiStore(player, StorePage.SAINT);
			gui.openGui();
		} else if (slot == 12) {
			player.playSound(e.getWhoClicked().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
			GuiStore gui = new GuiStore(player, StorePage.PERKS);
			gui.openGui();
		} else if (slot == 14) {
			player.playSound(e.getWhoClicked().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
			GuiStore gui = new GuiStore(player, StorePage.BOOSTS);
			gui.openGui();
		} else if (slot == 16) {
			player.playSound(e.getWhoClicked().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
			GuiStore gui = new GuiStore(player, StorePage.CRATES);
			gui.openGui();
		}

		// Exit button
		else if (slot == 22) {
			player.playSound(e.getWhoClicked().getLocation(), Sound.UI_BUTTON_CLICK, 0.9F, 1F);
			player.closeInventory();
		}
	}

	private void saintPageLogic(InventoryClickEvent e) {
		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		int slot = e.getSlot();
		Player player = (Player) e.getWhoClicked();

		// Clicking one of the ranks
		if (slot == 11) {
			player.sendMessage(ChatUtils.chatMessage("&3&lSaint I (1 Month): &bhttps://aranarth.craftingstore.net/package/1474413"));
			player.closeInventory();
		} else if (slot == 13) {
			player.sendMessage(ChatUtils.chatMessage("&6&lSaint II (1 Month): &ehttps://aranarth.craftingstore.net/package/1474415"));
			player.closeInventory();
		} else if (slot == 15) {
			player.sendMessage(ChatUtils.chatMessage("&4&lSaint III (1 Month): &chttps://aranarth.craftingstore.net/package/1474421"));
			player.closeInventory();
		} else if (slot == 20) {
			player.sendMessage(ChatUtils.chatMessage("&3&lSaint I (Lifetime): &bhttps://aranarth.craftingstore.net/package/493265"));
			player.closeInventory();
		} else if (slot == 22) {
			player.sendMessage(ChatUtils.chatMessage("&6&lSaint II (Lifetime): &ehttps://aranarth.craftingstore.net/package/1474409"));
			player.closeInventory();
		} else if (slot == 24) {
			player.sendMessage(ChatUtils.chatMessage("&4&lSaint III (Lifetime): &chttps://aranarth.craftingstore.net/package/1474412"));
			player.closeInventory();
		}

		// Return to main menu
		else if (slot == 31) {
			player.playSound(e.getWhoClicked().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
			GuiStore gui = new GuiStore(player, StorePage.MAIN);
			gui.openGui();
		}
	}

	private void perksPageLogic(InventoryClickEvent e) {
		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		int slot = e.getSlot();
		Player player = (Player) e.getWhoClicked();

		// Clicking one of the perks
		if (slot == 12) {
			player.sendMessage(ChatUtils.chatMessage("&8&lBlacklist: &7https://aranarth.craftingstore.net/package/1474962"));
			player.closeInventory();
		} else if (slot == 13) {
			player.sendMessage(ChatUtils.chatMessage("&5&lShulker Assist: &dhttps://aranarth.craftingstore.net/package/1474988"));
			player.closeInventory();
		} else if (slot == 14) {
			player.sendMessage(ChatUtils.chatMessage("&3&lInventory Assist: &bhttps://aranarth.craftingstore.net/package/1474994"));
			player.closeInventory();
		} else if (slot == 19) {
			player.sendMessage(ChatUtils.chatMessage("&6&lCompressor: &ehttps://aranarth.craftingstore.net/package/1474604"));
			player.closeInventory();
		} else if (slot == 20) {
			player.sendMessage(ChatUtils.chatMessage("&2&lRandomizer: &ahttps://aranarth.craftingstore.net/package/1474958"));
			player.closeInventory();
		} else if (slot == 21) {
			player.sendMessage(ChatUtils.chatMessage("&6&lTables: &ehttps://aranarth.craftingstore.net/package/1474970"));
			player.closeInventory();
		} else if (slot == 23) {
			player.sendMessage(ChatUtils.chatMessage("&6&lColored Chat: &ehttps://aranarth.craftingstore.net/package/1474977"));
			player.closeInventory();
		} else if (slot == 24) {
			player.sendMessage(ChatUtils.chatMessage("&4&lItem Name: &chttps://aranarth.craftingstore.net/package/1474973"));
			player.closeInventory();
		} else if (slot == 25) {
			player.sendMessage(ChatUtils.chatMessage("&3&lBlue Fire: &bhttps://aranarth.craftingstore.net/package/1475696"));
			player.closeInventory();
		} else if (slot == 30) {
			player.sendMessage(ChatUtils.chatMessage("&7&lInvisible Item Frames: &fhttps://aranarth.craftingstore.net/package/1475081"));
			player.closeInventory();
		} else if (slot == 31) {
			player.sendMessage(ChatUtils.chatMessage("&4&lAdditional 3 Homes: &chttps://aranarth.craftingstore.net/package/1475006"));
			player.closeInventory();
		} else if (slot == 32) {
			player.sendMessage(ChatUtils.chatMessage("&5&lDiscord Chat: &dhttps://aranarth.craftingstore.net/package/1477059"));
			player.closeInventory();
		}

		// Return to main menu
		else if (slot == 40) {
			player.playSound(e.getWhoClicked().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
			GuiStore gui = new GuiStore(player, StorePage.MAIN);
			gui.openGui();
		}
	}

	private void boostsPageLogic(InventoryClickEvent e) {
		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		int slot = e.getSlot();
		Player player = (Player) e.getWhoClicked();

		// Clicking one of the boosts
		if (slot == 10) {
			player.sendMessage(ChatUtils.chatMessage("&8&lBoost of the Miner: &7https://aranarth.craftingstore.net/package/1474946"));
			player.closeInventory();
		} else if (slot == 12) {
			player.sendMessage(ChatUtils.chatMessage("&6&lBoost of the Harvest: &ehttps://aranarth.craftingstore.net/package/1474947"));
			player.closeInventory();
		} else if (slot == 14) {
			player.sendMessage(ChatUtils.chatMessage("&4&lBoost of the Hunter: &chttps://aranarth.craftingstore.net/package/1474950"));
			player.closeInventory();
		} else if (slot == 16) {
			player.sendMessage(ChatUtils.chatMessage("&7&lBoost of Chi: &fhttps://aranarth.craftingstore.net/package/1474953"));
			player.closeInventory();
		}

		// Return to main menu
		else if (slot == 22) {
			player.playSound(e.getWhoClicked().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
			GuiStore gui = new GuiStore(player, StorePage.MAIN);
			gui.openGui();
		}
	}

	private void cratesPageLogic(InventoryClickEvent e) {
		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		int slot = e.getSlot();
		Player player = (Player) e.getWhoClicked();

		// Clicking one of the crate keys
		if (slot == 10) {
			player.sendMessage(ChatUtils.chatMessage("&6&lRare Crate Key (x3): &ehttps://aranarth.craftingstore.net/package/492746"));
			player.closeInventory();
		} else if (slot == 13) {
			player.sendMessage(ChatUtils.chatMessage("&5&lEpic Crate Key (x3): &dhttps://aranarth.craftingstore.net/package/1475091"));
			player.closeInventory();
		} else if (slot == 16) {
			player.sendMessage(ChatUtils.chatMessage("&3&lGodly Crate Key (x3): &bhttps://aranarth.craftingstore.net/package/1475090"));
			player.closeInventory();
		}

		// Return to main menu
		else if (slot == 22) {
			player.playSound(e.getWhoClicked().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
			GuiStore gui = new GuiStore(player, StorePage.MAIN);
			gui.openGui();
		}
	}

}
