package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiDominionFlagSelect;
import com.aearost.aranarthcore.gui.GuiDominionPermissions;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.List;

/**
 * Handles click events for the per-area flag select GUI.
 */
public class GuiDominionFlagSelectClick {

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

        String title = ChatUtils.stripColorFormatting(e.getView().getTitle());
        String activeTitleKey = resolveTitleKey(player, title);
        if (activeTitleKey == null) {
            return;
        }

        List<Outpost> outposts = OutpostUtils.getDominionOutposts(dominion.getId());
        int dominionSlot = GuiDominionFlagSelect.getDominionSlot(outposts.size());
        int slot = e.getSlot();

        if (slot == dominionSlot) {
            toggleDominionFlag(dominion, activeTitleKey);
            DominionUtils.updateDominion(dominion);
            GuiDominionFlagSelect.refreshItems(e.getClickedInventory(), dominion, outposts, activeTitleKey);
            player.updateInventory();
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
            return;
        }

        for (int i = 0; i < outposts.size(); i++) {
            if (slot == dominionSlot + 2 + i) {
                Outpost outpost = outposts.get(i);
                toggleOutpostFlag(outpost, activeTitleKey);
                Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), PersistenceUtils::saveOutposts);
                if (NetworkManager.isActive()) {
                    NetworkManager.getInstance().publishOutpostUpdate(outpost);
                }
                GuiDominionFlagSelect.refreshItems(e.getClickedInventory(), dominion, outposts, activeTitleKey);
                player.updateInventory();
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
                return;
            }
        }
    }

    private String resolveTitleKey(Player player, String strippedTitle) {
        if (strippedTitle.equals(Lang.getFor(player, GuiDominionFlagSelect.TITLE_MOB_SPAWNING_KEY))) {
            return GuiDominionFlagSelect.TITLE_MOB_SPAWNING_KEY;
        }
        if (strippedTitle.equals(Lang.getFor(player, GuiDominionFlagSelect.TITLE_PVP_KEY))) {
            return GuiDominionFlagSelect.TITLE_PVP_KEY;
        }
        if (strippedTitle.equals(Lang.getFor(player, GuiDominionFlagSelect.TITLE_BENDING_KEY))) {
            return GuiDominionFlagSelect.TITLE_BENDING_KEY;
        }
        if (strippedTitle.equals(Lang.getFor(player, GuiDominionFlagSelect.TITLE_EXPLOSIONS_KEY))) {
            return GuiDominionFlagSelect.TITLE_EXPLOSIONS_KEY;
        }
        return null;
    }

    private void toggleDominionFlag(Dominion dominion, String titleKey) {
        switch (titleKey) {
            case GuiDominionFlagSelect.TITLE_MOB_SPAWNING_KEY ->
                    dominion.setMobSpawningEnabled(!dominion.isMobSpawningEnabled());
            case GuiDominionFlagSelect.TITLE_PVP_KEY -> dominion.setMemberPvpEnabled(!dominion.isMemberPvpEnabled());
            case GuiDominionFlagSelect.TITLE_BENDING_KEY -> dominion.setBendingEnabled(!dominion.isBendingEnabled());
            case GuiDominionFlagSelect.TITLE_EXPLOSIONS_KEY ->
                    dominion.setExplosionEnabled(!dominion.isExplosionEnabled());
        }
    }

    private void toggleOutpostFlag(Outpost outpost, String titleKey) {
        switch (titleKey) {
            case GuiDominionFlagSelect.TITLE_MOB_SPAWNING_KEY ->
                    outpost.setMobSpawningEnabled(!outpost.isMobSpawningEnabled());
            case GuiDominionFlagSelect.TITLE_PVP_KEY -> outpost.setMemberPvpEnabled(!outpost.isMemberPvpEnabled());
            case GuiDominionFlagSelect.TITLE_BENDING_KEY -> outpost.setBendingEnabled(!outpost.isBendingEnabled());
            case GuiDominionFlagSelect.TITLE_EXPLOSIONS_KEY ->
                    outpost.setExplosionEnabled(!outpost.isExplosionEnabled());
        }
    }
}
