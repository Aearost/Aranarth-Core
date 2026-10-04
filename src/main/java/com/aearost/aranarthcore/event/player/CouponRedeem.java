package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.Coupon;
import com.aearost.aranarthcore.objects.Mail;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CouponUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.MailUtils;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

import static com.aearost.aranarthcore.objects.CustomKeys.COUPON_DISCOUNT;

/**
 * Handles sneak + right-click redemption of store discount coupon items.
 */
public class CouponRedeem {

    public void execute(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!e.getPlayer().isSneaking()) {
            return;
        }

        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        if (!item.getItemMeta().getPersistentDataContainer().has(COUPON_DISCOUNT, PersistentDataType.INTEGER)) {
            return;
        }

        e.setCancelled(true);

        int discountPercentage = item.getItemMeta().getPersistentDataContainer()
                .get(COUPON_DISCOUNT, PersistentDataType.INTEGER);

        Optional<Coupon> result = CouponUtils.consumeCoupon(discountPercentage);
        if (result.isEmpty()) {
            e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.get("coupon.no_codes_available")));
            return;
        }

        Coupon coupon = result.get();

        // Remove one coupon item from the player's hand
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            e.getPlayer().getInventory().setItemInMainHand(null);
        }

        // Send the coupon code as a mail message
        String mailMessage = ChatUtils.translateToColor(
                "&7&l" + coupon.getDiscountPercentage() + "% Coupon &8- &e" + coupon.getCode()
        );
        UUID playerUUID = e.getPlayer().getUniqueId();
        MailUtils.addMail(playerUUID, new Mail(new UUID(0L, 0L), playerUUID, System.currentTimeMillis(), mailMessage));

        e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.get("coupon.redeemed", "discount", String.valueOf(discountPercentage))));
    }
}
