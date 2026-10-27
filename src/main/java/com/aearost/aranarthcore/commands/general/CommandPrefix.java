package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.enums.PrefixStyle;
import com.aearost.aranarthcore.gui.GuiPrefix;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows players to choose whether their council, saint, and rank prefixes are displayed
 * as full text, a single letter, or a symbol.
 */
public class CommandPrefix implements CommandExecutor {

	private static final String USAGE = "prefix <council|saint|rank> <full|short|symbol>";

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
			return true;
		}

		if (args.length == 0) {
			new GuiPrefix(player).openGui();
			return true;
		}

		PrefixStyle style = args.length >= 2 ? PrefixStyle.fromString(args[1], null) : null;
		String type = args[0].toLowerCase();
		if (style == null || !(type.equals("council") || type.equals("saint") || type.equals("rank"))) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.invalid_syntax", "usage", USAGE)));
			return true;
		}

		applyStyle(player, type, style);
		player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "prefix.updated",
				"type", type, "style", style.name().toLowerCase())));
		player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "prefix.preview",
				"preview", ChatUtils.formatChatPrefix(player))));
		return true;
	}

	/**
	 * Applies a prefix style to the player, then persists and syncs it across servers.
	 *
	 * @param player The player whose prefix style is being changed.
	 * @param type   The prefix type, being council, saint, or rank.
	 * @param style  The new style of the prefix type.
	 */
	public static void applyStyle(Player player, String type, PrefixStyle style) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		switch (type) {
			case "council" -> aranarthPlayer.setCouncilPrefixStyle(style);
			case "saint" -> aranarthPlayer.setSaintPrefixStyle(style);
			default -> aranarthPlayer.setRankPrefixStyle(style);
		}
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		AranarthUtils.updateTab();
		// Immediately persist to MySQL so the styles are not reverted on the next join or server transfer
		if (DatabaseManager.isActive()) {
			final String toggleJson = PersistenceUtils.buildPlayerToggleJson(player.getUniqueId());
			if (toggleJson != null) {
				Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
						DatabaseManager.getInstance().savePlayerToggles(player.getUniqueId(), toggleJson));
			}
		}
		// Refreshes this player's prefix in the other server's TAB list immediately
		if (NetworkManager.isActive()) {
			NetworkManager.getInstance().publishPrefixUpdate(player.getUniqueId(), aranarthPlayer.getEncodedPrefixStyles());
		}
	}

}
