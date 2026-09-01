package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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
            sender.sendMessage(ChatUtils.chatMessage("&cThis command must be executed in-game!"));
            return true;
        }

        if (AranarthUtils.getPlayer(player.getUniqueId()).getCouncilRank() != 3) {
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/enchant <username> <enchantment> <level>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatUtils.chatMessage("&cPlayer &e" + args[0] + " &ccould not be found!"));
            return true;
        }

        ItemStack item = target.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(ChatUtils.chatMessage("&e" + target.getName() + " &cis not holding an item!"));
            return true;
        }

        Enchantment enchantment = Enchantment.getByName(args[1].toUpperCase());
        if (enchantment == null) {
            player.sendMessage(ChatUtils.chatMessage("&cUnknown enchantment: &e" + args[1]));
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
            if (level < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(ChatUtils.chatMessage("&cLevel must be a positive integer!"));
            return true;
        }

        item.addUnsafeEnchantment(enchantment, level);

        String enchantName = enchantment.getKey().getKey().replace("_", " ");
        player.sendMessage(ChatUtils.chatMessage("&7Applied &e" + enchantName + " " + level + " &7to &e" + target.getName() + "&7's held item."));
        if (!target.getUniqueId().equals(player.getUniqueId())) {
            target.sendMessage(ChatUtils.chatMessage("&e" + enchantName + " " + level + " &7has been applied to your held item."));
        }
        return true;
    }
}
