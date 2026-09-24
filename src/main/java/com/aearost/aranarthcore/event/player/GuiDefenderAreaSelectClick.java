package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDefenderAreaSelect;
import com.aearost.aranarthcore.gui.GuiDefenders;
import com.aearost.aranarthcore.gui.GuiDominionPermissions;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.OutpostUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.List;

/**
 * Handles click events for the Defender Area Select GUI.
 */
public class GuiDefenderAreaSelectClick {

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

        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.leader_only_permissions")));
            return;
        }

        if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) {
            return;
        }

        // Back button
        if (e.getCurrentItem().getType() == Material.BARRIER) {
            new GuiDominionPermissions(player).openGui();
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }

        List<Outpost> outposts = OutpostUtils.getDominionOutposts(dominion.getId());
        int domSlot = GuiDefenderAreaSelect.getDominionSlot(outposts.size());
        int slot = e.getSlot();

        if (slot == domSlot) {
            GuiDefenders.open(player, null);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }

        for (int i = 0; i < outposts.size(); i++) {
            if (slot == domSlot + 2 + i) {
                GuiDefenders.open(player, outposts.get(i));
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                return;
            }
        }
    }
}
