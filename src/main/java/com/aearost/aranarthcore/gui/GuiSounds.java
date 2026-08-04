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

import java.util.List;

public class GuiSounds {

    public static final String TITLE = "Player Sounds";

    private final Player player;
    private final Inventory initializedGui;

    public GuiSounds(Player player) {
        this.player = player;
        this.initializedGui = initializeGui(player);
    }

    public static ItemStack buildVolumeItem(Material material, String name, int volume) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String volumeColor = volume == 100 ? "&a&l" : (volume == 0 ? "&c&l" : "&e&l");
        meta.setDisplayName(ChatUtils.translateToColor(name + " &7&l- " + volumeColor + volume + "%"));
        meta.setLore(List.of(
                ChatUtils.translateToColor("&7Left Click: &c-10%"),
                ChatUtils.translateToColor("&7Right Click: &a+10%"),
                ChatUtils.translateToColor("&7Shift+Left: &cMute (0%)"),
                ChatUtils.translateToColor("&7Shift+Right: &aMax (100%)")
        ));
        item.setItemMeta(meta);
        return item;
    }

    public void openGui() {
        player.closeInventory();
        if (initializedGui != null) {
            player.openInventory(initializedGui);
        }
    }

    private Inventory initializeGui(Player player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        Inventory gui = Bukkit.getServer().createInventory(player, 36, TITLE);

        ItemStack blank = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta blankMeta = blank.getItemMeta();
        blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
        blank.setItemMeta(blankMeta);

        for (int i = 0; i <= 8; i++) {
            gui.setItem(i, blank);
        }
        for (int i = 27; i <= 35; i++) {
            gui.setItem(i, blank);
        }

        ItemStack exit = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName(ChatUtils.translateToColor("&4&lExit"));
        exit.setItemMeta(exitMeta);
        gui.setItem(31, exit);

        gui.setItem(9, buildVolumeItem(Material.BELL, "&f&lJoin Sound", aranarthPlayer.getJoinSoundVolume()));
        gui.setItem(10, buildVolumeItem(Material.NOTE_BLOCK, "&f&lLeave Sound", aranarthPlayer.getLeaveSoundVolume()));
        gui.setItem(11, buildVolumeItem(Material.EXPERIENCE_BOTTLE, "&f&lVote Sound", aranarthPlayer.getVoteSoundVolume()));
        gui.setItem(12, buildVolumeItem(Material.CHEST, "&f&lCrate Sound", aranarthPlayer.getCrateSoundVolume()));
        gui.setItem(13, buildVolumeItem(Material.WATER_BUCKET, "&f&lWeather Sound", aranarthPlayer.getWeatherSoundVolume()));
        gui.setItem(14, buildVolumeItem(Material.CLOCK, "&f&lNew Day Sound", aranarthPlayer.getNewDaySoundVolume()));
        gui.setItem(15, buildVolumeItem(Material.FIREWORK_ROCKET, "&f&lNew Month Sound", aranarthPlayer.getNewMonthSoundVolume()));
        gui.setItem(16, buildVolumeItem(Material.PAPER, "&f&lMessage Sound", aranarthPlayer.getPrivateMsgSoundVolume()));
        gui.setItem(17, buildVolumeItem(Material.ENDER_PEARL, "&f&lTeleport Sound", aranarthPlayer.getTeleportSoundVolume()));
        gui.setItem(18, buildVolumeItem(Material.NETHER_STAR, "&f&lAvatar Sound", aranarthPlayer.getAvatarSoundVolume()));
        gui.setItem(19, buildVolumeItem(Material.IRON_SWORD, "&f&lDominion Sound", aranarthPlayer.getDominionSoundVolume()));
        gui.setItem(20, buildVolumeItem(Material.AMETHYST_SHARD, "&f&lAranarthium Sound", aranarthPlayer.getAranarthiumSoundVolume()));
        gui.setItem(21, buildVolumeItem(Material.BOOK, "&f&lChat Game Sound", aranarthPlayer.getChatGameSoundVolume()));
        gui.setItem(22, buildVolumeItem(Material.CHEST, "&f&lChest Sort Sound", aranarthPlayer.getChestSortSoundVolume()));
        gui.setItem(23, buildVolumeItem(Material.IRON_PICKAXE, "&f&lJobs Sound", aranarthPlayer.getJobsSoundVolume()));
        gui.setItem(24, buildVolumeItem(Material.EXPERIENCE_BOTTLE, "&f&lExp Store Sound", aranarthPlayer.getExpStoreSoundVolume()));

        return gui;
    }

}
