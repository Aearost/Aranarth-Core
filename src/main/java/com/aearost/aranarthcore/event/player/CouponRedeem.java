package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.Mail;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CouponUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.MailUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

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
        if (item == null || !item.hasItemMeta() || e.getHand() == null) {
            return;
        }

        if (!item.getItemMeta().getPersistentDataContainer().has(COUPON_DISCOUNT, PersistentDataType.INTEGER)) {
            return;
        }

        e.setCancelled(true);

        int discountPercentage = item.getItemMeta().getPersistentDataContainer()
                .get(COUPON_DISCOUNT, PersistentDataType.INTEGER);

        // Remove one coupon item up front so it cannot be redeemed twice while the claim is pending
        Player player = e.getPlayer();
        ItemStack refund = item.clone();
        refund.setAmount(1);
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItem(e.getHand(), null);
        }

        UUID playerUUID = player.getUniqueId();
        CouponUtils.claimCoupon(discountPercentage, (coupon, remaining) -> {
            if (coupon == null) {
                // Give the coupon item back since no code could be claimed
                if (player.isOnline()) {
                    player.getInventory().addItem(refund).values()
                            .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("coupon.no_codes_available")));
                }
                return;
            }

            // Send the coupon code as a mail message
            String mailMessage = ChatUtils.translateToColor(
                    "&7&l" + coupon.getDiscountPercentage() + "% Coupon &8- &e" + coupon.getCode()
            );
            MailUtils.addMail(playerUUID, new Mail(new UUID(0L, 0L), playerUUID, System.currentTimeMillis(), mailMessage));

            DiscordUtils.createNotification(player.getName() + " has redeemed a " + discountPercentage + "% coupon: "
                    + coupon.getCode() + " (" + remaining + " remaining)", playerUUID);

            if (player.isOnline()) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("coupon.redeemed", "discount", String.valueOf(discountPercentage))));
            }
        });
    }
}
