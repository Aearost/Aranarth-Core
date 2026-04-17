package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shows a list of all Dominion members with their current rank.
 * The leader can click a member to cycle their rank (NEWCOMER → CITIZEN → CLERGY).
 * Title: "Dominion Members"
 */
public class GuiDominionMembers {

    private final Player player;
    private final Inventory initializedGui;

    public GuiDominionMembers(Player player) {
        this.player = player;
        this.initializedGui = initializeGui(player);
    }

    public void openGui() {
        player.closeInventory();
        player.openInventory(initializedGui);
    }

    private Inventory initializeGui(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return Bukkit.createInventory(player, 9, ChatUtils.translateToColor("Dominion Members"));
        }

        List<UUID> members = dominion.getMembers();
        // +1 ensures there is always at least one free slot for the back button
        int size = calculateSize(members.size() + 1);

        Inventory gui = Bukkit.createInventory(player, size, ChatUtils.translateToColor("Dominion Members"));

        for (int i = 0; i < members.size(); i++) {
            UUID memberUuid = members.get(i);
            DominionRank rank = dominion.getMemberRank(memberUuid);
            if (rank == null) {
                rank = DominionRank.NEWCOMER;
            }

            ItemStack skull = buildMemberSkull(memberUuid, rank, dominion.getLeader().equals(memberUuid));
            gui.setItem(i, skull);
        }

        // Back button at the slot immediately after the last member
        gui.setItem(gui.getSize() - 1, GuiDominionPermissions.buildBackButton());

        return gui;
    }

    /**
     * Helper method to build the metadata of each player in the Dominion.
     * @param uuid The UUID of the player.
     * @param rank The rank of the player.
     * @param isLeader Whether the player is the leader of the Dominion.
     * @return The customized item with the input type,
     */
    private ItemStack buildMemberSkull(UUID uuid, DominionRank rank, boolean isLeader) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        meta.setOwningPlayer(offlinePlayer);

        String nickname = AranarthUtils.getNickname(offlinePlayer);
        meta.setDisplayName(ChatUtils.translateToColor("&e" + nickname));

        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Rank: " + DominionUtils.getFormattedRankName(rank)));
        if (!isLeader) {
            lore.add(ChatUtils.translateToColor("&7&oClick to promote/demote"));
        } else {
            lore.add(ChatUtils.translateToColor("&7You are the leader"));
        }
        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private int calculateSize(int members) {
        if (members <= 9) return 9;
        if (members <= 18) return 18;
        if (members <= 27) return 27;
        if (members <= 36) return 36;
        if (members <= 45) return 45;
        return 54;
    }
}
