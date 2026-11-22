package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.event.listener.EssenceImbueListener;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.EnchantingUtils;
import com.aearost.aranarthcore.utils.EssenceImbueUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles transferring the player's EXP into their held blank Essence, at the same efficiency as storing EXP in bottles.
 */
public class EssenceTransfer {

    private static final long CONFIRM_WINDOW_MILLIS = 10_000;

    private static final HashMap<UUID, PendingTransfer> PENDING = new HashMap<>();

    /**
     * The state of the player and their Essence when the confirmation prompt was displayed.
     */
    private record PendingTransfer(int slot, int imbuedXp, int totalExp, long expiry) {
        boolean matches(int slot, int imbuedXp, int totalExp) {
            return System.currentTimeMillis() <= expiry && this.slot == slot && this.imbuedXp == imbuedXp && this.totalExp == totalExp;
        }
    }

    /**
     * Determines if the event is a sneak + right-click with a blank Essence in the main hand.
     * @param e The event.
     * @return Whether the event should be handled as an Essence transfer.
     */
    public static boolean isTransferAttempt(PlayerInteractEvent e) {
        return (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
                && e.getPlayer().isSneaking()
                && EssenceImbueUtils.isBlankEssence(e.getPlayer().getInventory().getItemInMainHand());
    }

    public void execute(PlayerInteractEvent e) {
        // Prevents interacting with the clicked block, and handles the event only once
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!EssenceImbueUtils.hasRequiredRank(player)) {
            String rankId = EssenceImbueUtils.getRequiredRankId(player);
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "essence.transfer_rank_required",
                    "article", Lang.key("rank.article_" + rankId), "rank", Lang.key("rank.title_" + rankId))));
            return;
        }
        // Also prevents transferring EXP in the Creative and Arena worlds, or while in creative mode
        if (!EnchantingUtils.canUseSkills(player)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "essence.transfer_survival_only")));
            return;
        }
        int unlockLevel = EnchantingUtils.getUnlockLevel("bottled-knowledge");
        if (EnchantingUtils.getLevel(player) < unlockLevel) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "essence.transfer_locked", "level", unlockLevel)));
            return;
        }

        int slot = player.getInventory().getHeldItemSlot();
        ItemStack essence = player.getInventory().getItemInMainHand();
        int maxXp = EssenceImbueUtils.getMaxImbuedXp();
        int imbuedXp = EssenceImbueUtils.getImbuedXp(essence);
        if (imbuedXp >= maxXp) {
            PENDING.remove(uuid);
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "essence.transfer_full")));
            return;
        }

        int totalExp = EssenceImbueUtils.getTotalExp(player);
        double ratio = EnchantingUtils.getBottledKnowledgeRatio(EnchantingUtils.getLevel(player));
        // Only spends the EXP needed to fill the Essence, or all of the player's EXP if they do not have enough
        int needed = (int) Math.ceil((maxXp - imbuedXp) / ratio);
        int spent = Math.min(totalExp, needed);
        int gained = Math.min(maxXp - imbuedXp, (int) Math.floor(spent * ratio));
        if (gained <= 0) {
            PENDING.remove(uuid);
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "essence.transfer_no_exp")));
            return;
        }

        String levels = formatLevels(EssenceImbueUtils.getExactLevel(totalExp) - EssenceImbueUtils.getExactLevel(totalExp - spent));
        String percent = EssenceImbueUtils.formatPercent(imbuedXp + gained);

        PendingTransfer pending = PENDING.get(uuid);
        if (pending == null || !pending.matches(slot, imbuedXp, totalExp)) {
            PENDING.put(uuid, new PendingTransfer(slot, imbuedXp, totalExp, System.currentTimeMillis() + CONFIRM_WINDOW_MILLIS));
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "essence.transfer_confirm",
                    "levels", levels, "percent", percent)));
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "essence.transfer_confirm_hint")));
            return;
        }

        PENDING.remove(uuid);
        EssenceImbueUtils.setTotalExp(player, totalExp - spent);
        EssenceImbueUtils.setImbuedXp(essence, imbuedXp + gained);
        player.getInventory().setItem(slot, essence);
        EssenceImbueListener.notifyThresholds(player, imbuedXp, imbuedXp + gained, maxXp);

        int expVol = AranarthUtils.getPlayer(uuid).getExpStoreSoundVolume();
        if (expVol > 0) {
            float expVf = expVol / 100f;
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, expVf, 1F);
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, expVf, 1.5F);
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, expVf, 2F);
        }
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "essence.transfer_complete",
                "levels", levels, "percent", percent)));
    }

    /**
     * Formats the input levels to at most one decimal place, i.e "152.3" or "20".
     */
    private String formatLevels(double levels) {
        double rounded = Math.round(levels * 10) / 10.0;
        if (rounded == Math.floor(rounded)) {
            return String.valueOf((long) rounded);
        }
        return String.format(Locale.US, "%.1f", rounded);
    }
}
