package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiSounds;
import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class GuiSoundsClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null) {
            return;
        }

        if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        int slot = e.getSlot();

        switch (slot) {
            case 31 -> {
                player.closeInventory();
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
            }
            case 9 -> {
                int vol = adjustVolume(aranarthPlayer.getJoinSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setJoinSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Join sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.BELL, "&f&lJoin", vol);
            }
            case 10 -> {
                int vol = adjustVolume(aranarthPlayer.getLeaveSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setLeaveSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Leave sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.NOTE_BLOCK, "&f&lLeave", vol);
            }
            case 11 -> {
                int vol = adjustVolume(aranarthPlayer.getVoteSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setVoteSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Vote sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, new KeyVote().getItem(), "&f&lVote", vol);
            }
            case 12 -> {
                int vol = adjustVolume(aranarthPlayer.getCrateSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setCrateSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Crate sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.CHEST, "&f&lCrate", vol);
            }
            case 13 -> {
                int vol = adjustVolume(aranarthPlayer.getWeatherSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setWeatherSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Weather sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.WATER_BUCKET, "&f&lWeather", vol);
            }
            case 14 -> {
                int vol = adjustVolume(aranarthPlayer.getNewDaySoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setNewDaySoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7New day sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.CLOCK, "&f&lNew Day", vol);
            }
            case 15 -> {
                int vol = adjustVolume(aranarthPlayer.getNewMonthSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setNewMonthSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7New month sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.FIREWORK_ROCKET, "&f&lNew Month", vol);
            }
            case 16 -> {
                int vol = adjustVolume(aranarthPlayer.getPrivateMsgSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setPrivateMsgSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Message sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.PAPER, "&f&lMessage", vol);
            }
            case 17 -> {
                int vol = adjustVolume(aranarthPlayer.getTeleportSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setTeleportSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Teleport sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.ENDER_PEARL, "&f&lTeleport", vol);
            }
            case 18 -> {
                int vol = adjustVolume(aranarthPlayer.getAvatarSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setAvatarSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Avatar sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.NETHER_STAR, "&f&lAvatar", vol);
            }
            case 19 -> {
                int vol = adjustVolume(aranarthPlayer.getDominionSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setDominionSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Dominion sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.IRON_SWORD, "&f&lDominion", vol);
            }
            case 20 -> {
                int vol = adjustVolume(aranarthPlayer.getAranarthiumSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setAranarthiumSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Aranarthium sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.AMETHYST_SHARD, "&f&lAranarthium", vol);
            }
            case 21 -> {
                int vol = adjustVolume(aranarthPlayer.getChatGameSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setChatGameSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Chat game sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.BOOK, "&f&lChat Game", vol);
            }
            case 22 -> {
                int vol = adjustVolume(aranarthPlayer.getChestSortSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setChestSortSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Chest sort sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.CHEST, "&f&lChest Sort", vol);
            }
            case 23 -> {
                int vol = adjustVolume(aranarthPlayer.getJobsSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setJobsSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Jobs sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.IRON_PICKAXE, "&f&lJobs", vol);
            }
            case 24 -> {
                int vol = adjustVolume(aranarthPlayer.getExpStoreSoundVolume(), e.isLeftClick(), e.isShiftClick());
                aranarthPlayer.setExpStoreSoundVolume(vol);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7Exp store sound volume set to &e" + vol + "%"));
                refreshGui(player, slot, Material.EXPERIENCE_BOTTLE, "&f&lExp Store", vol);
            }
        }
    }

    /**
     * Calculates the new volume based on click type.
     * Left = +10, Right = -10, Shift+Left = 100, Shift+Right = 0.
     */
    private int adjustVolume(int current, boolean isLeft, boolean isShift) {
        if (isShift) {
            return isLeft ? 0 : 100;
        }
        return isLeft ? Math.max(0, current - 10) : Math.min(100, current + 10);
    }

    private void refreshGui(Player player, int slot, Material material, String name, int vol) {
        player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
        player.getOpenInventory().getTopInventory().setItem(slot, GuiSounds.buildVolumeItem(material, name, vol));
    }

    private void refreshGui(Player player, int slot, ItemStack item, String name, int vol) {
        player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
        player.getOpenInventory().getTopInventory().setItem(slot, GuiSounds.buildVolumeItem(item, name, vol));
    }

}
