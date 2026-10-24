package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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

    public static ItemStack buildVolumeItem(Player player, ItemStack item, String name, int volume) {
        ItemMeta meta = item.getItemMeta();

        String volumeColor = volume == 100 ? "&a&l" : (volume == 0 ? "&c&l" : "&e&l");
        meta.setDisplayName(ChatUtils.translateToColor(name + " &7&l- " + volumeColor + volume + "%"));
        meta.setLore(List.of(
                Lang.getFor(player, "gui.sounds.volume_lore1"),
                Lang.getFor(player, "gui.sounds.volume_lore2"),
                Lang.getFor(player, "gui.sounds.volume_lore3"),
                Lang.getFor(player, "gui.sounds.volume_lore4")
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildVolumeItem(Player player, Material material, String name, int volume) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String volumeColor = volume == 100 ? "&a&l" : (volume == 0 ? "&c&l" : "&e&l");
        meta.setDisplayName(ChatUtils.translateToColor(name + " &7&l- " + volumeColor + volume + "%"));
        meta.setLore(List.of(
                Lang.getFor(player, "gui.sounds.volume_lore1"),
                Lang.getFor(player, "gui.sounds.volume_lore2"),
                Lang.getFor(player, "gui.sounds.volume_lore3"),
                Lang.getFor(player, "gui.sounds.volume_lore4")
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
        Inventory gui = GuiHolder.create(player, 36, Lang.getFor(player, TITLE_KEY));

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

        gui.setItem(9, buildVolumeItem(player, Material.BELL, Lang.getFor(player, "gui.sounds.join"), aranarthPlayer.getJoinSoundVolume()));
        gui.setItem(10, buildVolumeItem(player, Material.NOTE_BLOCK, Lang.getFor(player, "gui.sounds.leave"), aranarthPlayer.getLeaveSoundVolume()));
        gui.setItem(11, buildVolumeItem(player, new KeyVote().getItem(), Lang.getFor(player, "gui.sounds.vote"), aranarthPlayer.getVoteSoundVolume()));
        gui.setItem(12, buildVolumeItem(player, Material.CHEST, Lang.getFor(player, "gui.sounds.crate"), aranarthPlayer.getCrateSoundVolume()));
        gui.setItem(13, buildVolumeItem(player, Material.WATER_BUCKET, Lang.getFor(player, "gui.sounds.weather"), aranarthPlayer.getWeatherSoundVolume()));
        gui.setItem(14, buildVolumeItem(player, Material.CLOCK, Lang.getFor(player, "gui.sounds.new_day"), aranarthPlayer.getNewDaySoundVolume()));
        gui.setItem(15, buildVolumeItem(player, Material.FIREWORK_ROCKET, Lang.getFor(player, "gui.sounds.new_month"), aranarthPlayer.getNewMonthSoundVolume()));
        gui.setItem(16, buildVolumeItem(player, Material.PAPER, Lang.getFor(player, "gui.sounds.message"), aranarthPlayer.getPrivateMsgSoundVolume()));
        gui.setItem(17, buildVolumeItem(player, Material.ENDER_PEARL, Lang.getFor(player, "gui.sounds.teleport"), aranarthPlayer.getTeleportSoundVolume()));
        gui.setItem(18, buildVolumeItem(player, Material.NETHER_STAR, Lang.getFor(player, "gui.sounds.avatar"), aranarthPlayer.getAvatarSoundVolume()));
        gui.setItem(19, buildVolumeItem(player, Material.IRON_SWORD, Lang.getFor(player, "gui.sounds.dominion"), aranarthPlayer.getDominionSoundVolume()));
        gui.setItem(20, buildVolumeItem(player, Material.AMETHYST_SHARD, Lang.getFor(player, "gui.sounds.aranarthium"), aranarthPlayer.getAranarthiumSoundVolume()));
        gui.setItem(21, buildVolumeItem(player, Material.BOOK, Lang.getFor(player, "gui.sounds.chat_game"), aranarthPlayer.getChatGameSoundVolume()));
        gui.setItem(22, buildVolumeItem(player, Material.CHEST, Lang.getFor(player, "gui.sounds.chest_sort"), aranarthPlayer.getChestSortSoundVolume()));
        gui.setItem(23, buildVolumeItem(player, Material.IRON_PICKAXE, Lang.getFor(player, "gui.sounds.jobs"), aranarthPlayer.getJobsSoundVolume()));
        gui.setItem(24, buildVolumeItem(player, Material.EXPERIENCE_BOTTLE, Lang.getFor(player, "gui.sounds.exp_store"), aranarthPlayer.getExpStoreSoundVolume()));
        gui.setItem(25, buildVolumeItem(player, Material.TOTEM_OF_UNDYING, Lang.getFor(player, "gui.sounds.low_health"), aranarthPlayer.getLowHealthSoundVolume()));
        gui.setItem(26, buildVolumeItem(player, Material.GOAT_HORN, Lang.getFor(player, "gui.sounds.voice_chat"), aranarthPlayer.getVoiceChatSoundVolume()));

        return gui;
    }

}
