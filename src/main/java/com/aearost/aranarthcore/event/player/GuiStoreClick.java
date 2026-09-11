package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiStore;
import com.aearost.aranarthcore.objects.StorePage;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
        } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Aranarth Store - Perks")) {
            perksPageLogic(e);
        } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Aranarth Store - Boosts")) {
            boostsPageLogic(e);
        } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Aranarth Store - Crate Keys")) {
            cratesPageLogic(e);
        } else {
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
            player.playSound(e.getWhoClicked(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
            GuiStore gui = new GuiStore(player, StorePage.SAINT);
            gui.openGui();
        } else if (slot == 12) {
            player.playSound(e.getWhoClicked(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
            GuiStore gui = new GuiStore(player, StorePage.PERKS);
            gui.openGui();
        } else if (slot == 14) {
            player.playSound(e.getWhoClicked(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
            GuiStore gui = new GuiStore(player, StorePage.BOOSTS);
            gui.openGui();
        } else if (slot == 16) {
            player.playSound(e.getWhoClicked(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
            GuiStore gui = new GuiStore(player, StorePage.CRATES);
            gui.openGui();
        }

        // Exit button
        else if (slot == 22) {
            player.playSound(e.getWhoClicked(), Sound.UI_BUTTON_CLICK, 0.9F, 1F);
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
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.acolyte_monthly")));
            player.closeInventory();
        } else if (slot == 13) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.disciple_monthly")));
            player.closeInventory();
        } else if (slot == 15) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.seraph_monthly")));
            player.closeInventory();
        } else if (slot == 20) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.acolyte_lifetime")));
            player.closeInventory();
        } else if (slot == 22) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.disciple_lifetime")));
            player.closeInventory();
        } else if (slot == 24) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.seraph_lifetime")));
            player.closeInventory();
        }

        // Return to main menu
        else if (slot == 31) {
            player.playSound(e.getWhoClicked(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
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
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.blacklist")));
            player.closeInventory();
        } else if (slot == 13) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.shulker_assist")));
            player.closeInventory();
        } else if (slot == 14) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.inventory_assist")));
            player.closeInventory();
        } else if (slot == 19) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.compressor")));
            player.closeInventory();
        } else if (slot == 20) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.randomizer")));
            player.closeInventory();
        } else if (slot == 21) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.tables")));
            player.closeInventory();
        } else if (slot == 22) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.colored_nickname")));
            player.closeInventory();
        } else if (slot == 23) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.colored_chat")));
            player.closeInventory();
        } else if (slot == 24) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.item_name")));
            player.closeInventory();
        } else if (slot == 25) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.blue_fire")));
            player.closeInventory();
        } else if (slot == 29) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.invisible_frames")));
            player.closeInventory();
        } else if (slot == 30) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.additional_homes")));
            player.closeInventory();
        } else if (slot == 32) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.discord_chat")));
            player.closeInventory();
        } else if (slot == 33) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.invisible_armor")));
            player.closeInventory();
        }

        // Return to main menu
        else if (slot == 40) {
            player.playSound(e.getWhoClicked(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
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
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.boost_miner")));
            player.closeInventory();
        } else if (slot == 12) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.boost_harvest")));
            player.closeInventory();
        } else if (slot == 14) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.boost_hunter")));
            player.closeInventory();
        } else if (slot == 16) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.boost_chi")));
            player.closeInventory();
        }

        // Return to main menu
        else if (slot == 22) {
            player.playSound(e.getWhoClicked(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
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
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.rare_key")));
            player.closeInventory();
        } else if (slot == 13) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.epic_key")));
            player.closeInventory();
        } else if (slot == 16) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("store.godly_key")));
            player.closeInventory();
        }

        // Return to main menu
        else if (slot == 22) {
            player.playSound(e.getWhoClicked(), Sound.ITEM_BOOK_PAGE_TURN, 1F, 1.3F);
            GuiStore gui = new GuiStore(player, StorePage.MAIN);
            gui.openGui();
        }
    }

}
