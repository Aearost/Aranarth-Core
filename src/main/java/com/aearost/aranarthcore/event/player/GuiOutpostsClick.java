package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDominionPermissions;
import com.aearost.aranarthcore.gui.GuiOutposts;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.OutpostUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Handles click events for the Dominion Outposts GUI.
 */
public class GuiOutpostsClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CHEST) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        // Back button
        if (clicked.getType() == Material.BARRIER) {
            new GuiDominionPermissions(player).openGui();
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }

        // Check if clicked an outpost slot
        int slot = e.getSlot();
        int[] outpostSlots = GuiOutposts.getOutpostSlots();
        int outpostIndex = -1;
        for (int i = 0; i < outpostSlots.length; i++) {
            if (outpostSlots[i] == slot) {
                outpostIndex = i + 1;
                break;
            }
        }

        if (outpostIndex == -1) {
            return;
        }

        // Locked slot
        if (clicked.getType() == Material.RED_CONCRETE) {
            player.sendMessage(ChatUtils.chatMessage("&cThis outpost is not yet unlocked"));
            return;
        }

        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }

        // Empty slot (outpost available but not yet created)
        String displayName = ChatUtils.stripColorFormatting(clicked.getItemMeta().getDisplayName());
        if (displayName.startsWith("Outpost Slot")) {
            player.sendMessage(ChatUtils.chatMessage("&7Use &e/d outpost create <name> &7to establish an outpost"));
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }

        // Active outpost, teleport to home
        final int finalOutpostIndex = outpostIndex;
        List<Outpost> outposts = OutpostUtils.getDominionOutposts(dominion.getId());
        Outpost outpost = outposts.stream()
                .filter(o -> o.getOutpostIndex() == finalOutpostIndex)
                .findFirst().orElse(null);

        if (outpost == null) {
            player.sendMessage(ChatUtils.chatMessage("&cThat outpost could not be found!"));
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        AranarthUtils.teleportPlayer(player, player.getLocation(), outpost.getHome(), aranarthPlayer.isInAdminMode(), outpost.getName(), "&7You have teleported to your outpost", success -> {
            if (success) {
                player.sendMessage(ChatUtils.chatMessage("&7Teleported to the outpost &e" + outpost.getName()));
            } else {
                player.sendMessage(ChatUtils.chatMessage("&cCould not teleport to the outpost &e" + outpost.getName()));
            }
        });
        player.closeInventory();
    }
}
