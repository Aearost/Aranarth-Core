package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Overrides the vanilla /enchant command to allow applying unsafe (over-limit) enchantments.
 */
public class CommandEnchant implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }

        if (AranarthUtils.getPlayer(player.getUniqueId()).getCouncilRank() != 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "enchant <username> <enchantment> <level>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[0])));
            return true;
        }

        ItemStack item = target.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("enchant.not_holding_item", "name", target.getName())));
            return true;
        }

        Enchantment enchantment = Enchantment.getByName(args[1].toUpperCase());
        if (enchantment == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("enchant.unknown", "name", args[1])));
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
            if (level < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.amount_positive")));
            return true;
        }

        item.addUnsafeEnchantment(enchantment, level);

        String enchantName = enchantment.getKey().getKey().replace("_", " ");
        player.sendMessage(ChatUtils.chatMessage(Lang.get("enchant.success", "enchantment", enchantName, "level", String.valueOf(level), "player", target.getName())));
        if (!target.getUniqueId().equals(player.getUniqueId())) {
            target.sendMessage(ChatUtils.chatMessage(Lang.get("enchant.applied", "enchant", enchantName, "level", level)));
        }
        return true;
    }
}
