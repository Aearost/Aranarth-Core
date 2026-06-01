package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Shows a list of all Dominion members with their current rank.
 * The leader can click a member to cycle their rank (NEWCOMER → CITIZEN → LIEUTENANT).
 * Title: "Dominion Members"
 */
public class GuiDominionMembers {

    private final Player player;
    private final Inventory initializedGui;

    private GuiDominionMembers(Player player, Dominion dominion, Map<UUID, PlayerProfile> profiles) {
        this.player = player;
        this.initializedGui = initializeGui(player, dominion, profiles);
    }

    public void openGui() {
        player.closeInventory();
        player.openInventory(initializedGui);
    }

    /**
     * Opens the GUI containing all dominion members' heads.
     */
    public static void open(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            Inventory emptyGui = Bukkit.createInventory(player, 9, ChatUtils.translateToColor("Dominion Members"));
            player.closeInventory();
            player.openInventory(emptyGui);
            return;
        }

        List<UUID> members = new ArrayList<>(dominion.getMembers());

        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            Map<UUID, PlayerProfile> profiles = new LinkedHashMap<>();
            for (UUID uuid : members) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                PlayerProfile profile = Bukkit.createProfile(uuid, op.getName());
                profile.complete(true);
                profiles.put(uuid, profile);
            }

            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () ->
                    new GuiDominionMembers(player, dominion, profiles).openGui());
        });
    }

    private Inventory initializeGui(Player player, Dominion dominion, Map<UUID, PlayerProfile> profiles) {
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

            PlayerProfile profile = profiles.get(memberUuid);
            ItemStack skull = buildMemberSkull(memberUuid, rank, dominion.getLeader().equals(memberUuid), profile);
            gui.setItem(i, skull);
        }

        // Back button at the slot immediately after the last member
        gui.setItem(gui.getSize() - 1, GuiDominionPermissions.buildBackButton());

        return gui;
    }

    /**
     * Helper method to build the metadata of each player in the Dominion.
     *
     * @param uuid     The UUID of the player.
     * @param rank     The rank of the player.
     * @param isLeader Whether the player is the leader of the Dominion.
     * @param profile  The pre-loaded player profile for the skull skin.
     * @return The customized item with the input type.
     */
    private ItemStack buildMemberSkull(UUID uuid, DominionRank rank, boolean isLeader, PlayerProfile profile) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (profile != null) {
            meta.setPlayerProfile(profile);
        }

        String nickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(uuid));
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
        if (members <= 9) {
            return 9;
        }
        if (members <= 18) {
            return 18;
        }
        if (members <= 27) {
            return 27;
        }
        if (members <= 36) {
            return 36;
        }
        if (members <= 45) {
            return 45;
        }
        return 54;
    }
}
