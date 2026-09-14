package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GuiSounds {

    public static final String TITLE_KEY = "gui.sounds.title";

    private final Player player;
    private final Inventory initializedGui;

    public GuiSounds(Player player) {
        this.player = player;
        this.initializedGui = initializeGui(player);
    }

    public static ItemStack buildVolumeItem(ItemStack item, String name, int volume) {
        ItemMeta meta = item.getItemMeta();

        String volumeColor = volume == 100 ? "&a&l" : (volume == 0 ? "&c&l" : "&e&l");
        meta.setDisplayName(ChatUtils.translateToColor(name + " &7&l- " + volumeColor + volume + "%"));
        meta.setLore(List.of(
                Lang.get("gui.sounds.volume_lore1"),
                Lang.get("gui.sounds.volume_lore2"),
                Lang.get("gui.sounds.volume_lore3"),
                Lang.get("gui.sounds.volume_lore4")
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildVolumeItem(Material material, String name, int volume) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String volumeColor = volume == 100 ? "&a&l" : (volume == 0 ? "&c&l" : "&e&l");
        meta.setDisplayName(ChatUtils.translateToColor(name + " &7&l- " + volumeColor + volume + "%"));
        meta.setLore(List.of(
                Lang.get("gui.sounds.volume_lore1"),
                Lang.get("gui.sounds.volume_lore2"),
                Lang.get("gui.sounds.volume_lore3"),
                Lang.get("gui.sounds.volume_lore4")
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
        Inventory gui = Bukkit.getServer().createInventory(player, 36, Lang.getFor(player, TITLE_KEY));

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
        exitMeta.setDisplayName(Lang.getFor(player, "gui.exit"));
        exit.setItemMeta(exitMeta);
        gui.setItem(31, exit);

        gui.setItem(9, buildVolumeItem(Material.BELL, Lang.get("gui.sounds.join"), aranarthPlayer.getJoinSoundVolume()));
        gui.setItem(10, buildVolumeItem(Material.NOTE_BLOCK, Lang.get("gui.sounds.leave"), aranarthPlayer.getLeaveSoundVolume()));
        gui.setItem(11, buildVolumeItem(new KeyVote().getItem(), Lang.get("gui.sounds.vote"), aranarthPlayer.getVoteSoundVolume()));
        gui.setItem(12, buildVolumeItem(Material.CHEST, Lang.get("gui.sounds.crate"), aranarthPlayer.getCrateSoundVolume()));
        gui.setItem(13, buildVolumeItem(Material.WATER_BUCKET, Lang.get("gui.sounds.weather"), aranarthPlayer.getWeatherSoundVolume()));
        gui.setItem(14, buildVolumeItem(Material.CLOCK, Lang.get("gui.sounds.new_day"), aranarthPlayer.getNewDaySoundVolume()));
        gui.setItem(15, buildVolumeItem(Material.FIREWORK_ROCKET, Lang.get("gui.sounds.new_month"), aranarthPlayer.getNewMonthSoundVolume()));
        gui.setItem(16, buildVolumeItem(Material.PAPER, Lang.get("gui.sounds.message"), aranarthPlayer.getPrivateMsgSoundVolume()));
        gui.setItem(17, buildVolumeItem(Material.ENDER_PEARL, Lang.get("gui.sounds.teleport"), aranarthPlayer.getTeleportSoundVolume()));
        gui.setItem(18, buildVolumeItem(Material.NETHER_STAR, Lang.get("gui.sounds.avatar"), aranarthPlayer.getAvatarSoundVolume()));
        gui.setItem(19, buildVolumeItem(Material.IRON_SWORD, Lang.get("gui.sounds.dominion"), aranarthPlayer.getDominionSoundVolume()));
        gui.setItem(20, buildVolumeItem(Material.AMETHYST_SHARD, Lang.get("gui.sounds.aranarthium"), aranarthPlayer.getAranarthiumSoundVolume()));
        gui.setItem(21, buildVolumeItem(Material.BOOK, Lang.get("gui.sounds.chat_game"), aranarthPlayer.getChatGameSoundVolume()));
        gui.setItem(22, buildVolumeItem(Material.CHEST, Lang.get("gui.sounds.chest_sort"), aranarthPlayer.getChestSortSoundVolume()));
        gui.setItem(23, buildVolumeItem(Material.IRON_PICKAXE, Lang.get("gui.sounds.jobs"), aranarthPlayer.getJobsSoundVolume()));
        gui.setItem(24, buildVolumeItem(Material.EXPERIENCE_BOTTLE, Lang.get("gui.sounds.exp_store"), aranarthPlayer.getExpStoreSoundVolume()));
        gui.setItem(25, buildVolumeItem(Material.TOTEM_OF_UNDYING, Lang.get("gui.sounds.low_health"), aranarthPlayer.getLowHealthSoundVolume()));

        return gui;
    }

}
